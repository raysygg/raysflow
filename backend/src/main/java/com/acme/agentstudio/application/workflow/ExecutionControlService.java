package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.model.RunControlAction;
import com.acme.agentstudio.domain.runtime.model.RunControlRequest;
import com.acme.agentstudio.domain.runtime.model.RunControlResult;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionContextEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionContextMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 分布式工作流运行控制服务（Execution Control Service）。
 * 负责 Worker 节点租约抢占（Claim）、心跳续约（Heartbeat）、运行状态切换（暂停 PAUSE / 恢复 RESUME / 取消 CANCEL / 重试 RETRY / 重放 REPLAY）、
 * 人工审批决策（APPROVE / REJECT）提交以及超时未续租 Context 的排队回收机制。
 */
@Service
public class ExecutionControlService {

    /**
     * 恢复原因：人工重试
     */
    private static final String RECOVERY_REASON_RETRY = "MANUAL_RETRY";

    /**
     * 恢复原因：人工恢复
     */
    private static final String RECOVERY_REASON_RECOVER = "MANUAL_RECOVER";

    /**
     * 执行上下文 Mapper
     */
    private final PlatformExecutionContextMapper mapper;

    /**
     * 执行事件日志 Mapper
     */
    private final PlatformExecutionEventMapper eventMapper;

    /**
     * 持久化流程编排执行服务
     */
    private final PersistentOrchestrationExecutionService executionService;

    /**
     * 实时执行事件发布器
     */
    private final ExecutionEventPublisher eventPublisher;

