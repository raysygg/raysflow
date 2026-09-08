package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.application.application.ApplicationContracts.ProtocolField;
import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEntrypointMapper;
import com.acme.agentstudio.infrastructure.workflow.ValidationIssue;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 工作流发布入口契约兼容性校验器（Workflow Entrypoint Validator）。
 * 发布前校验当前应用已开启的所有发布入口（API、Webhook、表单等）必填字段与工作流全局 InputSchema 是否相互匹配兼容。
 */
@Component
public class WorkflowEntrypointValidator {

    /** 应用入口 Mapper */
    private final ApplicationEntrypointMapper entrypointMapper;

    /** Jackson JSON 映射器 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入依赖 Mapper 与 JSON 工具。
     */
    public WorkflowEntrypointValidator(ApplicationEntrypointMapper entrypointMapper, ObjectMapper objectMapper) {
        this.entrypointMapper = entrypointMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 校验发布入口必填字段与工作流全局 InputSchema 的兼容性。
     *
     * @param tenantId 当前租户 ID
     * @param appId 编排应用 ID
     * @param graph 拟发布的工作流定义图
     * @return 兼容性问题列表 List<ValidationIssue>
     */
    public List<ValidationIssue> validate(Long tenantId, Long appId, GraphDefinition graph) {
        Set<String> workflowFields = new LinkedHashSet<>();
        graph.inputSchema().path("properties").fieldNames().forEachRemaining(workflowFields::add);
        List<ValidationIssue> issues = new ArrayList<>();

        for (ApplicationEntrypointEntity entrypoint : enabledEntrypoints(tenantId, appId)) {
            for (ProtocolField field : inputFields(entrypoint)) {
                if (field.required() && !workflowFields.contains(field.name())) {
                    issues.add(ValidationIssue.error(
                            "ENTRYPOINT_INPUT_INCOMPATIBLE",
                            null,
                            "inputSchema.properties." + field.name(),
                            "入口【" + entrypoint.getName() + "】要求的必填字段【" + field.name() + "】不在工作流 InputSchema 规范中。",
                            "请在画布基本信息中添加该输入字段，或在应用入口管理中修改入口的参数要求。"
                    ));
                }
            }
        }
        return List.copyOf(issues);
    }

    /** 查询租户下当前应用已禁/启用的入口列表 */
    private List<ApplicationEntrypointEntity> enabledEntrypoints(Long tenantId, Long appId) {
        return entrypointMapper.selectList(new LambdaQueryWrapper<ApplicationEntrypointEntity>()
                .eq(ApplicationEntrypointEntity::getTenantId, tenantId)
                .eq(ApplicationEntrypointEntity::getApplicationId, appId)
                .eq(ApplicationEntrypointEntity::getEnabled, true));
    }

    /** 解析入口配置的反序列化 ProtocolField 列表 */
    private List<ProtocolField> inputFields(ApplicationEntrypointEntity entrypoint) {
        try {
            return objectMapper.readValue(entrypoint.getInputSchemaJson(), new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalArgumentException("入口【" + entrypoint.getName() + "】的 JSON 输入契约解析失败，请检查或重新保存该入口配置。", exception);
        }
    }
}

