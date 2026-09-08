package com.acme.agentstudio.domain.runtime.model;

import java.util.List;
import java.util.Map;

/**
 * 应用创建与首次价值（Time to First Value）向导引导步骤定义 Record（Application Wizard Step）。
 * 包含步骤 Key、标题、描述、必填字段列表、默认参数 Map、阻断告警提示信息 List 以及是否可跳过标志 skippable。
 *
 * @param key 步骤唯一标识 Key
 * @param title 步骤中文标题
 * @param description 步骤说明文案
 * @param requiredFields 本步骤必填配置字段列表
 * @param defaults 默认初始化配置 Map
 * @param blockingMessages 阻止提交的校验错误提示列表
 * @param skippable 是否允许跳过该步骤
 */
public record ApplicationWizardStep(
        String key,
        String title,
        String description,
        List<String> requiredFields,
        Map<String, Object> defaults,
        List<String> blockingMessages,
        boolean skippable
) {
    /** 紧凑构造函数做断言校验与集合防空转换 */
    public ApplicationWizardStep {
        if (key == null || key.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("向导步骤标识和标题不能为空");
        }
        requiredFields = (requiredFields == null) ? List.of() : List.copyOf(requiredFields);
        defaults = (defaults == null) ? Map.of() : Map.copyOf(defaults);
        blockingMessages = (blockingMessages == null) ? List.of() : List.copyOf(blockingMessages);
    }
}

