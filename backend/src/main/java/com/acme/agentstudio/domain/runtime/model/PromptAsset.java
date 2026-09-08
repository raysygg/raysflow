package com.acme.agentstudio.domain.runtime.model;

import java.util.List;

/**
 * 租户物理隔离的提示词模板资产实体 Record（Prompt Asset）。
 * 包含资产 ID assetId、租户 ID tenantId、资产名称 name、所有者用户 ID ownerActorId、包含的版本历史列表 versions 与软删除标记 deleted。
 *
 * @param assetId Prompt 资产唯一 ID
 * @param tenantId 归属租户物理 ID
 * @param name 提示词资产名称
 * @param ownerActorId 创建人账号 ID
 * @param versions 关联的版本明细列表 List&lt;PromptAssetVersion&gt;
 * @param deleted 是否已物理/逻辑删除
 */
public record PromptAsset(
        String assetId,
        long tenantId,
        String name,
        String ownerActorId,
        List<PromptAssetVersion> versions,
        boolean deleted
) {
    /** 紧凑构造函数做输入验证校验 */
    public PromptAsset {
        if (assetId == null || assetId.isBlank() || tenantId <= 0 || name == null || name.isBlank()
                || ownerActorId == null || ownerActorId.isBlank()) {
            throw new IllegalArgumentException("Prompt 资产标识、租户、名称和所有者不能为空");
        }
        versions = (versions == null) ? List.of() : List.copyOf(versions);
    }

    /**
     * 追加发布一个新版本的 Prompt 资产版本记录。
     *
     * @param version 新发布的 Prompt 资产版本实体
     * @return 包含新版本的全新不可变 PromptAsset 实例
     */
    public PromptAsset append(PromptAssetVersion version) {
        List<PromptAssetVersion> updated = new java.util.ArrayList<>(versions);
        updated.add(version);
        return new PromptAsset(assetId, tenantId, name, ownerActorId, updated, deleted);
    }
}

