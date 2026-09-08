package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.SemanticChunkPlan;
import dev.langchain4j.data.document.Document;

/**
 * SemanticChunking 业务服务接口。
 * 定义 SemanticChunking 相关的核心业务契约与流程接口。
 */
/** 与文件类型无关的上下文感知切块边界。 */
public interface SemanticChunkingService {
    SemanticChunkPlan split(Document document, String documentTitle);
}

