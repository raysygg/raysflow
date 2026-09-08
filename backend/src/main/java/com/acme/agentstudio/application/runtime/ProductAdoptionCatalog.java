package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.GoldenPathDefinition;
import com.acme.agentstudio.domain.runtime.model.MvpPhase;
import com.acme.agentstudio.domain.runtime.model.ProductAcceptanceTargets;
import com.acme.agentstudio.domain.runtime.model.ProductPhaseDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 产品采用与 MVP 阶段演进目录（Product Adoption Catalog）。
 * 存储平台 MVP 分期定义（MVP 1-5）、核心业务黄金路径（Golden Path）以及产品验收质量目标指标（TTFV 首次交付时长、RAG 忠实度、挂起恢复率等），
 * 供应用创建向导、产品运营面板与发布门禁复用。
 */
@Component
public class ProductAdoptionCatalog {

    /** 首次应用创建交付目标最大分钟数（TTFV） */
    private static final int TARGET_MAX_MINUTES_TO_FIRST_APP = 10;

    /** 任务完成率目标（80%） */
    private static final double TARGET_TASK_COMPLETION_RATE = 0.8D;

    /** RAG 忠实度（拒绝幻觉）率目标（90%） */
    private static final double TARGET_GROUNDED_ANSWER_RATE = 0.9D;

    /** 故障与挂起恢复成功率目标（95%） */
    private static final double TARGET_RECOVERY_RATE = 0.95D;

    /** 响应最大延迟毫秒数 */
    private static final long TARGET_MAX_LATENCY_MILLIS = 15_000L;

    /** 单次调用允许的最大成本预算（微元） */
    private static final long TARGET_MAX_COST_MICROS = 100_000L;

    /** MVP 阶段路线图定义列表 */
    private static final List<ProductPhaseDefinition> PHASES = List.of(
            phase(MvpPhase.MVP_1, "知识助手", List.of("Chat", "Prompt", "RAG", "工具", "会话"), List.of(), true),
            phase(MvpPhase.MVP_2, "可恢复 Agent", List.of("ReAct", "短期记忆", "检查点恢复"), List.of("MVP_1"), true),
            phase(MvpPhase.MVP_3, "可控流程", List.of("Plan", "人工介入", "评测门禁"), List.of("MVP_2"), true),
            phase(MvpPhase.MVP_4, "多智能体协作", List.of("角色", "协作拓扑", "共享上下文"), List.of("MVP_3"), true),
            phase(MvpPhase.MVP_5, "框架兼容", List.of("AutoGen", "LangGraph", "CAMEL", "AgentScope"), List.of("MVP_4"), false)
    );

    /** 核心业务黄金路径定义列表 */
    private static final List<GoldenPathDefinition> GOLDEN_PATHS = List.of(
            new GoldenPathDefinition(
                    "KNOWLEDGE_ASSISTANT",
                    "知识助手",
                    "业务员工",
                    List.of("创建应用", "绑定知识库", "配置 Prompt", "测试引用", "发布并对话"),
                    List.of("回答包含可追溯引用", "无命中时不编造结论", "会话可恢复")
            ),
            new GoldenPathDefinition(
                    "CONTRACT_REVIEW",
                    "带审批的合同审核",
                    "法务与业务审批人",
                    List.of("上传合同", "检索条款", "生成审核计划", "风险项人工确认", "输出审查报告"),
                    List.of("高风险动作必须确认", "每个结论可定位原文", "审批结果可审计")
            ),
            new GoldenPathDefinition(
                    "MULTI_AGENT_RESEARCH",
                    "多智能体研究",
                    "研究与分析团队",
                    List.of("定义角色", "选择协作拓扑", "配置共享上下文", "运行研究任务", "复核协作 Trace"),
                    List.of("委派深度有上限", "每个 Agent 有独立预算", "最终答案有明确所有者")
            )
    );

    /** 质量与体验验收指标目标 */
    private static final ProductAcceptanceTargets TARGETS = new ProductAcceptanceTargets(
            TARGET_MAX_MINUTES_TO_FIRST_APP,
            TARGET_TASK_COMPLETION_RATE,
            TARGET_GROUNDED_ANSWER_RATE,
            TARGET_RECOVERY_RATE,
            TARGET_MAX_LATENCY_MILLIS,
            TARGET_MAX_COST_MICROS
    );

    /**
     * 导出产品能力目录实体结构供运营诊断和向导展现。
     *
     * @return 产品定义目录对象 ProductDefinitionCatalog
     */
    public ProductDefinitionCatalog catalog() {
        return new ProductDefinitionCatalog(1, PHASES, GOLDEN_PATHS, TARGETS);
    }

    /**
     * 产品能力目录包含实体 Record。
     *
     * @param definitionVersion 目录 Schema 版本号
     * @param phases MVP 演进分期定义
     * @param goldenPaths 黄金路径列表
     * @param acceptanceTargets 验收指标标准
     */
    public record ProductDefinitionCatalog(
            int definitionVersion,
            List<ProductPhaseDefinition> phases,
            List<GoldenPathDefinition> goldenPaths,
            ProductAcceptanceTargets acceptanceTargets
    ) {
    }

    /** 内部快捷方法构建 MVP 阶段定义 */
    private static ProductPhaseDefinition phase(
            MvpPhase phase,
            String name,
            List<String> capabilities,
            List<String> dependencies,
            boolean visible
    ) {
        return new ProductPhaseDefinition(
                phase,
                name,
                capabilities,
                dependencies,
                List.of("黄金路径可完整执行", "失败和权限边界可观测", "发布检查可阻断不完整配置"),
                visible
        );
    }
}
