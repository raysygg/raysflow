package com.acme.agentstudio.domain.application.model;

import com.acme.agentstudio.domain.runtime.model.ApplicationWizardStep;
import java.util.List;

/**
 * 前端应用创建与发布可视化向导步骤定义实体 Record（Application Wizard Definition）。
 * 包含高级配置模式标志 advanced、向导步骤列表 steps (List&lt;ApplicationWizardStep&gt;)、
 * 中途进度恢复支持标志 resumeSupported 及新手默认选项使能标志 beginnerDefaultsEnabled。
 *
 * @param advanced 是否为面向高级开发者的全功能自由配置模式
 * @param steps 可视化步骤步骤链列表
 * @param resumeSupported 是否支持保存草稿并中途恢复引导流程
 * @param beginnerDefaultsEnabled 是否对新手用户启用智能预填默认模板值
 */
public record ApplicationWizardDefinition(
        boolean advanced,
        List<ApplicationWizardStep> steps,
        boolean resumeSupported,
        boolean beginnerDefaultsEnabled
) {
    /** 紧凑构造函数做输入步骤 List 防空保护 */
    public ApplicationWizardDefinition {
        steps = (steps == null) ? List.of() : List.copyOf(steps);
    }
}

