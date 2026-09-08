package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformConversationEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformConversationMessageEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionContextEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationVersionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformConversationMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformConversationMessageMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionContextMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 运行时会话生命周期应用服务（Conversation Runtime Service）。
 * 负责会话的创建（Create）、消息追加（Append Message）、归档与恢复（Archive/Restore）、
 * 分支复制（Branch）、上下文清理（Clear Context）以及导出（Export）全流程管理。
 */
@Service
public class ConversationRuntimeService {

    /** 默认活跃会话状态标识 */
    private static final String STATUS_ACTIVE = "ACTIVE";

    /** 用户角色标识常量 */
    private static final String ROLE_USER = "USER";

    /** 会话实体 Mapper */
    private final PlatformConversationMapper conversationMapper;

    /** 消息实体 Mapper */
    private final PlatformConversationMessageMapper messageMapper;

    /** 发布版本 Mapper */
    private final OrchestrationVersionMapper versionMapper;

    /** 运行上下文 Mapper */
    private final PlatformExecutionContextMapper executionContextMapper;

    /**
     * 构造函数注入依赖项。
     */
    public ConversationRuntimeService(
            PlatformConversationMapper conversationMapper,
            PlatformConversationMessageMapper messageMapper,
            OrchestrationVersionMapper versionMapper,
            PlatformExecutionContextMapper executionContextMapper
    ) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.versionMapper = versionMapper;
        this.executionContextMapper = executionContextMapper;
    }

    /**
     * 将 Runtime 合同中的发布版本业务标识解析为内部数据库主键，并创建会话。
     *
     * @param tenantId 租户 ID
     * @param appId 应用 ID
     * @param releaseVersionId 发布版本业务字符串 ID
     * @param actorId 操作者账号 ID
     * @param conversationId 指定会话 ID（可选）
     * @return PlatformConversationEntity 会话实体
     */
    @Transactional
    public PlatformConversationEntity create(
            long tenantId,
            long appId,
            String releaseVersionId,
            long actorId,
            String conversationId
    ) {
        Long versionId = resolveVersionId(tenantId, appId, releaseVersionId);
        return create(tenantId, appId, versionId, actorId, conversationId);
    }

    /**
     * 创建或关联已有会话实体，确保应用与版本绑定契约正确。
     *
     * @param tenantId 租户 ID
     * @param appId 应用 ID
     * @param versionId 发布版本数据库主键 ID
     * @param actorId 操作者账号 ID
     * @param conversationId 会话 ID
     * @return PlatformConversationEntity 会话实体
     */
    @Transactional
    public PlatformConversationEntity create(
            long tenantId,
            long appId,
            Long versionId,
            long actorId,
            String conversationId
    ) {
        requirePositive(tenantId, "租户");
        requirePositive(appId, "应用");
        requirePositive(actorId, "操作者");

        String id = (conversationId == null || conversationId.isBlank()) ? UUID.randomUUID().toString() : conversationId;
        PlatformConversationEntity existing = conversationMapper.selectOne(new LambdaQueryWrapper<PlatformConversationEntity>()
                .eq(PlatformConversationEntity::getTenantId, tenantId)
                .eq(PlatformConversationEntity::getConversationId, id));

        if (existing != null) {
            if (!Long.valueOf(appId).equals(existing.getAppId())) {
                throw new IllegalArgumentException("指定会话不属于当前所选应用。");
            }
            if (existing.getVersionId() != null && !existing.getVersionId().equals(versionId)) {
                throw new IllegalArgumentException("当前会话已绑定至其他发布版本，请开启新会话后再继续对话。");
            }
            if (existing.getVersionId() == null && versionId != null) {
                existing.setVersionId(versionId);
                existing.setUpdatedAt(LocalDateTime.now());
                conversationMapper.updateById(existing);
            }
            return requireAccess(existing, tenantId, actorId);
        }

        PlatformConversationEntity conversation = new PlatformConversationEntity();
        conversation.setTenantId(tenantId);
        conversation.setConversationId(id);
        conversation.setAppId(appId);
        conversation.setVersionId(versionId);
        conversation.setStatus(STATUS_ACTIVE);
        conversation.setCreatedBy(actorId);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    /**
     * 在指定会话中追加一条新消息。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者 ID
     * @param conversationId 会话 ID
     * @param messageId 消息 ID
     * @param executionId 关联运行执行 ID
     * @param roleCode 角色编码（USER/ASSISTANT/SYSTEM）
     * @param contentJson 消息正文 JSON
     * @return 追加的消息实体 PlatformConversationMessageEntity
     */
    @Transactional
    public PlatformConversationMessageEntity appendMessage(
            long tenantId,
            long actorId,
            String conversationId,
            String messageId,
            String executionId,
            String roleCode,
            String contentJson
    ) {
        PlatformConversationEntity conversation = requireConversation(tenantId, actorId, conversationId);
        if (BusinessStatus.ARCHIVED.equals(conversation.getStatus())) {
            throw new IllegalStateException("已归档的会话无法继续追加新消息。");
        }

        PlatformConversationMessageEntity message = new PlatformConversationMessageEntity();
        message.setTenantId(tenantId);
        message.setConversationId(conversationId);
        message.setMessageId(messageId == null || messageId.isBlank() ? UUID.randomUUID().toString() : messageId);
        message.setExecutionId(executionId);
        message.setRoleCode(roleCode == null || roleCode.isBlank() ? ROLE_USER : roleCode);
        message.setContentJson(contentJson == null ? "{}" : contentJson);
        message.setStatus(STATUS_ACTIVE);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return message;
    }

    /**
     * 将指定会话置为归档（ARCHIVED）状态。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者 ID
     * @param conversationId 会话 ID
     * @return 归档后的会话实体
     */
    @Transactional
    public PlatformConversationEntity archive(long tenantId, long actorId, String conversationId) {
        PlatformConversationEntity conversation = requireConversation(tenantId, actorId, conversationId);
        conversation.setStatus(BusinessStatus.ARCHIVED);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return conversation;
    }

    /**
     * 将归档的会话重新恢复为活跃（ACTIVE）状态。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者 ID
     * @param conversationId 会话 ID
     * @return 恢复后的会话实体
     */
    @Transactional
    public PlatformConversationEntity restore(long tenantId, long actorId, String conversationId) {
        PlatformConversationEntity conversation = requireConversation(tenantId, actorId, conversationId);
        conversation.setStatus(STATUS_ACTIVE);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return conversation;
    }

    /**
     * 导出完整会话信息、消息记录与关联的 Run 运行状态。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者 ID
     * @param conversationId 会话 ID
     * @return 包含会话、消息列表与关联运行列表的导出 Map
     */
    public Map<String, Object> export(long tenantId, long actorId, String conversationId) {
        PlatformConversationEntity conversation = requireConversation(tenantId, actorId, conversationId);
        List<PlatformConversationMessageEntity> messages = messageMapper.selectList(new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                .eq(PlatformConversationMessageEntity::getTenantId, tenantId)
                .eq(PlatformConversationMessageEntity::getConversationId, conversationId)
                .ne(PlatformConversationMessageEntity::getStatus, BusinessStatus.ARCHIVED)
                .orderByAsc(PlatformConversationMessageEntity::getCreatedAt));

        List<PlatformConversationMessageEntity> allMessages = messageMapper.selectList(new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                .eq(PlatformConversationMessageEntity::getTenantId, tenantId)
                .eq(PlatformConversationMessageEntity::getConversationId, conversationId)
                .orderByAsc(PlatformConversationMessageEntity::getCreatedAt));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversation", conversation);
        result.put("messages", messages);
        result.put("runs", relatedRuns(tenantId, allMessages));
        return result;
    }

    /**
     * 删除会话上下文消息，但保留会话和 Run 事实，避免清理操作破坏运行审计。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者 ID
     * @param conversationId 会话 ID
     * @return 清空后的会话实体
     */
    @Transactional
    public PlatformConversationEntity clearContext(long tenantId, long actorId, String conversationId) {
        PlatformConversationEntity conversation = requireConversation(tenantId, actorId, conversationId);
        List<PlatformConversationMessageEntity> messages = messageMapper.selectList(new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                .eq(PlatformConversationMessageEntity::getTenantId, tenantId)
                .eq(PlatformConversationMessageEntity::getConversationId, conversationId)
                .ne(PlatformConversationMessageEntity::getStatus, BusinessStatus.ARCHIVED));

        messages.forEach(message -> message.setStatus(BusinessStatus.ARCHIVED));
        messages.forEach(messageMapper::updateById);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return conversation;
    }

    /**
     * 返回当前用户可见的 Runtime 会话摘要，包含从首条消息提取的主题名称。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者账号 ID
     * @return 会话摘要 Map 列表
     */
    public List<Map<String, Object>> list(long tenantId, long actorId) {
        requirePositive(tenantId, "租户");
        requirePositive(actorId, "操作者");

        return conversationMapper.selectList(new LambdaQueryWrapper<PlatformConversationEntity>()
                        .eq(PlatformConversationEntity::getTenantId, tenantId)
                        .eq(PlatformConversationEntity::getCreatedBy, actorId)
                        .orderByDesc(PlatformConversationEntity::getUpdatedAt))
                .stream()
                .map(conversation -> {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("id", conversation.getConversationId());
                    summary.put("conversationId", conversation.getConversationId());
                    summary.put("appId", conversation.getAppId());
                    summary.put("versionId", conversation.getVersionId());
                    summary.put("status", conversation.getStatus());
                    summary.put("createdAt", conversation.getCreatedAt());
                    summary.put("updatedAt", conversation.getUpdatedAt());

                    PlatformConversationMessageEntity firstUserMsg = messageMapper.selectOne(
                            new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                                    .eq(PlatformConversationMessageEntity::getTenantId, tenantId)
                                    .eq(PlatformConversationMessageEntity::getConversationId, conversation.getConversationId())
                                    .eq(PlatformConversationMessageEntity::getRoleCode, ROLE_USER)
                                    .orderByAsc(PlatformConversationMessageEntity::getCreatedAt)
                                    .last("LIMIT 1")
                    );

                    String sessionName = "新对话";
                    if (firstUserMsg != null && firstUserMsg.getContentJson() != null) {
                        String content = firstUserMsg.getContentJson();
                        try {
                            if (content.startsWith("{")) {
                                int idx = content.indexOf("\"text\":\"");
                                if (idx != -1) {
                                    int start = idx + 8;
                                    int end = content.indexOf("\"", start);
                                    if (end != -1) {
                                        content = content.substring(start, end);
                                    }
                                } else {
                                    int inputIdx = content.indexOf("\"input\":\"");
                                    if (inputIdx != -1) {
                                        int start = inputIdx + 9;
                                        int end = content.indexOf("\"", start);
                                        if (end != -1) {
                                            content = content.substring(start, end);
                                        }
                                    }
                                }
                            }
                            content = content.replace("\\n", " ").replace("\\\"", "\"").trim();
                            if (!content.isBlank()) {
                                sessionName = content.length() > 22 ? content.substring(0, 22) + "..." : content;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    summary.put("name", sessionName);
                    return summary;
                })
                .toList();
    }

    /**
     * 复制现有会话并分叉产生新会话（Branch Conversation）。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者 ID
     * @param sourceConversationId 源会话 ID
     * @param newConversationId 新会话 ID
     * @return 新生成的会话实体
     */
    @Transactional
    public PlatformConversationEntity branch(
            long tenantId,
            long actorId,
            String sourceConversationId,
            String newConversationId
    ) {
        PlatformConversationEntity source = requireConversation(tenantId, actorId, sourceConversationId);
        PlatformConversationEntity target = create(tenantId, source.getAppId(), source.getVersionId(), actorId, newConversationId);

        List<PlatformConversationMessageEntity> messages = messageMapper.selectList(new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                .eq(PlatformConversationMessageEntity::getTenantId, tenantId)
                .eq(PlatformConversationMessageEntity::getConversationId, sourceConversationId)
                .ne(PlatformConversationMessageEntity::getStatus, BusinessStatus.ARCHIVED)
                .orderByAsc(PlatformConversationMessageEntity::getCreatedAt));

        for (PlatformConversationMessageEntity message : messages) {
            appendMessage(
                    tenantId,
                    actorId,
                    target.getConversationId(),
                    UUID.randomUUID().toString(),
                    null,
                    message.getRoleCode(),
                    message.getContentJson()
            );
        }
        return target;
    }

    /** 校验会话存在性与归属权限 */
    private PlatformConversationEntity requireConversation(long tenantId, long actorId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("会话标识 conversationId 不能为空。");
        }
        PlatformConversationEntity conversation = conversationMapper.selectOne(new LambdaQueryWrapper<PlatformConversationEntity>()
                .eq(PlatformConversationEntity::getTenantId, tenantId)
                .eq(PlatformConversationEntity::getConversationId, conversationId));
        return requireAccess(conversation, tenantId, actorId);
    }

    /** 从消息关联的执行 ID 查询当前租户的 Run 摘要 */
    private List<ConversationRunSummary> relatedRuns(long tenantId, List<PlatformConversationMessageEntity> messages) {
        List<String> executionIds = messages.stream()
                .map(PlatformConversationMessageEntity::getExecutionId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (executionIds.isEmpty()) {
            return List.of();
        }
        return executionContextMapper.selectList(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                        .eq(PlatformExecutionContextEntity::getTenantId, tenantId)
                        .in(PlatformExecutionContextEntity::getExecutionId, executionIds)
                        .orderByDesc(PlatformExecutionContextEntity::getStartedAt))
                .stream()
                .map(run -> new ConversationRunSummary(
                        run.getExecutionId(),
                        run.getAppId(),
                        run.getVersionId(),
                        run.getStatus(),
                        run.getStartedAt(),
                        run.getFinishedAt(),
                        run.getErrorCode(),
                        run.getErrorMessage()
                ))
                .toList();
    }

    /** 会话页使用的稳定 Run 摘要 Record */
    public record ConversationRunSummary(
            String executionId,
            Long appId,
            Long versionId,
            String status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String errorCode,
            String errorMessage
    ) {
    }

    /** 权限与归属校验 */
    private PlatformConversationEntity requireAccess(PlatformConversationEntity conversation, long tenantId, long actorId) {
        if (conversation == null
                || !Long.valueOf(tenantId).equals(conversation.getTenantId())
                || !Long.valueOf(actorId).equals(conversation.getCreatedBy())) {
            throw new IllegalArgumentException("指定会话不存在或无权访问。");
        }
        return conversation;
    }

    /** 正数校验工具 */
    private void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + "标识必须大于零。");
        }
    }

    /** 解析发布版本主键 ID */
    private Long resolveVersionId(long tenantId, long appId, String releaseVersionId) {
        if (releaseVersionId == null || releaseVersionId.isBlank()) {
            return null;
        }
        OrchestrationVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, tenantId)
                .eq(OrchestrationVersionEntity::getAppId, appId)
                .eq(OrchestrationVersionEntity::getVersionId, releaseVersionId));
        if (version == null) {
            throw new IllegalArgumentException("指定的发布版本不存在或不属于当前应用。");
        }
        return version.getId();
    }
}