    /**
     * Jackson JSON 映射器
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入运行控制依赖服务。
     */
    public ExecutionControlService(PlatformExecutionContextMapper mapper,
                                   PlatformExecutionEventMapper eventMapper,
                                   PersistentOrchestrationExecutionService executionService,
                                   ExecutionEventPublisher eventPublisher,
                                   ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.eventMapper = eventMapper;
        this.executionService = executionService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Worker 节点抢占/续约指定工作流执行上下文的租约（Lease）。
     *
     * @param user         当前操作用户
     * @param executionId  工作流执行实例 ID
     * @param worker       当前 Worker 节点标识
     * @param leaseSeconds 租约有效期秒数
     * @return 最新的工作流执行状态 Map
     */
    public Map<String, Object> claim(SecurityUser user, String executionId, String worker, int leaseSeconds) {
        PlatformExecutionContextEntity context = require(user, executionId);
        LocalDateTime now = LocalDateTime.now();

        if (isTerminalStatus(context.getStatus())) {
            throw new IllegalStateException("当前工作流执行已进入终态，无法继续抢占或续约租约。");
        }

        if (context.getLeaseUntil() != null && context.getLeaseUntil().isAfter(now) && !worker.equals(context.getLeaseOwner())) {
            throw new IllegalStateException("当前工作流租约已被其他 Worker 节点 [" + context.getLeaseOwner() + "] 占用。");
        }

        context.setLeaseOwner(worker);
        context.setLeaseUntil(now.plusSeconds(Math.max(1, leaseSeconds)));
        context.setHeartbeatAt(now);
        mapper.updateById(context);
        return state(context);
    }

    /**
     * 运维控制工作流执行：取消、暂停、恢复、重试、转交、重放或提交人工审批决策。
     *
     * @param user        当前操作用户
     * @param executionId 工作流执行实例 ID
     * @param request     运行控制请求
     * @return 控制执行结果对象
     */
    public RunControlResult control(SecurityUser user, String executionId, RunControlRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("提交的运行控制请求不能为空。");
        }
        PlatformExecutionContextEntity context = require(user, executionId);

        RunControlAction action = request.action();
        String transferTarget = request.transferTarget() == null ? "" : request.transferTarget().trim();

        switch (action) {
            case CANCEL -> {
                context.setCancelRequested(true);
                if (ExecutionStatus.QUEUED.equals(context.getStatus()) || ExecutionStatus.PAUSED.equals(context.getStatus())) {
                    context.setStatus(ExecutionStatus.CANCELLED);
                }
            }
            case PAUSE -> {
                context.setPauseRequested(true);
                if (ExecutionStatus.RUNNING.equals(context.getStatus())) {
                    context.setStatus(ExecutionStatus.PAUSING);
                }
            }
            case RESUME -> {
                context.setPauseRequested(false);
                if (ExecutionStatus.PAUSED.equals(context.getStatus()) || ExecutionStatus.PAUSING.equals(context.getStatus())) {
                    context.setStatus(ExecutionStatus.QUEUED);
                }
            }
            case HEARTBEAT -> context.setHeartbeatAt(LocalDateTime.now());
            case APPROVE, REJECT -> {
                if (!"WAITING_APPROVAL".equals(context.getStatus())) {
                    throw new IllegalStateException("当前工作流实例未处于等待人工审批（WAITING_APPROVAL）状态。");
                }
                context.setApprovalDecision(action.name());
                context.setPauseRequested(false);
                context.setStatus(ExecutionStatus.QUEUED);

                PlatformExecutionEventEntity event = new PlatformExecutionEventEntity();
                event.setTenantId(user.getTenantId());
                event.setExecutionId(executionId);
                event.setNodeId(context.getCurrentNodeId());
                event.setEventType(action == RunControlAction.APPROVE
                        ? ExecutionEventType.APPROVAL_APPROVE : ExecutionEventType.APPROVAL_REJECT);

                PlatformExecutionEventEntity previous = eventMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                        .eq(PlatformExecutionEventEntity::getTenantId, user.getTenantId())
                        .eq(PlatformExecutionEventEntity::getExecutionId, executionId)
                        .orderByDesc(PlatformExecutionEventEntity::getSequenceNo)
                        .last("LIMIT 1"));

                event.setSequenceNo(previous == null ? 1L : previous.getSequenceNo() + 1);
                event.setPayloadJson(writePayload(Map.of(
                        "decision", action.name(),
                        "comment", request.comment() == null ? "" : request.comment(),
                        "operator", user.getUsername()
                )));
                event.setCreatedAt(LocalDateTime.now());
                fillTrace(event, context);
                eventMapper.insert(event);
                eventPublisher.publish(event);
            }
            case RETRY -> {
                if (!ExecutionStatus.FAILED.equals(context.getStatus()) && !ExecutionStatus.CANCELLED.equals(context.getStatus())) {
                    throw new IllegalStateException("只有处于失败或已取消状态的工作流才允许重新发起重试。");
                }
                return result(executionId, action, executionService.enqueueRetry(user, context), "已成功创建重试运行任务。");
            }
            case RECOVER -> {
                if (!ExecutionStatus.QUEUED.equals(context.getStatus()) && !ExecutionStatus.PAUSED.equals(context.getStatus())) {
                    throw new IllegalStateException("当前执行状态不支持恢复排队。");
                }
                context.setStatus(ExecutionStatus.QUEUED);
                context.setPauseRequested(false);
            }
            case TRANSFER -> {
                if (transferTarget.isBlank()) {
                    throw new IllegalArgumentException("转交工作流运行必须指定接收目标的 Worker 或用户标识。");
                }
                if (isTerminalStatus(context.getStatus())) {
                    throw new IllegalStateException("已进入终态的工作流运行不能再转交。");
                }
                context.setLeaseOwner(transferTarget);
                context.setLeaseUntil(null);
                context.setStatus(ExecutionStatus.QUEUED);
                appendAuditEvent(user, context, "EXECUTION_TRANSFERRED", "{\"target\":\"" + escapeJson(transferTarget) + "\"}");
            }
            case REPLAY -> {
                if (!isTerminalStatus(context.getStatus())) {
                    throw new IllegalStateException("只有已结束的工作流运行才能执行重放。");
                }
                appendAuditEvent(user, context, "EXECUTION_REPLAY_REQUESTED", "{\"sourceExecutionId\":\"" + escapeJson(executionId) + "\"}");
                Map<String, Object> input;
                try {
                    input = objectMapper.readValue(context.getInputJson(), new TypeReference<>() {
                    });
                } catch (Exception exception) {
                    throw new IllegalStateException("原运行输入 JSON 无法正常解析，无法完成重放。", exception);
                }

                String releaseVersionId = executionService.resolveReleaseVersionId(user, context);
                Map<String, Object> replay = new LinkedHashMap<>(executionService.enqueue(
                        user,
                        context.getAppId(),
                        releaseVersionId,
                        context.getExecutionType(),
                        executionId + ":REPLAY:" + System.currentTimeMillis(),
                        null,
                        null,
                        input
                ));
                replay.put("replayedFrom", executionId);
                return result(executionId, action, replay, "已成功创建重放运行实例。");
            }
        }
        mapper.updateById(context);

