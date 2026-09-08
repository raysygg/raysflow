package com.acme.agentstudio.common.exception;

import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.model.ModelInvocationErrorCategory;

/**
 * RagEmbeddingInvocation 业务处理异常类。
 */
/**
 * 知识库 Embeddings 向量模型服务调用失败异常类。
 * 当知识库向量索引或检索过程中绑定的 Embedding 引擎遇到网络超时、凭证失效、维度不匹配等错误时抛出。
 * 为保障向量计算空间的一致性，系统禁止隐式降级或切换到其他不相容的向量模型。
 */
/**
 * RagEmbeddingInvocation 业务处理异常类。
 */
public class RagEmbeddingInvocationException extends RuntimeException {

    /** 模型调用错误归类（如 AUTHENTICATION_FAILED, TIMEOUT, UNREACHABLE 等） */
    private final ModelInvocationErrorCategory category;

    /** 目标模型来源信息 */
    private final RagModelSource modelSource;

    /**
     * 构造向量模型调用异常对象。
     *
     * @param category 错误分类枚举
     * @param modelSource 模型来源
     * @param message 友好中文提示文案
     * @param cause 底层引发异常的对象
     */
    public RagEmbeddingInvocationException(ModelInvocationErrorCategory category,
                                           RagModelSource modelSource,
                                           String message,
                                           Throwable cause) {
        super(message, cause);
        this.category = category;
        this.modelSource = modelSource;
    }

    /** 获取模型调用错误分类 */
    public ModelInvocationErrorCategory category() {
        return category;
    }

    /** 获取模型来源 */
    public RagModelSource modelSource() {
        return modelSource;
    }
}

