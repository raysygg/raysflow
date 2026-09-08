package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.task.PersistentTaskWorker;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.AsyncTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeDeadLetterEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeSloAlertEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeTaskAttemptEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AsyncTaskMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeDeadLetterMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeSloAlertMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeTaskAttemptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运行时运维诊断与多链路健康检查服务（Runtime Operations Health Service）。
 * 将控制层 API、后台异步 Worker、分布式租约（Lease）、排队积压（Queue Backlog）与外部依赖告警（SloAlert）拆解为独立组件诊断报告，
 * 出具多维度的健康汇总指标（READY / DEGRADED / UNAVAILABLE），避免单纯的 API HTTP 存活误导为整条 Task 运行链路健康。
 */
@Service
public class RuntimeOperationsHealthService {

    /** 健康状态：完全就绪 */
    private static final String READY = "READY";

    /** 健康状态：降级警告 */
    private static final String DEGRADED = "DEGRADED";

    /** 健康状态：不可用 */
    private static final String UNAVAILABLE = "UNAVAILABLE";

    /** 队列排队积压预警阈值 */
    private static final long BACKLOG_WARNING = 100L;

    /** 持久化后台 Task 处理 Worker */
    private final PersistentTaskWorker worker;

    /** 异步 Task 实体 Mapper */
    private final AsyncTaskMapper taskMapper;

    /** 任务重试尝试 Mapper */
    private final RuntimeTaskAttemptMapper attemptMapper;

    /** 运行时死信队列 Mapper */
    private final RuntimeDeadLetterMapper deadLetterMapper;

    /** SLO 监控告警 Mapper */
    private final RuntimeSloAlertMapper alertMapper;

    /**
     * 构造函数注入依赖服务与 Mappers。
     */
    public RuntimeOperationsHealthService(
            PersistentTaskWorker worker,
            AsyncTaskMapper taskMapper,
            RuntimeTaskAttemptMapper attemptMapper,
            RuntimeDeadLetterMapper deadLetterMapper,
            RuntimeSloAlertMapper alertMapper
    ) {
        this.worker = worker;
        this.taskMapper = taskMapper;
        this.attemptMapper = attemptMapper;
        this.deadLetterMapper = deadLetterMapper;
        this.alertMapper = alertMapper;
    }

    /**
     * 查询生成当前租户的全局运维健康报告 HealthSummary。
     *
     * @param user 当前登录 SecurityUser
     * @return 整体健康汇总对象 HealthSummary
     */
    public HealthSummary summary(SecurityUser user) {
        requireUser(user);
        Long tenantId = user.getTenantId();

        long queued = taskMapper.selectCount(new LambdaQueryWrapper<AsyncTaskEntity>()
                .eq(AsyncTaskEntity::getTenantId, tenantId)
                .eq(AsyncTaskEntity::getStatus, "QUEUED"));

        long expiredLeases = attemptMapper.selectCount(new LambdaQueryWrapper<RuntimeTaskAttemptEntity>()
                .eq(RuntimeTaskAttemptEntity::getTenantId, tenantId)
                .in(RuntimeTaskAttemptEntity::getAttemptStatus, "CLAIMED", "HEARTBEATING")
                .lt(RuntimeTaskAttemptEntity::getLeaseUntil, LocalDateTime.now()));

        long deadLetters = deadLetterMapper.selectCount(new LambdaQueryWrapper<RuntimeDeadLetterEntity>()
                .eq(RuntimeDeadLetterEntity::getTenantId, tenantId)
                .in(RuntimeDeadLetterEntity::getDeadLetterStatus, "OPEN", "ASSIGNED"));

        long openAlerts = alertMapper.selectCount(new LambdaQueryWrapper<RuntimeSloAlertEntity>()
                .eq(RuntimeSloAlertEntity::getTenantId, tenantId)
                .in(RuntimeSloAlertEntity::getAlertStatus, "OPEN", "ACKNOWLEDGED"));

        List<HealthComponent> components = List.of(
                new HealthComponent("API", "服务接口", READY, "REST API 接入层服务可以正常接收 HTTP/WebSocket 请求", 0),
                new HealthComponent(
                        "WORKER",
                        "后台任务",
                        worker.isReady() ? READY : UNAVAILABLE,
                        worker.isReady() ? "后台 PersistentTaskWorker 异步处理正常" : "后台 Worker 暂时离线不可处理新任务",
                        0
                ),
                new HealthComponent(
                        "LEASE",
                        "任务租约",
                        (expiredLeases == 0) ? READY : UNAVAILABLE,
                        (expiredLeases == 0) ? "分布式 Worker 心跳与租约更新正常" : "检测到超时失联的过期任务租约 (" + expiredLeases + ")",
                        expiredLeases
                ),
                new HealthComponent(
                        "QUEUE",
                        "等待队列",
                        (queued < BACKLOG_WARNING) ? READY : DEGRADED,
                        (queued < BACKLOG_WARNING) ? "任务等待队列积压处于正常范围" : "等待处理的排队任务数较多 (" + queued + ")",
                        queued
                ),
                new HealthComponent(
                        "DEPENDENCY",
                        "外部依赖",
                        (openAlerts == 0) ? READY : DEGRADED,
                        (openAlerts == 0) ? "未发现持续影响的外部模型/网络告警" : "存在需要关注的持续 SLO 告警 (" + openAlerts + ")",
                        openAlerts
                )
        );

        String overallStatus;
        if (components.stream().anyMatch(item -> UNAVAILABLE.equals(item.status()))) {
            overallStatus = UNAVAILABLE;
        } else if (components.stream().anyMatch(item -> DEGRADED.equals(item.status()))) {
            overallStatus = DEGRADED;
        } else {
            overallStatus = READY;
        }

        return new HealthSummary(
                overallStatus,
                queued,
                expiredLeases,
                deadLetters,
                openAlerts,
                components,
                Instant.now()
        );
    }

    /** 安全用户凭证校验 */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null) {
            throw new IllegalArgumentException("当前登录身份信息无效，请重新登录。");
        }
    }

    /** 单个健康检测组件 Record */
    public record HealthComponent(String code, String label, String status, String message, long value) {
    }

    /** 整体运维健康报告 Record */
    public record HealthSummary(
            String status,
            long queueBacklog,
            long expiredLeases,
            long deadLetterBacklog,
            long openAlerts,
            List<HealthComponent> components,
            Instant checkedAt
    ) {
    }
}

