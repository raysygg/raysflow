package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeDeadLetterDispositionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeDeadLetterEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeTaskAttemptEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeDeadLetterDispositionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeDeadLetterMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeTaskAttemptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 运行时运营工作台与失败故障诊断服务（Runtime Operations Service）。
 * 提供死信队列的分页多条件查询（failures）以及单次 Run 全链路生命周期时间线溯源（timeline）。
 * 所有查询均强制限制在当前租户（tenantId）的安全边界内，并对异常与摘要进行脱敏清洗（RuntimeTelemetrySanitizer）。
 */
@Service
public class RuntimeOperationsService {

    /** 运行时死信队列 Mapper */
    private final RuntimeDeadLetterMapper deadLetterMapper;

    /** 任务重试尝试 Mapper */
    private final RuntimeTaskAttemptMapper attemptMapper;

    /** 平台执行事件 Mapper */
    private final PlatformExecutionEventMapper eventMapper;

    /** 死信处置审计 Mapper */
    private final RuntimeDeadLetterDispositionMapper dispositionMapper;

    /**
     * 构造函数注入依赖 Mappers。
     */
    public RuntimeOperationsService(
            RuntimeDeadLetterMapper deadLetterMapper,
            RuntimeTaskAttemptMapper attemptMapper,
            PlatformExecutionEventMapper eventMapper,
            RuntimeDeadLetterDispositionMapper dispositionMapper
    ) {
        this.deadLetterMapper = deadLetterMapper;
        this.attemptMapper = attemptMapper;
        this.eventMapper = eventMapper;
        this.dispositionMapper = dispositionMapper;
    }

    /**
     * 多条件分页查询当前租户归属下的失败死信记录。
     *
     * @param user 当前登录 SecurityUser
     * @param applicationId 应用 ID（可选）
     * @param releaseId 发布版本 ID（可选）
     * @param errorCategory 错误类别（可选）
     * @param assignee 处理责任人账号 ID（可选）
     * @param status 死信状态（可选）
     * @param page 分页页码
     * @param pageSize 每页条数
     * @return 分页明细结果 FailurePage
     */
    public FailurePage failures(
            SecurityUser user,
            Long applicationId,
            String releaseId,
            String errorCategory,
            Long assignee,
            String status,
            long page,
            long pageSize
    ) {
        requireUser(user);

        LambdaQueryWrapper<RuntimeDeadLetterEntity> query = new LambdaQueryWrapper<RuntimeDeadLetterEntity>()
                .eq(RuntimeDeadLetterEntity::getTenantId, user.getTenantId())
                .orderByDesc(RuntimeDeadLetterEntity::getCreatedAt);

        query.eq(applicationId != null, RuntimeDeadLetterEntity::getApplicationId, applicationId)
                .eq(releaseId != null && !releaseId.isBlank(), RuntimeDeadLetterEntity::getReleaseId, releaseId)
                .eq(errorCategory != null && !errorCategory.isBlank(), RuntimeDeadLetterEntity::getErrorCategory, errorCategory)
                .eq(assignee != null, RuntimeDeadLetterEntity::getAssignedTo, assignee)
                .eq(status != null && !status.isBlank(), RuntimeDeadLetterEntity::getDeadLetterStatus, status);

        Page<RuntimeDeadLetterEntity> result = deadLetterMapper.selectPage(
                new Page<>(Math.max(1, page), Math.min(50, Math.max(1, pageSize))),
                query
        );

        return new FailurePage(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getRecords().stream().map(DeadLetterView::from).toList()
        );
    }

    /**
     * 聚合特定 runId 在 Worker 尝试、事件总线与人工处置阶段的所有时间线节点。
     *
     * @param user 当前登录 SecurityUser
     * @param runId 运行 ID
     * @return 时间线有序列表 List&lt;TimelineItem&gt;
     */
    public List<TimelineItem> timeline(SecurityUser user, String runId) {
        requireUser(user);
        List<TimelineItem> items = new ArrayList<>();

        attemptMapper.selectList(new LambdaQueryWrapper<RuntimeTaskAttemptEntity>()
                .eq(RuntimeTaskAttemptEntity::getTenantId, user.getTenantId())
                .eq(RuntimeTaskAttemptEntity::getRunId, runId)
        ).forEach(item -> items.add(new TimelineItem(
                item.getStartedAt(),
                "ATTEMPT",
                item.getAttemptStatus(),
                RuntimeTelemetrySanitizer.summary(item.getErrorSummary())
        )));

        eventMapper.selectList(new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                .eq(PlatformExecutionEventEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionEventEntity::getExecutionId, runId)
        ).forEach(item -> items.add(new TimelineItem(
                item.getCreatedAt(),
                item.getEventType(),
                item.getStatus(),
                RuntimeTelemetrySanitizer.summary(item.getSummaryJson())
        )));

        List<Long> deadLetterIds = deadLetterMapper.selectList(new LambdaQueryWrapper<RuntimeDeadLetterEntity>()
                .eq(RuntimeDeadLetterEntity::getTenantId, user.getTenantId())
                .eq(RuntimeDeadLetterEntity::getRunId, runId)
        ).stream().map(RuntimeDeadLetterEntity::getId).toList();

        if (!deadLetterIds.isEmpty()) {
            dispositionMapper.selectList(new LambdaQueryWrapper<RuntimeDeadLetterDispositionEntity>()
                    .eq(RuntimeDeadLetterDispositionEntity::getTenantId, user.getTenantId())
                    .in(RuntimeDeadLetterDispositionEntity::getDeadLetterId, deadLetterIds)
            ).forEach(item -> items.add(new TimelineItem(
                    item.getCreatedAt(),
                    "MANUAL_ACTION",
                    item.getDispositionType(),
                    RuntimeTelemetrySanitizer.summary(item.getReason())
            )));
        }

        return items.stream()
                .sorted(Comparator.comparing(TimelineItem::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** 校验登录身份与租户 ID */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份信息无效，请重新登录。");
        }
    }

    /** 死信展示传输 Record */
    public record DeadLetterView(
            Long id,
            Long applicationId,
            String releaseId,
            String runId,
            String errorCategory,
            String safeSummary,
            String status,
            Long assignedTo,
            String replayRunId,
            LocalDateTime createdAt
    ) {
        static DeadLetterView from(RuntimeDeadLetterEntity e) {
            return new DeadLetterView(
                    e.getId(),
                    e.getApplicationId(),
                    e.getReleaseId(),
                    e.getRunId(),
                    e.getErrorCategory(),
                    RuntimeTelemetrySanitizer.summary(e.getErrorSummary()),
                    e.getDeadLetterStatus(),
                    e.getAssignedTo(),
                    e.getReplayRunId(),
                    e.getCreatedAt()
            );
        }
    }

    /** 失败列表分页响应 Record */
    public record FailurePage(long page, long pageSize, long total, List<DeadLetterView> records) {
    }

    /** 时间线节点项 Record */
    public record TimelineItem(LocalDateTime occurredAt, String stage, String status, String safeSummary) {
    }
}

