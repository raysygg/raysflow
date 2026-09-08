package com.acme.agentstudio.application.task;

import com.acme.agentstudio.domain.common.ExecutionStatus;
import com.acme.agentstudio.domain.task.model.AsyncTaskSummary;
import com.acme.agentstudio.infrastructure.persistence.entity.AsyncTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AsyncTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于数据库（MySQL）的分布式持久化异步任务队列（Persistent Async Task Queue）服务。
 * 提供可靠的任务入队（enqueue / enqueueOnce）、FIFO 条件抢占（claim）、心跳续租（heartbeat）、超时失联回收（reclaimExpired）、指数退避重试（failOrRetry）与幂等复用，确保在节点宕机或重启时任务绝不丢失。
 */
@Service
public class PersistentTaskQueueService {

    /** 异步任务 Persistence Mapper */
    private final AsyncTaskMapper taskMapper;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入任务 Mapper 与初始化 JSON 工具。
     */
    public PersistentTaskQueueService(AsyncTaskMapper taskMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 将一个新的异步任务入队保存到数据库队列。
     *
     * @param tenantId 租户 ID
     * @param taskType 任务类型编码（如 KNOWLEDGE_REINDEX / ORCHESTRATION_EXECUTION）
     * @param payload 任务负载参数映射 Map
     * @param maxRetries 允许的最大重试次数
     * @return 入队的任务全局唯一 ID
     */
    public Long enqueue(Long tenantId, String taskType, Map<String, Object> payload, int maxRetries) {
        try {
            AsyncTaskEntity task = new AsyncTaskEntity();
            task.setTenantId(tenantId);
            task.setRequestId(stringValue(payload.get("requestId")));
            task.setTraceId(stringValue(payload.get("traceId")));
            task.setRunId(stringValue(payload.get("executionId")));
            task.setTaskType(taskType);
            task.setIdempotencyKey(stringValue(payload.get("idempotencyKey")));
            task.setPayloadJson(objectMapper.writeValueAsString(payload));
            task.setStatus(ExecutionStatus.QUEUED.name());
            task.setRetryCount(0);
            task.setMaxRetries(maxRetries);
            task.setAvailableAt(LocalDateTime.now());
            task.setCreatedAt(LocalDateTime.now());
            taskMapper.insert(task);
            return task.getId();
        } catch (Exception exception) {
            throw new IllegalStateException("异步任务入队保存失败。", exception);
        }
    }

    /**
     * 强类型任务载荷序列化入队，避免业务层手写 Map。
     *
     * @param tenantId 租户 ID
     * @param taskType 任务类型编码
     * @param payload 强类型对象载荷
     * @param maxRetries 最大重试次数
     * @return 任务全局 ID
     */
    public Long enqueuePayload(Long tenantId, String taskType, Object payload, int maxRetries) {
        try {
            Map<String, Object> values = objectMapper.convertValue(payload, new TypeReference<>() {});
            return enqueue(tenantId, taskType, values, maxRetries);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("异步任务载荷对象格式无效，无法序列化。", exception);
        }
    }

    /**
     * 按业务幂等键（Idempotency Key）复用已有任务，防止重复点击或网络重试导致任务重复入队。
     *
     * @param tenantId 租户 ID
     * @param taskType 任务类型编码
     * @param idempotencyKey 幂等校验 Key
     * @param payload 任务负载 Map
     * @param maxRetries 最大重试次数
     * @return 任务全局 ID
     */
    public Long enqueueOnce(Long tenantId, String taskType, String idempotencyKey,
                            Map<String, Object> payload, int maxRetries) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return enqueue(tenantId, taskType, payload, maxRetries);
        }
        AsyncTaskEntity existing = taskMapper.selectOne(new LambdaQueryWrapper<AsyncTaskEntity>()
                .eq(AsyncTaskEntity::getTenantId, tenantId)
                .eq(AsyncTaskEntity::getTaskType, taskType)
                .eq(AsyncTaskEntity::getIdempotencyKey, idempotencyKey));
        if (existing != null) {
            return existing.getId();
        }
        Map<String, Object> durablePayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        durablePayload.put("idempotencyKey", idempotencyKey);
        try {
            return enqueue(tenantId, taskType, durablePayload, maxRetries);
        } catch (Exception exception) {
            AsyncTaskEntity winner = taskMapper.selectOne(new LambdaQueryWrapper<AsyncTaskEntity>()
                    .eq(AsyncTaskEntity::getTenantId, tenantId)
                    .eq(AsyncTaskEntity::getTaskType, taskType)
                    .eq(AsyncTaskEntity::getIdempotencyKey, idempotencyKey));
            if (winner != null) {
                return winner.getId();
            }
            throw exception;
        }
    }

    /**
     * 从数据库队列中提取下一个待处理且已到可执行时间的任务记录（FIFO 顺序）。
     *
     * @return 待执行的异步任务实体（若无可用任务则返回 null）
     */
    public AsyncTaskEntity nextQueued() {
        return taskMapper.selectOne(new LambdaQueryWrapper<AsyncTaskEntity>()
                .eq(AsyncTaskEntity::getStatus, ExecutionStatus.QUEUED.name())
                .le(AsyncTaskEntity::getAvailableAt, LocalDateTime.now())
                .orderByAsc(AsyncTaskEntity::getId)
                .last("LIMIT 1"));
    }

    /**
     * Worker 抢占认领任务执行权（通过数据库 CAS 锁行判定，防止并发抢占）。
     *
     * @param task 待认领的任务实体
     * @param workerId 当前 Worker 节点的标识 ID
     * @return true 表示成功认领到执行权，false 表示被其他 Worker 抢先认领
     */
    public boolean claim(AsyncTaskEntity task, String workerId) {
        return taskMapper.claim(task.getId(), workerId) == 1;
    }

    /**
     * Worker 节点在执行长任务期间定期更新心跳保活时间。
     *
     * @param taskId 任务 ID
     * @param workerId Worker 节点标识 ID
     */
    public void heartbeat(Long taskId, String workerId) {
        taskMapper.heartbeat(taskId, workerId);
    }

    /**
     * 回收因 Worker 节点崩溃或网络断连超时未发送心跳的任务，将其重置为 QUEUED 并实施退避重新入队。
     *
     * @param timeoutSeconds 心跳判定超时秒数
     * @param delaySeconds 重新可执行的延迟秒数
     * @return 被成功回收重新入队的任务实体列表
     */
    public List<AsyncTaskEntity> reclaimExpired(int timeoutSeconds, int delaySeconds) {
        List<AsyncTaskEntity> expired = new ArrayList<>(taskMapper.selectList(new LambdaQueryWrapper<AsyncTaskEntity>()
                .eq(AsyncTaskEntity::getStatus, ExecutionStatus.RUNNING.name())
                .lt(AsyncTaskEntity::getHeartbeatAt, LocalDateTime.now().minusSeconds(timeoutSeconds))));
        if (!expired.isEmpty()) {
            taskMapper.reclaimExpired(timeoutSeconds, delaySeconds, "工作节点心跳超时，任务已由平台重新接管并回退入队。");
        }
        return expired.stream().map(task -> taskMapper.selectById(task.getId())).toList();
    }

    /**
     * 标记异步任务已成功处理完成。
     *
     * @param taskId 任务 ID
     * @param workerId 认领该任务的 Worker 标识
     */
    public void complete(Long taskId, String workerId) {
        if (taskMapper.complete(taskId, workerId) != 1) {
            throw new IllegalStateException("任务完成状态更新失败，该任务可能已超时并被其他 Worker 节点接管。");
        }
    }

    /**
     * 任务执行异常处理：未达最大重试次数则增加 retryCount 并延期重新入队，达到上限则标记为 FAILED 失败终态。
     *
     * @param taskId 任务 ID
     * @param workerId Worker 标识
     * @param delaySeconds 延迟重试时间（秒）
     * @param errorMessage 捕获的异常信息
     * @return 更新后的任务实体
     */
    public AsyncTaskEntity failOrRetry(Long taskId, String workerId, int delaySeconds, String errorMessage) {
        taskMapper.failOrRetry(taskId, workerId, delaySeconds, errorMessage);
        return taskMapper.selectById(taskId);
    }

    /**
     * 确定性/不可恢复的错误直接标记为 FAILED 终态（跳过无意义的重试）。
     *
     * @param taskId 任务 ID
     * @param workerId Worker 标识
     * @param errorMessage 错误消息
     * @return 更新后的任务实体
     */
    public AsyncTaskEntity failPermanently(Long taskId, String workerId, String errorMessage) {
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<AsyncTaskEntity>()
                .eq(AsyncTaskEntity::getId, taskId)
                .eq(AsyncTaskEntity::getStatus, ExecutionStatus.RUNNING.name())
                .eq(AsyncTaskEntity::getWorkerId, workerId)
                .set(AsyncTaskEntity::getStatus, ExecutionStatus.FAILED.name())
                .set(AsyncTaskEntity::getErrorMessage, errorMessage)
                .set(AsyncTaskEntity::getFinishedAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new IllegalStateException("任务已被其他 Worker 节点接管，无法直接写入失败终态。");
        }
        return taskMapper.selectById(taskId);
    }

    /**
     * 查询指定知识库文档历史触发的重建向量索引任务摘要列表。
     *
     * @param tenantId 租户 ID
     * @param documentId 知识库文档 ID
     * @return 匹配的重建索引任务摘要列表
     */
    public List<AsyncTaskSummary> listKnowledgeReindexTasks(Long tenantId, Long documentId) {
        return taskMapper.selectList(new LambdaQueryWrapper<AsyncTaskEntity>()
                        .eq(AsyncTaskEntity::getTenantId, tenantId)
                        .eq(AsyncTaskEntity::getTaskType, TaskType.KNOWLEDGE_REINDEX)
                        .orderByDesc(AsyncTaskEntity::getId)).stream()
                .filter(task -> containsDocument(task, documentId))
                .map(task -> new AsyncTaskSummary(
                        task.getId(),
                        task.getTaskType(),
                        task.getStatus(),
                        task.getRetryCount(),
                        task.getMaxRetries(),
                        task.getAvailableAt(),
                        task.getCreatedAt(),
                        task.getFinishedAt(),
                        task.getErrorMessage()
                ))
                .toList();
    }

    /**
     * 校验 Payload 是否包含目标文档 ID。
     */
    private boolean containsDocument(AsyncTaskEntity task, Long documentId) {
        try {
            Map<String, Object> payload = objectMapper.readValue(task.getPayloadJson(), new TypeReference<>() {});
            Object value = payload.get("documentId");
            return value instanceof Number number && documentId.equals(number.longValue());
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * 将对象安全转为 String。
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

