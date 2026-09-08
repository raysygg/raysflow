package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 提示词模板 Prompt 资产的不可变版本快照实体 Record（Prompt Asset Version）。
 * 包含版本 ID versionId、递增版本号 versionNumber、模板内容 template、变量明细 variables、
 * 发布状态 status (PromptVersionStatus)、关联绑定 Release 实例 ID 集合 releaseBindings、创建人 createdBy 与创建时间 createdAt。
 *
 * @param versionId 版本唯一 ID
 * @param versionNumber 递增主版本号 (从 1 开始)
 * @param template 包含 {{var}} 插值的模板文本内容
 * @param variables 包含的变量定义列表 List&lt;PromptVariable&gt;
 * @param status 版本状态（PromptVersionStatus：DRAFT / PUBLISHED / ARCHIVED）
 * @param releaseBindings 强绑定消费此 Prompt 版本的不可变 Release ID 集合
 * @param createdBy 创建者用户账号 ID
 * @param createdAt 创建时间
 */
public record PromptAssetVersion(
        String versionId,
        int versionNumber,
        String template,
        List<PromptVariable> variables,
        PromptVersionStatus status,
        Set<String> releaseBindings,
        String createdBy,
        Instant createdAt
) {
    /** 紧凑构造函数做输入属性校验 */
    public PromptAssetVersion {
        if (versionId == null || versionId.isBlank() || versionNumber < 1 || template == null || template.isBlank()
                || createdBy == null || createdBy.isBlank() || status == null) {
            throw new IllegalArgumentException("Prompt 版本标识、模板、创建人和状态不能为空");
        }
        variables = (variables == null) ? List.of() : List.copyOf(variables);
        releaseBindings = (releaseBindings == null) ? Set.of() : Set.copyOf(releaseBindings);
        createdAt = (createdAt == null) ? Instant.now() : createdAt;
    }
}

