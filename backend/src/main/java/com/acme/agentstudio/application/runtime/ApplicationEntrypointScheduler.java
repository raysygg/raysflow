package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.application.ApplicationEntrypointType;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.WebhookNonceEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEntrypointMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.WebhookNonceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 应用定时入口与 Webhook 随机数清理后台调度器（Application Entrypoint Scheduler）。
 * 利用分布式数据库租约（Database Schedule Lease）保证多节点部署场景下定时触发器不会被重复调用。
 */
@Service
@Slf4j
public class ApplicationEntrypointScheduler {

    /** 每次扫描到期的定时入口最大数量上限 */
    private static final int SCAN_LIMIT = 100;

    /** 调度器抢占数据库租约的时长（秒） */
    private static final int LEASE_SECONDS = 120;

    /** 当前调度实例的全局唯一 Worker ID */
    private final String workerId = "entrypoint-scheduler-" + UUID.randomUUID();

    /** 应用入口数据 Access 对象 */
    private final ApplicationEntrypointMapper entrypointMapper;

    /** 入口调用触发应用服务 */
    private final EntrypointInvocationService invocationService;

    /** 定时事务与游标更新应用服务 */
    private final ApplicationScheduleTransactionService transactionService;

    /** Webhook Nonce 防重放标记 Mapper */
    private final WebhookNonceMapper nonceMapper;

    /**
     * 构造函数注入所有必要的调度依赖服务。
     */
    public ApplicationEntrypointScheduler(
            ApplicationEntrypointMapper entrypointMapper,
            EntrypointInvocationService invocationService,
            ApplicationScheduleTransactionService transactionService,
            WebhookNonceMapper nonceMapper
    ) {
        this.entrypointMapper = entrypointMapper;
        this.invocationService = invocationService;
        this.transactionService = transactionService;
        this.nonceMapper = nonceMapper;
    }

    /**
     * 定时任务：清理已经过期的 Webhook 校验随机数（Nonce），防止防重放数据无限膨胀。
     */
    @Scheduled(fixedDelayString = "${app.runtime.webhook-nonce-cleanup-delay-ms:300000}")
    public void cleanupExpiredWebhookNonces() {
        nonceMapper.delete(new LambdaQueryWrapper<WebhookNonceEntity>()
                .lt(WebhookNonceEntity::getExpiresAt, LocalDateTime.now()));
    }

    /**
     * 定时任务：轮询扫表并分发所有已到期的定时应用入口（SCHEDULE）。
     */
    @Scheduled(fixedDelayString = "${app.runtime.entrypoint-schedule-delay-ms:5000}")
    public void dispatchDueEntrypoints() {
        LocalDateTime now = LocalDateTime.now();
        dueEntrypoints(now).forEach(this::dispatchOne);
    }

    /** 扫描数据库中已到期且未被租约锁定的定时入口列表 */
    private List<ApplicationEntrypointEntity> dueEntrypoints(LocalDateTime now) {
        return entrypointMapper.selectList(new LambdaQueryWrapper<ApplicationEntrypointEntity>()
                .eq(ApplicationEntrypointEntity::getEntrypointType, ApplicationEntrypointType.SCHEDULE.name())
                .eq(ApplicationEntrypointEntity::getEnabled, true)
                .le(ApplicationEntrypointEntity::getNextFireAt, now)
                .and(query -> query.isNull(ApplicationEntrypointEntity::getScheduleLeaseUntil)
                        .or()
                        .lt(ApplicationEntrypointEntity::getScheduleLeaseUntil, now))
                .orderByAsc(ApplicationEntrypointEntity::getNextFireAt)
                .last("LIMIT " + SCAN_LIMIT));
    }

    /** 单个定时入口抢占租约并分发运行 */
    private void dispatchOne(ApplicationEntrypointEntity entrypoint) {
        if (entrypointMapper.claimSchedule(entrypoint.getId(), workerId, LEASE_SECONDS) != 1) {
            return;
        }
        LocalDateTime fireTime = entrypoint.getNextFireAt();
        try {
            com.acme.agentstudio.domain.runtime.model.RunContracts.RunSubmission submission = invocationService.invokeSchedule(entrypoint, fireTime);
            transactionService.complete(entrypoint, fireTime);
            log.info("定时入口已成功触发运行实例，entrypointId=[{}], runId=[{}], fireTime=[{}]",
                    entrypoint.getId(), submission.runId(), fireTime);
        } catch (Exception exception) {
            transactionService.release(entrypoint);
            log.error("定时入口触发运行异常，entrypointId=[{}], fireTime=[{}], 错误提示信息=[{}]",
                    entrypoint.getId(), fireTime, exception.getMessage(), exception);
        }
    }
}
