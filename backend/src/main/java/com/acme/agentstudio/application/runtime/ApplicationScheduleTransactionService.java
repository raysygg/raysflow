package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.application.ApplicationEntrypointService;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEntrypointMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 应用定时入口调度事务应用服务（Application Schedule Transaction Service）。
 * 负责在独立短事务中更新 Cron 定时触发游标（Next Fire Time）并释放租约锁，确保长耗时的工作流执行不阻塞锁事务。
 */
@Service
public class ApplicationScheduleTransactionService {

    /** 入口持久化 Mapper */
    private final ApplicationEntrypointMapper entrypointMapper;

    /**
     * 构造函数注入入口 Mapper 依赖。
     *
     * @param entrypointMapper 应用入口数据 Access 对象
     */
    public ApplicationScheduleTransactionService(ApplicationEntrypointMapper entrypointMapper) {
        this.entrypointMapper = entrypointMapper;
    }

    /**
     * 定时任务成功触发后，在独立事务中推进下一次 Cron 触发游标并释放调度租约锁。
     *
     * @param entrypoint 应用入口实体 ApplicationEntrypointEntity
     * @param fireTime 本次实际触发时间点 LocalDateTime
     */
    @Transactional
    public void complete(ApplicationEntrypointEntity entrypoint, LocalDateTime fireTime) {
        entrypoint.setNextFireAt(ApplicationEntrypointService.nextFire(
                entrypoint.getCronExpression(),
                entrypoint.getTimezone(),
                fireTime
        ));
        entrypoint.setScheduleLeaseOwner(null);
        entrypoint.setScheduleLeaseUntil(null);
        entrypoint.setUpdatedAt(LocalDateTime.now());
        entrypointMapper.updateById(entrypoint);
    }

    /**
     * 异常或跳过触发时，在独立事务中直接清空释放租约锁。
     *
     * @param entrypoint 应用入口实体 ApplicationEntrypointEntity
     */
    @Transactional
    public void release(ApplicationEntrypointEntity entrypoint) {
        entrypoint.setScheduleLeaseOwner(null);
        entrypoint.setScheduleLeaseUntil(null);
        entrypointMapper.updateById(entrypoint);
    }
}

