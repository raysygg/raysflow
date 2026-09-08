package com.acme.agentstudio.infrastructure.rag.model;

import com.acme.agentstudio.domain.knowledge.model.QueryUnderstandingResult;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.port.QueryUnderstandingService;
import com.acme.agentstudio.infrastructure.model.ChatModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ModelQueryUnderstanding 业务服务接口。
 * 定义 ModelQueryUnderstanding 相关的核心业务契约与流程接口。
 */
@Component
/**
 * ModelQueryUnderstanding 业务逻辑服务接口。
 * 负责 ModelQueryUnderstanding 核心业务逻辑与流程编排。
 */
public class ModelQueryUnderstandingService implements QueryUnderstandingService {
    private static final Logger LOG = LoggerFactory.getLogger(ModelQueryUnderstandingService.class);
    private static final int MAX_REWRITE_LENGTH = 1000;

    private final ChatModelRegistry modelRegistry;

    public ModelQueryUnderstandingService(ChatModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

        /**
         * understand 方法。
         *
         * @param tenantId tenantId 参数
         * @param profile profile 参数
         * @param query query 参数
         * @return QueryUnderstandingResult 返回对象
         */
    @Override
    public QueryUnderstandingResult understand(Long tenantId, RagEmbeddingProfile profile, String query) {
        String original = query == null ? "" : query.trim();
        if (!profile.queryRewriteEnabled() || isBlank(profile.queryRewriteModelKey())) {
            return new QueryUnderstandingResult(original, "", false);
        }
        try {
            String rewritten = modelRegistry.getModel(tenantId, profile.queryRewriteModelKey())
                    .generate(prompt(original));
            String normalized = normalize(rewritten);
            return new QueryUnderstandingResult(original, normalized, !normalized.equals(original));
        } catch (RuntimeException exception) {
            LOG.warn("RAG 查询改写失败，继续使用原始查询，tenantId={}, profile={}", tenantId, profile.code());
            return new QueryUnderstandingResult(original, "", false);
        }
    }

    private String prompt(String query) {
        return "请将下面的问题改写为语义清晰、适合多语言向量检索的一句话。"
                + "不得补充原问题没有的信息，只输出改写结果。\n问题：" + query;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= MAX_REWRITE_LENGTH ? normalized : normalized.substring(0, MAX_REWRITE_LENGTH);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
