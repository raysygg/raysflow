package com.acme.agentstudio.application.knowledge;

import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.QualityFinding;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.QualityFindingStatus;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.QualitySeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文档解析质量巡检规则服务。
 * 负责在文档解析分块完成后对文本完整性、内容空洞、切块数量及语言覆盖范围进行可解释规则巡检，输出阻断级（Blocker）与警告级（Warning）缺陷项。
 */
public class KnowledgeQualityRuleService {

    /** 目标类型：文档 */
    private static final String TARGET_DOCUMENT = "DOCUMENT";

    /** 错误码：解析失败 */
    private static final String CODE_PARSE_FAILED = "PARSE_FAILED";

    /** 错误码：内容为空 */
    private static final String CODE_EMPTY_CONTENT = "EMPTY_CONTENT";

    /** 错误码：语言不受支持 */
    private static final String CODE_LANGUAGE_UNSUPPORTED = "LANGUAGE_UNSUPPORTED";

    /** 错误码：分块为空 */
    private static final String CODE_CHUNK_EMPTY = "CHUNK_EMPTY";

    /**
     * 巡检文档质量指标并生成缺陷 Findings 列表。
     *
     * @param input 文档质量巡检入参（包含解析状态、切块数、正文等）
     * @return 识别出的质量问题明细列表
     */
    public List<QualityFinding> inspect(DocumentQualityInput input) {
        List<QualityFinding> findings = new ArrayList<>();
        if (!input.parsed()) {
            findings.add(blocker(CODE_PARSE_FAILED, input.documentId(), "文档解析失败"));
            return findings;
        }
        if (input.normalizedContent().isBlank()) {
            findings.add(blocker(CODE_EMPTY_CONTENT, input.documentId(), "文档没有可检索内容"));
        }
        if (!input.languageSupported()) {
            findings.add(warning(CODE_LANGUAGE_UNSUPPORTED, input.documentId(), "文档语言暂不在当前 Profile 覆盖范围"));
        }
        if (input.chunkCount() == 0) {
            findings.add(blocker(CODE_CHUNK_EMPTY, input.documentId(), "文档没有生成有效分块"));
        }
        return findings;
    }

    /**
     * 构造阻断级（BLOCKER）缺陷项。
     */
    private QualityFinding blocker(String code, Long documentId, String reason) {
        return new QualityFinding(code, QualitySeverity.BLOCKER, TARGET_DOCUMENT, documentId,
                reason, "修复文档后重新检查", QualityFindingStatus.OPEN, null);
    }

    /**
     * 构造警告级（WARNING）缺陷项。
     */
    private QualityFinding warning(String code, Long documentId, String reason) {
        return new QualityFinding(code, QualitySeverity.WARNING, TARGET_DOCUMENT, documentId,
                reason, "调整语言或检索配置后重新检查", QualityFindingStatus.OPEN, null);
    }

    /** 文档质量巡检输入参数契约 Record */
    public record DocumentQualityInput(Long documentId, boolean parsed, String normalizedContent,
                                      boolean languageSupported, int chunkCount) {
        public DocumentQualityInput {
            normalizedContent = normalizedContent == null ? "" : normalizedContent;
            chunkCount = Math.max(0, chunkCount);
        }
    }
}

