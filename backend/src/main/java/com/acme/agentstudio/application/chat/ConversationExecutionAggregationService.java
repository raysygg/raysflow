package com.acme.agentstudio.application.chat;

import com.acme.agentstudio.application.workflow.PersistentOrchestrationExecutionService;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformConversationMessageEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformConversationMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话历史与工作流执行轨迹聚合恢复服务。
 * 负责提供单一会话（Conversation）下按时间序拉取的对话消息记录、最后一次 ExecutionID 及其上下文 Execution 详细图节点执行明细聚合视图。
 */
@Service
public class ConversationExecutionAggregationService {

    /** 平台会话消息持久化 Mapper */
    private final PlatformConversationMessageMapper messageMapper;

    /** 工作流持久化执行轨迹应用服务 */
    private final PersistentOrchestrationExecutionService executionService;

    /**
     * 构造函数注入会话消息与执行轨迹关联组件。
     */
    public ConversationExecutionAggregationService(PlatformConversationMessageMapper messageMapper,
                                                    PersistentOrchestrationExecutionService executionService) {
        this.messageMapper = messageMapper;
        this.executionService = executionService;
    }

    /**
     * 聚合恢复指定会话的消息历史流、关联的 Runtime 执行轨迹与应用上下文状态。
     *
     * @param user 当前登录用户
     * @param conversationId 会话唯一标识 ID
     * @return 包含 messages、executionId、execution 以及 applicationContext 的聚合 Map
     */
    public Map<String, Object> aggregate(SecurityUser user, String conversationId) {
        if (user == null || user.getTenantId() == null || conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("会话恢复参数无效");
        }
        List<PlatformConversationMessageEntity> records = messageMapper.selectList(new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                .eq(PlatformConversationMessageEntity::getTenantId, user.getTenantId())
                .eq(PlatformConversationMessageEntity::getConversationId, conversationId)
                .orderByAsc(PlatformConversationMessageEntity::getCreatedAt)
                .orderByAsc(PlatformConversationMessageEntity::getId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversationId);
        result.put("messages", records);
        String executionId = records.stream().map(PlatformConversationMessageEntity::getExecutionId)
                .filter(value -> value != null && !value.isBlank()).reduce((first, second) -> second).orElse(null);
        result.put("executionId", executionId);
        Map<String, Object> execution = executionId == null ? null : executionService.get(user, executionId);
        result.put("execution", execution);
        if (execution != null) {
            result.put("applicationContext", Map.of(
                    "applicationId", execution.getOrDefault("applicationId", ""),
                    "versionId", execution.getOrDefault("versionId", ""),
                    "executionType", execution.getOrDefault("executionType", "CONVERSATION"),
                    "status", execution.getOrDefault("status", "UNKNOWN")
            ));
        }
        return result;
    }
}

