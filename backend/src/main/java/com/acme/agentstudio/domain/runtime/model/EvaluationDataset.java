package com.acme.agentstudio.domain.runtime.model;

import java.util.List;
import java.util.Set;

/**
 * 自动化评测中心版本化固定测试集数据集 Record（Evaluation Dataset）。
 * 包含数据集 ID datasetId、名称 name、覆盖的评测维度集合 dimensions、测试用例列表 cases 与所有人用户 ID ownerActorId。
 *
 * @param datasetId 测试集数据集唯一 ID
 * @param name 测试集名称
 * @param dimensions 评估考察的评测维度集合 Set&lt;EvaluationDimension&gt;
 * @param cases 包含的测试用例列表 List&lt;PromptEvaluationCase&gt;
 * @param ownerActorId 数据集创建所有人账号 ID
 */
public record EvaluationDataset(
        String datasetId,
        String name,
        Set<EvaluationDimension> dimensions,
        List<PromptEvaluationCase> cases,
        String ownerActorId
) {
    /** 紧凑构造函数做输入断言校验 */
    public EvaluationDataset {
        if (datasetId == null || datasetId.isBlank() || name == null || name.isBlank()
                || ownerActorId == null || ownerActorId.isBlank()) {
            throw new IllegalArgumentException("评测数据集标识、名称和所有者不能为空");
        }
        dimensions = (dimensions == null) ? Set.of() : Set.copyOf(dimensions);
        cases = (cases == null) ? List.of() : List.copyOf(cases);
    }
}

