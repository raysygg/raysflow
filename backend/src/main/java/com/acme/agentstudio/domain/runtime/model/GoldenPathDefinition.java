package com.acme.agentstudio.domain.runtime.model;

import java.util.List;

/**
 * 面向租户最佳实践与产品引导的业务“黄金路径”标准定义实体 Record（Golden Path Definition）。
 * 包含路径编码 pathCode、名称 name、目标受众 audience、引导步骤列表 steps 以及验收标准列表 acceptanceCriteria。
 *
 * @param pathCode 黄金路径唯一编码
 * @param name 路径展示名称
 * @param audience 目标用户角色受众
 * @param steps 包含的操作步骤说明列表
 * @param acceptanceCriteria 阶段验收标准列表
 */
public record GoldenPathDefinition(
        String pathCode,
        String name,
        String audience,
        List<String> steps,
        List<String> acceptanceCriteria
) {
    /** 紧凑构造函数做输入属性断言校验 */
    public GoldenPathDefinition {
        if (pathCode == null || pathCode.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("黄金路径标识和名称不能为空");
        }
        steps = (steps == null) ? List.of() : List.copyOf(steps);
        acceptanceCriteria = (acceptanceCriteria == null) ? List.of() : List.copyOf(acceptanceCriteria);
    }
}

