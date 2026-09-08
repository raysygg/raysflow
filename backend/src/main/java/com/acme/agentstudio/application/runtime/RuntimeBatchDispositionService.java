package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.RuntimeRecoveryProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.DeadLetterStatus;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.DispositionType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时死信任务批量处置服务（Runtime Batch Disposition Service）。
 * 针对大量死信队列任务提供批量重试、状态变更或丢弃能力。
 * 执行隔离处理：逐项触发 dispose 并记录审计，单项失败时不影响其余关联死信的正常处置，最终出具包含成功与失败统计的 BatchResult。
 */
@Service
public class RuntimeBatchDispositionService {

    /** 运行时死信管理持久化服务 */
    private final PersistentRuntimeDeadLetterService deadLetterService;

    /** 运行时恢复治理配置参数 */
    private final RuntimeRecoveryProperties properties;

    /**
     * 构造函数注入依赖服务与配置。
     */
    public RuntimeBatchDispositionService(
            PersistentRuntimeDeadLetterService deadLetterService,
            RuntimeRecoveryProperties properties
    ) {
        this.deadLetterService = deadLetterService;
        this.properties = properties;
    }

    /**
     * 执行批量死信处置命令。
     *
     * @param user 当前登录 SecurityUser
     * @param command 批量处置命令 BatchCommand
     * @return 批量处置统计结果 BatchResult
     */
    public BatchResult execute(SecurityUser user, BatchCommand command) {
        if (command == null || command.deadLetterIds() == null || command.deadLetterIds().isEmpty()) {
            throw new IllegalArgumentException("必须选择至少一条需要批量处置的死信记录。");
        }
        if (!command.confirmed()) {
            throw new IllegalArgumentException("执行批量处置前必须在前端显式二次确认影响范围。");
        }
        if (command.deadLetterIds().size() > properties.getMaxBatchSize()) {
            throw new IllegalArgumentException("一次性批量处置的死信记录数量超过系统允许的最大限制（" + properties.getMaxBatchSize() + "）。");
        }

        List<ItemResult> items = new ArrayList<>();
        for (Long id : command.deadLetterIds()) {
            try {
                deadLetterService.dispose(
                        user,
                        id,
                        command.type(),
                        command.nextStatus(),
                        command.reason(),
                        command.assignee()
                );
                items.add(new ItemResult(id, true, "处置成功完成。"));
            } catch (RuntimeException exception) {
                items.add(new ItemResult(id, false, RuntimeTelemetrySanitizer.summary(exception.getMessage())));
            }
        }

        long succeeded = items.stream().filter(ItemResult::success).count();
        return new BatchResult(items.size(), succeeded, items.size() - succeeded, List.copyOf(items));
    }

    /**
     * 批量处置请求 Command Record。
     *
     * @param deadLetterIds 选中的死信实体 ID 列表
     * @param type 处置动作类型
     * @param nextStatus 目标转换状态
     * @param reason 批量处置原因说明
     * @param assignee 指派人 ID
     * @param confirmed 显式二次确认标志
     */
    public record BatchCommand(
            List<Long> deadLetterIds,
            DispositionType type,
            DeadLetterStatus nextStatus,
            String reason,
            Long assignee,
            boolean confirmed
    ) {
    }

    /** 单项处置结果 Record */
    public record ItemResult(Long deadLetterId, boolean success, String message) {
    }

    /** 批量处置结果统计 Record */
    public record BatchResult(int total, long succeeded, long failed, List<ItemResult> items) {
    }
}

