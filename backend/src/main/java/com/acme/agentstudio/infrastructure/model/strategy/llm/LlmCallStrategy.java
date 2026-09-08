package com.acme.agentstudio.infrastructure.model.strategy.llm;

import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * 基于 LangChain4j 框架体系的 LLM 大语言模型策略接口（LLM Call Strategy）。
 */
public interface LlmCallStrategy {

    /**
     * 根据模型连接契约创建 LangChain4j ChatLanguageModel 模型通道实例。
     *
     * @param connection 连接配置契约对象
     * @return LangChain4j ChatLanguageModel 实例
     */
    ChatLanguageModel createChatModel(RagModelConnectionResolver.ModelConnection connection);

    /**
     * 执行大模型单轮 Prompt 文本生成（默认直接调用 createChatModel(connection).generate(prompt)）。
     *
     * @param connection 连接配置契约对象
     * @param prompt 提示词文本
     * @param temperature 采样温度参数
     * @return 模型响应生成的回答字符串
     */
    default String generateText(RagModelConnectionResolver.ModelConnection connection, String prompt, double temperature) {
        return createChatModel(connection).generate(prompt);
    }

    /**
     * 判断当前策略是否匹配特定的大模型供应商（默认 OpenAI 策略返回 false，特化类重写返回 true）。
     *
     * @param provider 模型供应商枚举 ModelProvider
     * @return true 表示支持该供应商特化逻辑
     */
    default boolean supports(ModelProvider provider) {
        return false;
    }
}

