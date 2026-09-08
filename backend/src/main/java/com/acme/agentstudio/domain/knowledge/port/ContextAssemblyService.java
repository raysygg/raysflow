package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.ContextAssemblyResult;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate;

import java.util.List;

/**
 * ContextAssembly 业务服务接口。
 * 定义 ContextAssembly 相关的核心业务契约与流程接口。
 */
/** 将精确子块命中还原为受 Token 边界约束的父级上下文。 */
public interface ContextAssemblyService {
    ContextAssemblyResult assemble(RagEmbeddingProfile profile, Long indexGenerationId,
                                   List<RetrievalCandidate> candidates);
}