        if (action == RunControlAction.APPROVE || action == RunControlAction.REJECT) {
            return result(executionId, action, executionService.enqueueResume(user, executionId, action.name()), "审批决策已提交并继续入队。");
        }
        if (action == RunControlAction.RESUME) {
            return result(executionId, action, executionService.enqueueRecovery(user, executionId, "MANUAL_RESUME"), "工作流运行已成功恢复。");
        }
        if (action == RunControlAction.RETRY) {
            return result(executionId, action, executionService.enqueueRecovery(user, executionId, RECOVERY_REASON_RETRY), "工作流运行已重新进入重试队列。");
        }
        if (action == RunControlAction.RECOVER) {
            return result(executionId, action, executionService.enqueueRecovery(user, executionId, RECOVERY_REASON_RECOVER), "工作流运行已恢复入队。");
        }
        return new RunControlResult(executionId, context.getStatus(), action, null, "运行控制指令已顺利执行。");
    }

    /**
     * 构造控制命令响应小结
     */
    private RunControlResult result(String runId, RunControlAction action, Map<String, Object> source, String message) {
        Object target = source.getOrDefault("executionId", source.get("runId"));
        Object status = source.get("status");
        return new RunControlResult(runId, status == null ? null : String.valueOf(status), action, target == null ? null : String.valueOf(target), message);
    }

    /**
     * 序列化事件 JSON Payload
     */
    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("运行控制事件 Payload JSON 序列化失败。", exception);
        }
    }

    /**
     * 查询可以被重新抢占或恢复的可恢复工作流实例列表。
     *
     * @param user 当前用户
     * @return 包含排队中/运行中/挂起中可恢复实例的 Map 结构
     */
    public Map<String, Object> recoverable(SecurityUser user) {
        return Map.of("executions", mapper.selectList(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .in(PlatformExecutionContextEntity::getStatus, "QUEUED", "RUNNING", "PAUSING")
                .and(q -> q.isNull(PlatformExecutionContextEntity::getLeaseUntil).or().lt(PlatformExecutionContextEntity::getLeaseUntil, LocalDateTime.now()))
                .orderByAsc(PlatformExecutionContextEntity::getStartedAt)));
    }

    /**
     * 定时任务：扫描并自动回收租约已超时的 Worker 节点实例，将其状态重新打回 QUEUED 入队等待。
     */
    @Scheduled(fixedDelayString = "${app.orchestration.lease-reclaim-delay-ms:5000}")
    public void reclaimExpiredLeases() {
        LocalDateTime now = LocalDateTime.now();
        mapper.selectList(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .in(PlatformExecutionContextEntity::getStatus, "RUNNING", "PAUSING")
                .isNotNull(PlatformExecutionContextEntity::getLeaseUntil)
                .lt(PlatformExecutionContextEntity::getLeaseUntil, now)).forEach(context -> {
            context.setStatus("QUEUED");
            context.setLeaseOwner(null);
            context.setLeaseUntil(null);
            context.setHeartbeatAt(now);
            mapper.updateById(context);

            PlatformExecutionEventEntity event = new PlatformExecutionEventEntity();
            event.setTenantId(context.getTenantId());
            event.setExecutionId(context.getExecutionId());
            event.setNodeId(context.getCurrentNodeId());
            event.setEventType("LEASE_RECLAIMED");
            event.setSequenceNo(nextSequence(context.getTenantId(), context.getExecutionId()));
            event.setPayloadJson("{\"reason\":\"lease_expired\"}");
            event.setCreatedAt(now);
            fillTrace(event, context);
            eventMapper.insert(event);
            eventPublisher.publish(event);

            executionService.enqueueLeaseRecovery(context.getTenantId(), context.getExecutionId());
        });
    }

    /**
     * 计算下一个序号
     */
    private long nextSequence(Long tenantId, String executionId) {
        PlatformExecutionEventEntity event = eventMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                .eq(PlatformExecutionEventEntity::getTenantId, tenantId)
                .eq(PlatformExecutionEventEntity::getExecutionId, executionId)
                .orderByDesc(PlatformExecutionEventEntity::getSequenceNo)
                .last("LIMIT 1"));
        return event == null || event.getSequenceNo() == null ? 1L : event.getSequenceNo() + 1;
    }

    /**
     * 追加审计控制事件
     */
    private void appendAuditEvent(SecurityUser user, PlatformExecutionContextEntity context, String eventType, String payloadJson) {
        PlatformExecutionEventEntity event = new PlatformExecutionEventEntity();
        event.setTenantId(user.getTenantId());
        event.setExecutionId(context.getExecutionId());
        event.setNodeId(context.getCurrentNodeId());
        event.setEventType(eventType);
        event.setSequenceNo(nextSequence(user.getTenantId(), context.getExecutionId()));
        event.setPayloadJson(payloadJson);
        event.setCreatedAt(LocalDateTime.now());
        fillTrace(event, context);
        eventMapper.insert(event);
        eventPublisher.publish(event);
    }

    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 填充 Trace 诊断信息
     */
    private void fillTrace(PlatformExecutionEventEntity event, PlatformExecutionContextEntity context) {
        event.setRequestId(context.getRequestId());
        event.setTraceId(context.getTraceId());
        event.setTaskId(context.getTaskId());
        event.setSpanId(UUID.randomUUID().toString());
    }

    /**
     * 获取必需的执行上下文
     */
    private PlatformExecutionContextEntity require(SecurityUser user, String executionId) {
        if (user == null || user.getTenantId() == null) {
            throw new IllegalArgumentException("当前登录身份无效。");
        }
        PlatformExecutionContextEntity context = mapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (context == null) {
            throw new IllegalArgumentException("未找到 ID 为 [" + executionId + "] 的工作流执行上下文。");
        }
        return context;
    }

    /**
     * 返回当前状态 Map
     */
    private Map<String, Object> state(PlatformExecutionContextEntity c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("executionId", c.getExecutionId());
        map.put("status", c.getStatus());
        map.put("currentNodeId", String.valueOf(c.getCurrentNodeId()));
        map.put("approvalDecision", String.valueOf(c.getApprovalDecision()));
        map.put("leaseOwner", String.valueOf(c.getLeaseOwner()));
        map.put("leaseUntil", String.valueOf(c.getLeaseUntil()));
        map.put("cancelRequested", String.valueOf(c.getCancelRequested()));
        map.put("pauseRequested", String.valueOf(c.getPauseRequested()));
        return map;
    }

    /**
     * 判断状态是否已终止
     */
    private boolean isTerminalStatus(String status) {
        return ExecutionStatus.isTerminal(status);
    }
}

