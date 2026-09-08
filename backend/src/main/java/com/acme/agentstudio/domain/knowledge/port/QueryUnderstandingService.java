package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.QueryUnderstandingResult;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;

/**
 * QueryUnderstanding 业务服务接口。
 * 定义 QueryUnderstanding 相关的核心业务契约与流程接口。
 */
/** 可选语义查询改写边界。 */
public interface QueryUnderstandingService {
    QueryUnderstandingResult understand(Long tenantId, RagEmbeddingProfile profile, String query);
}

