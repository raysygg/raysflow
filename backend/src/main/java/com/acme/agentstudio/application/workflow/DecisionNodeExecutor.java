package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 决策型工作流节点执行器（分类节点与路由规则节点）。
 * 负责调用 LLM 进行意图/内容文本分类（Classify），或者纯根据规则进行动态端口路由选择（Route）。
 */
@Service
public class DecisionNodeExecutor {

    /** LLM 大模型通用执行器 */
    private final LlmNodeExecutor llm;

    /** Jackson JSON 映射组件 */
    private final ObjectMapper mapper;

    /**
     * 构造函数注入模型执行器与 JSON 映射组件。
     */
    public DecisionNodeExecutor(LlmNodeExecutor llm, ObjectMapper mapper) {
        this.llm = llm;
        this.mapper = mapper;
    }

    /**
     * 调用大模型对输入文本或对话进行受限类别（Categories）判定与分类端口路由。
     *
     * @param tenantId 租户 ID
     * @param config 节点配置节点
     * @param values 运行时变量 Map
     * @param modelReference 模型依赖配置
     * @return 包含分类标签与路由端口的 Decision Result 对象
     */
    public Result classify(Long tenantId, JsonNode config, Map<String, Object> values,
                           WorkflowDependencySnapshot.ModelDependencyReference modelReference) {
        // 分类节点复用模型执行器，但将模型输出转换成受配置约束的分支端口。
        LlmNodeExecutor.Result response = llm.execute(tenantId, config, values, modelReference, LlmNodeExecutor.ModelDeltaListener.NOOP);
        String label = response.text().trim();

        if (label.startsWith("{") && label.endsWith("}")) {
            try {
                label = mapper.readTree(label).path("category").asText(label);
            } catch (Exception ignored) {
                // 允许解析 JSON 失败降级使用原始文本
            }
        }

        String port = config.path("categories").isArray() && config.path("categories").size() > 0 ? "default" : label;
        for (JsonNode category : config.path("categories")) {
            if (category.asText().equalsIgnoreCase(label)) {
                port = category.asText();
                break;
            }
        }

        return new Result(label, port, response.modelKey(), response.attempt());
    }

    /**
     * 规则路由判定（非模型调用，纯基于条件规则表进行分支分配）。
     *
     * @param config 节点配置节点
     * @param values 运行时变量 Map
     * @return 包含输入值与路由端口的 Decision Result 对象
     */
    public Result route(JsonNode config, Map<String, Object> values) {
        // 路由节点不调用模型，只按规则匹配输入；没有命中时使用配置的默认端口。
        String input = String.valueOf(values.getOrDefault(
                config.path("inputReference").asText(WorkflowVariableNames.VARIABLE_INPUT), ""
        ));
        String port = config.path("defaultPort").asText("default");

        for (JsonNode rule : config.path("rules")) {
            if (rule.path("equals").asText().equals(input)) {
                port = rule.path("port").asText(port);
                break;
            }
        }

        return new Result(input, port, null, 0);
    }

    /** 决策结果 Record */
    public record Result(String value, String port, String modelKey, int attempt) {
    }
}

