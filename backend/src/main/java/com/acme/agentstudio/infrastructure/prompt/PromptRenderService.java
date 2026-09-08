package com.acme.agentstudio.infrastructure.prompt;

import com.acme.agentstudio.domain.common.ApplicationMessages;
import com.acme.agentstudio.domain.common.PromptPlaceholders;
import com.acme.agentstudio.domain.knowledge.model.RagSearchResult;
import com.acme.agentstudio.infrastructure.persistence.entity.AgentProfileEntity;
import com.acme.agentstudio.infrastructure.rag.RagAnswerGuard;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Prompt 渲染与 RAG 背景上下文组装服务（Prompt Render Service）。
 * 负责解析 AgentProfileEntity 绑定的提示词模板（Prompt Template），
 * 填充变量 `{{userMessage}}`、`{{ragContext}}`，自动注入 Grounding 回答防幻觉约束规则（GROUNDING_RULES），
 * 并处理知识片段为空时的安全降级拦截提示。
 */
@Service
public class PromptRenderService {

    /** 强 Grounding 事实验证回答约束规则 */
    private static final String GROUNDING_RULES = """
            【回答约束】
            1. 只能依据下方“背景知识”回答，禁止使用常识或库外信息补充事实。
            2. 若背景知识为空，或不足以支撑结论，必须明确拒答，不能猜测。
            3. 回答中引用事实时，使用 [来源N] 标注对应知识片段。
            4. 不要编造文档编号、金额、日期、条款或审批结论。
            """;

    /**
     * 根据 Agent 配置实体、用户输入与 RAG 检索结果渲染系统提示词。
     *
     * @param agent Agent 配置实体 AgentProfileEntity
     * @param userMessage 用户发送的消息字符串
     * @param references RAG 检索到的参考文本片段列表 List&lt;RagSearchResult&gt;
     * @return 渲染后的最终 Prompt 字符串
     */
    public String render(AgentProfileEntity agent, String userMessage, List<RagSearchResult> references) {
        if (agent == null || agent.getPromptTemplate() == null || agent.getPromptTemplate().isBlank()) {
            throw new IllegalArgumentException(ApplicationMessages.AGENT_PROMPT_TEMPLATE_REQUIRED);
        }
        return renderTemplate(agent.getPromptTemplate(), userMessage, references);
    }

    /**
     * 基础渲染逻辑：替换模版占位符并拼装 Grounding 规则与空知识库提示。
     *
     * @param promptTemplate 提示词模版字符串
     * @param userMessage 用户消息
     * @param references 知识参考列表
     * @return 最终 Prompt 字符串
     */
    public String renderTemplate(String promptTemplate, String userMessage, List<RagSearchResult> references) {
        if (promptTemplate == null || promptTemplate.isBlank()) {
            throw new IllegalArgumentException(ApplicationMessages.AGENT_PROMPT_TEMPLATE_REQUIRED);
        }

        String safeUserMessage = (userMessage == null) ? "" : userMessage;
        String referenceBlock = buildReferenceBlock(references);

        String rendered = promptTemplate;
        boolean hasExplicitUserMessage = promptTemplate.contains(PromptPlaceholders.USER_MESSAGE);
        boolean hasExplicitRagContext = promptTemplate.contains(PromptPlaceholders.RAG_CONTEXT);

        if (hasExplicitUserMessage) {
            rendered = rendered.replace(PromptPlaceholders.USER_MESSAGE, safeUserMessage);
        }
        if (hasExplicitRagContext) {
            rendered = rendered.replace(PromptPlaceholders.RAG_CONTEXT, referenceBlock);
        }

        // 兜底追加：若模板未显式使用 {{rag_context}} 占位符且存在检索片段，自动将【背景知识】追加在后方
        if (!hasExplicitRagContext && references != null && !references.isEmpty()) {
            rendered = rendered + "\n\n【背景知识】\n" + referenceBlock;
        }

        // 兜底追加：若模板未显式使用 {{user_message}} 且用户输入非空，自动将【用户提问】追加在后方
        if (!hasExplicitUserMessage && !safeUserMessage.isBlank()) {
            rendered = rendered + "\n\n【用户提问】\n" + safeUserMessage;
        }

        if (!rendered.contains("【回答约束】")) {
            rendered = GROUNDING_RULES + "\n" + rendered;
        }

        if (references == null || references.isEmpty()) {
            rendered = rendered + "\n\n【系统提示】背景知识为空。请直接回复：" + RagAnswerGuard.NO_HIT_REPLY;
        }

        return rendered;
    }

    /**
     * 将 RAG 检索到的参考片段转换为带有 [来源N] 编号、来源文件名与相似度分数的格式化文本块。
     *
     * @param references RAG 检索结果列表
     * @return 格式化的文本块字符串
     */
    public String buildReferenceBlock(List<RagSearchResult> references) {
        if (references == null || references.isEmpty()) {
            return "（无命中知识片段）";
        }

        return IntStream.range(0, references.size())
                .mapToObj(index -> {
                    RagSearchResult item = references.get(index);
                    return "[来源" + (index + 1) + "] " + item.sourceLabel()
                            + "，相似度=" + String.format("%.3f", item.score()) + "\n" + item.text();
                })
                .collect(Collectors.joining("\n\n"));
    }
}

