package com.acme.agentstudio.application.knowledge;

import com.acme.agentstudio.config.KnowledgeOperationsProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.SyncRunCheckpoint;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.SyncRunStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeSourceEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeSyncRunEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeSourceMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeSyncRunMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 知识库数据源增量同步任务（Sync Run）与断点检查点（Checkpoint）管理服务。
 * 负责在异步 Worker 执行文档拉取与索引时保存游标（Cursor）断点续传状态、更新发现/变更/删除/失败文档统计计数，并在同步成功时更新数据源游标。
 */
@Service
public class KnowledgeSyncRunService {

    /** 数据源 Mapper */
    private final KnowledgeSourceMapper sourceMapper;

    /** 同步任务 Mapper */
    private final KnowledgeSyncRunMapper runMapper;

    /** 知识运维配置属性 */
    private final KnowledgeOperationsProperties properties;

    /**
     * 构造函数注入数据源与同步任务 Persistence 组件及配置属性。
     */
    public KnowledgeSyncRunService(KnowledgeSourceMapper sourceMapper,
                                   KnowledgeSyncRunMapper runMapper,
                                   KnowledgeOperationsProperties properties) {
        this.sourceMapper = sourceMapper;
        this.runMapper = runMapper;
        this.properties = properties;
    }

    /**
     * 开启一次全新的数据源同步运行任务，记录初始游标位置。
     *
     * @param user 当前登录用户
     * @param sourceId 数据源 ID
     * @return 初始的同步断点检查点对象
     */
    @Transactional
    public SyncRunCheckpoint begin(SecurityUser user, Long sourceId) {
        KnowledgeSourceEntity source = requireSource(user, sourceId);
        KnowledgeSyncRunEntity run = new KnowledgeSyncRunEntity();
        run.setTenantId(user.getTenantId());
        run.setSourceId(source.getId());
        run.setRunStatus(SyncRunStatus.QUEUED.name());
        run.setCursorBefore(source.getSyncCursor());
        run.setDiscoveredCount(0);
        run.setChangedCount(0);
        run.setDeletedCount(0);
        run.setSkippedCount(0);
        run.setFailedCount(0);
        run.setCreatedAt(LocalDateTime.now());
        runMapper.insert(run);
        return toCheckpoint(run);
    }

    /**
     * 更新保存当前同步运行任务的游标断点（Checkpoint）与计数指标。
     *
     * @param user 当前登录用户
     * @param runId 同步任务 ID
     * @param cursor 最新的更新游标字符串
     * @param discovered 发现的文档数
     * @param changed 变更新增的文档数
     * @param deleted 已删除的文档数
     * @param skipped 跳过的未变更文档数
     * @param failed 失败的文档数
     * @param status 当前任务状态
     * @return 最新的断点检查点对象
     */
    @Transactional
    public SyncRunCheckpoint checkpoint(SecurityUser user, Long runId, String cursor,
                                       int discovered, int changed, int deleted,
                                       int skipped, int failed, SyncRunStatus status) {
        requireTenant(user);
        KnowledgeSyncRunEntity current = runMapper.selectOne(new LambdaQueryWrapper<KnowledgeSyncRunEntity>()
                .eq(KnowledgeSyncRunEntity::getTenantId, user.getTenantId())
                .eq(KnowledgeSyncRunEntity::getId, runId));
        if (current == null) {
            throw new IllegalArgumentException("同步任务不存在");
        }
        int retryLimit = properties.getMaxSyncRetryCount();
        if (failed > retryLimit && status != SyncRunStatus.FAILED) {
            throw new IllegalArgumentException("同步失败次数超过重试上限");
        }
        runMapper.update(null, new LambdaUpdateWrapper<KnowledgeSyncRunEntity>()
                .eq(KnowledgeSyncRunEntity::getTenantId, user.getTenantId())
                .eq(KnowledgeSyncRunEntity::getId, runId)
                .set(KnowledgeSyncRunEntity::getCursorAfter, cursor)
                .set(KnowledgeSyncRunEntity::getDiscoveredCount, discovered)
                .set(KnowledgeSyncRunEntity::getChangedCount, changed)
                .set(KnowledgeSyncRunEntity::getDeletedCount, deleted)
                .set(KnowledgeSyncRunEntity::getSkippedCount, skipped)
                .set(KnowledgeSyncRunEntity::getFailedCount, failed)
                .set(KnowledgeSyncRunEntity::getRunStatus, status.name())
                .set(KnowledgeSyncRunEntity::getCompletedAt,
                        status == SyncRunStatus.SUCCEEDED || status == SyncRunStatus.FAILED ? LocalDateTime.now() : null));

        if (status == SyncRunStatus.SUCCEEDED && cursor != null) {
            sourceMapper.update(null, new LambdaUpdateWrapper<KnowledgeSourceEntity>()
                    .eq(KnowledgeSourceEntity::getTenantId, user.getTenantId())
                    .eq(KnowledgeSourceEntity::getId, current.getSourceId())
                    .set(KnowledgeSourceEntity::getSyncCursor, cursor)
                    .set(KnowledgeSourceEntity::getLastSyncedAt, LocalDateTime.now()));
        }
        current.setCursorAfter(cursor);
        current.setDiscoveredCount(discovered);
        current.setChangedCount(changed);
        current.setDeletedCount(deleted);
        current.setSkippedCount(skipped);
        current.setFailedCount(failed);
        current.setRunStatus(status.name());
        return toCheckpoint(current);
    }

    /**
     * 校验数据源是否存在。
     */
    private KnowledgeSourceEntity requireSource(SecurityUser user, Long sourceId) {
        requireTenant(user);
        KnowledgeSourceEntity source = sourceMapper.selectOne(new LambdaQueryWrapper<KnowledgeSourceEntity>()
                .eq(KnowledgeSourceEntity::getTenantId, user.getTenantId())
                .eq(KnowledgeSourceEntity::getId, sourceId));
        if (source == null) {
            throw new IllegalArgumentException("数据来源不存在");
        }
        return source;
    }

    /**
     * 转换实体对象为检查点契约。
     */
    private SyncRunCheckpoint toCheckpoint(KnowledgeSyncRunEntity run) {
        return new SyncRunCheckpoint(run.getId(), run.getCursorAfter(), value(run.getDiscoveredCount()),
                value(run.getChangedCount()), value(run.getDeletedCount()), value(run.getSkippedCount()),
                value(run.getFailedCount()), SyncRunStatus.valueOf(run.getRunStatus()));
    }

    /**
     * Integer 转 int 安全防 NPT 辅助方法。
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 校验租户身份。
     */
    private void requireTenant(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
    }
}

