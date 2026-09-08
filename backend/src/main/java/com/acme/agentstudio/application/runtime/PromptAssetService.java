package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.PromptAsset;
import com.acme.agentstudio.domain.runtime.model.PromptAssetVersion;
import com.acme.agentstudio.domain.runtime.model.PromptVariable;
import com.acme.agentstudio.domain.runtime.model.PromptVersionStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 提示词资产与版本管理应用服务（Prompt Asset Service）。
 * 负责 Prompt 提示词资产的创建（Create）、草稿修改（Save Draft）、版本号管理（Versioning）、
 * 生产发布门禁绑定（Publishing）以及指定 Release 绑定的只读 Prompt 版本检索。
 */
@Service
public class PromptAssetService {

    /** 内存 Prompt 资产存储 Map */
    private final Map<String, PromptAsset> assets = new ConcurrentHashMap<>();

    /**
     * 创建一条全新的 Prompt 资产实体，并产生首个 DRAFT 状态版本号 (v1)。
     *
     * @param tenantId 租户 ID
     * @param name Prompt 资产名称
     * @param ownerActorId 所有者账号 ID
     * @param template 提示词模板字符串
     * @param variables 变量占位符定义列表
     * @return 新建的 Prompt 资产实体 PromptAsset
     */
    public PromptAsset create(
            long tenantId,
            String name,
            String ownerActorId,
            String template,
            List<PromptVariable> variables
    ) {
        String assetId = UUID.randomUUID().toString();
        PromptAsset asset = new PromptAsset(assetId, tenantId, name, ownerActorId, List.of(), false);
        PromptAssetVersion draft = new PromptAssetVersion(
                UUID.randomUUID().toString(),
                1,
                template,
                variables,
                PromptVersionStatus.DRAFT,
                Set.of(),
                ownerActorId,
                null
        );
        PromptAsset result = asset.append(draft);
        assets.put(assetId, result);
        return result;
    }

    /**
     * 在已有 Prompt 资产下保存修改并生成新的 DRAFT 草稿版本号。
     *
     * @param tenantId 租户 ID
     * @param assetId 资产 ID
     * @param actorId 操作者 ID
     * @param template 提示词模板
     * @param variables 变量列表
     * @return 更新后的 Prompt 资产实体 PromptAsset
     */
    public PromptAsset saveDraft(
            long tenantId,
            String assetId,
            String actorId,
            String template,
            List<PromptVariable> variables
    ) {
        PromptAsset current = requireOwned(tenantId, assetId, actorId);
        int nextVersion = current.versions().stream()
                .mapToInt(PromptAssetVersion::versionNumber)
                .max()
                .orElse(0) + 1;

        PromptAssetVersion draft = new PromptAssetVersion(
                UUID.randomUUID().toString(),
                nextVersion,
                template,
                variables,
                PromptVersionStatus.DRAFT,
                Set.of(),
                actorId,
                null
        );
        PromptAsset result = current.append(draft);
        assets.put(assetId, result);
        return result;
    }

    /**
     * 将 DRAFT 草稿状态的 Prompt 版本发布为 PUBLISHED，并绑定特定的线上 releaseId 生产指针。
     *
     * @param tenantId 租户 ID
     * @param assetId 资产 ID
     * @param actorId 操作者 ID
     * @param versionId 目标版本 ID
     * @param releaseBindings 绑定的 Release 指针集合 Set&lt;String&gt;
     * @return 发布后的 Prompt 资产实体 PromptAsset
     */
    public PromptAsset publish(
            long tenantId,
            String assetId,
            String actorId,
            String versionId,
            Set<String> releaseBindings
    ) {
        PromptAsset current = requireOwned(tenantId, assetId, actorId);
        PromptAssetVersion target = current.versions().stream()
                .filter(version -> version.versionId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到指定的 Prompt 版本号：" + versionId));

        if (target.status() != PromptVersionStatus.DRAFT) {
            throw new IllegalStateException("只有处于 DRAFT 草稿状态的 Prompt 版本才允许发布。");
        }

        PromptAssetVersion published = new PromptAssetVersion(
                target.versionId(),
                target.versionNumber(),
                target.template(),
                target.variables(),
                PromptVersionStatus.PUBLISHED,
                releaseBindings,
                target.createdBy(),
                target.createdAt()
        );
        PromptAsset result = replaceVersion(current, published);
        assets.put(assetId, result);
        return result;
    }

    /**
     * 校验并获取特定应用生产发布版本（releaseId）所绑定的已发布 Prompt 版本实体。
     *
     * @param tenantId 租户 ID
     * @param assetId 资产 ID
     * @param releaseId 关联的应用 Release ID
     * @return 绑定的已发布 Prompt 版本实体 PromptAssetVersion
     */
    public PromptAssetVersion requirePublished(long tenantId, String assetId, String releaseId) {
        PromptAsset asset = requireTenant(tenantId, assetId);
        return asset.versions().stream()
                .filter(version -> version.status() == PromptVersionStatus.PUBLISHED
                        && version.releaseBindings().contains(releaseId))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("指定 Prompt 资产尚未绑定可用于该 releaseId 的已发布版本。"));
    }

    /** 替换资产列表中的特定版本实体 */
    private PromptAsset replaceVersion(PromptAsset asset, PromptAssetVersion replacement) {
        List<PromptAssetVersion> versions = asset.versions().stream()
                .map(version -> version.versionId().equals(replacement.versionId()) ? replacement : version)
                .toList();
        return new PromptAsset(
                asset.assetId(),
                asset.tenantId(),
                asset.name(),
                asset.ownerActorId(),
                versions,
                asset.deleted()
        );
    }

    /** 所有者权限校验 */
    private PromptAsset requireOwned(long tenantId, String assetId, String actorId) {
        PromptAsset asset = requireTenant(tenantId, assetId);
        if (!asset.ownerActorId().equals(actorId)) {
            throw new IllegalArgumentException("当前用户无权修改该 Prompt 资产。");
        }
        return asset;
    }

    /** 租户归属校验 */
    private PromptAsset requireTenant(long tenantId, String assetId) {
        PromptAsset asset = assets.get(assetId);
        if (asset == null || asset.tenantId() != tenantId || asset.deleted()) {
            throw new IllegalArgumentException("指定 Prompt 资产不存在或无权访问。");
        }
        return asset;
    }
}

