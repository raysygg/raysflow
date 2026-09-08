package com.acme.agentstudio.application.lifecycle;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleException;
import com.acme.agentstudio.infrastructure.persistence.entity.MarketplaceInstallationEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.MarketplaceItemEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationDraftRevisionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformToolConnectorEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RagEmbeddingProfileEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.MarketplaceInstallationMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.MarketplaceItemMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationDraftRevisionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformToolConnectorMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RagEmbeddingProfileMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 商业化能力集市（Marketplace）模板安装与三方合并（3-way Merge）升级服务。
 * 负责解析集市发布模板 Manifest 清单、对租户环境下的模型/知识库/工具依赖做Preflight预检与替换，基于模板 Baseline, 租户 Draft 与新 Template 执行无损三方平滑升级。
 */
@Service
public class MarketplaceApplicationInstallationService {

    /** 状态：已就绪 */
    private static final String READY = "READY";

    /** 状态：草稿已创建 */
    private static final String DRAFT_CREATED = "DRAFT_CREATED";

    /** 来源类型：能力集市 */
    private static final String SOURCE_MARKETPLACE = "MARKETPLACE";

    /** 状态：活动 */
    private static final String ACTIVE = "ACTIVE";

    /** 能力集市项目 Mapper */
    private final MarketplaceItemMapper itemMapper;

    /** 集市安装记录 Mapper */
    private final MarketplaceInstallationMapper installationMapper;

    /** 工作流应用 Mapper */
    private final OrchestrationAppMapper appMapper;

    /** 草稿版本 Mapper */
    private final OrchestrationDraftRevisionMapper draftMapper;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /** AI 模型配置 Persistence Mapper */
    private final SysModelConfigMapper modelMapper;

    /** 知识库向量配置 Persistence Mapper */
    private final RagEmbeddingProfileMapper knowledgeProfileMapper;

    /** 工具连接器 Persistence Mapper */
    private final PlatformToolConnectorMapper toolConnectorMapper;

    /**
     * 构造函数注入安装升级服务所需依赖组件。
     */
    public MarketplaceApplicationInstallationService(MarketplaceItemMapper itemMapper,
                                                     MarketplaceInstallationMapper installationMapper,
                                                     OrchestrationAppMapper appMapper,
                                                     OrchestrationDraftRevisionMapper draftMapper,
                                                     ObjectMapper objectMapper,
                                                     SysModelConfigMapper modelMapper,
                                                     RagEmbeddingProfileMapper knowledgeProfileMapper,
                                                     PlatformToolConnectorMapper toolConnectorMapper) {
        this.itemMapper = itemMapper;
        this.installationMapper = installationMapper;
        this.appMapper = appMapper;
        this.draftMapper = draftMapper;
        this.objectMapper = objectMapper;
        this.modelMapper = modelMapper;
        this.knowledgeProfileMapper = knowledgeProfileMapper;
        this.toolConnectorMapper = toolConnectorMapper;
    }

    /**
     * 安装前预检（Preflight）：检查模板所需 AI 模型、知识库与工具连接器在当前租户下是否存在，并提供可用的替换选项。
     *
     * @param user 当前登录用户
     * @param marketplaceItemId 集市模板 ID
     * @param replacements 用户提交的依赖替换映射
     * @return 预检结果契约
     */
    public InstallationPreflight preflight(SecurityUser user, Long marketplaceItemId,
                                           List<DependencyReplacement> replacements) {
        requireIdentity(user);
        MarketplaceItemEntity item = requireReadyItem(marketplaceItemId);
        try {
            JsonNode manifest = objectMapper.readTree(item.getManifestJson());
            if (!manifest.path("graphJson").isObject()) {
                throw error("TEMPLATE_GRAPH_INVALID", "模板缺少有效的工作流图定义。");
            }
            List<MissingDependency> missing = inspectDependencies(user.getTenantId(), manifest, replacements);
            return new InstallationPreflight(
                    item.getId(),
                    manifest.path("schemaVersion").asText(),
                    missing.isEmpty(),
                    missing
            );
        } catch (ApplicationLifecycleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw error("TEMPLATE_MANIFEST_INVALID", "模板清单格式无效，无法解析。");
        }
    }

    /**
     * 升级前预检（Upgrade Preflight）：对比模板原始基线 Base、租户已定制 Draft 与新版本 Template，识别冲突（Conflicts）。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param marketplaceItemId 新版本集市模板 ID
     * @return 升级预检契约（包含冲突列表）
     */
    public UpgradePreflight preflightUpgrade(SecurityUser user, Long applicationId, Long marketplaceItemId) {
        requireIdentity(user);
        OrchestrationAppEntity app = requireApplication(user.getTenantId(), applicationId);
        MarketplaceInstallationEntity installation = requireInstallation(user.getTenantId(), applicationId);
        MarketplaceItemEntity item = requireReadyItem(marketplaceItemId);
        try {
            JsonNode base = parseDraft(installation.getDraftRevisionId());
            JsonNode current = parseDraft(currentDraft(user.getTenantId(), applicationId).getId());
            JsonNode template = objectMapper.readTree(item.getManifestJson()).path("graphJson");
            List<UpgradeConflict> conflicts = new ArrayList<>();
            mergeObject(base, current, template, Set.of(), conflicts, "$");
            return new UpgradePreflight(
                    app.getId(),
                    installation.getTemplateVersion(),
                    item.getId(),
                    itemVersion(item),
                    conflicts
            );
        } catch (ApplicationLifecycleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw error("MARKETPLACE_UPGRADE_INVALID", "模板升级预检失败。");
        }
    }

    /**
     * 执行平滑升级（Upgrade）：按用户裁决的冲突处理方案（Choice）将新模板融合入新草稿版本。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param request 升级请求（包含冲突解决决议 List）
     * @return 升级执行结果契约
     */
    @Transactional
    public UpgradeResult upgrade(SecurityUser user, Long applicationId, UpgradeRequest request) {
        requireIdentity(user);
        if (request == null || request.marketplaceItemId() == null) {
            throw error("MARKETPLACE_UPGRADE_ITEM_REQUIRED", "升级目标模板不能为空。");
        }
        OrchestrationAppEntity app = requireApplication(user.getTenantId(), applicationId);
        MarketplaceInstallationEntity installation = requireInstallation(user.getTenantId(), applicationId);
        MarketplaceItemEntity item = requireReadyItem(request.marketplaceItemId());
        try {
            JsonNode base = parseDraft(installation.getDraftRevisionId());
            OrchestrationDraftRevisionEntity currentDraft = currentDraft(user.getTenantId(), applicationId);
            JsonNode current = objectMapper.readTree(currentDraft.getGraphJson());
            JsonNode template = objectMapper.readTree(item.getManifestJson()).path("graphJson");
            List<UpgradeConflict> conflicts = new ArrayList<>();
            Set<String> resolutions = new HashSet<>();
            if (request.resolutions() != null) {
                request.resolutions().forEach(itemResolution ->
                        resolutions.add(itemResolution.path() + ":" + itemResolution.choice().name()));
            }
            ObjectNode merged = mergeObject(base, current, template, resolutions, conflicts, "$");
            if (!conflicts.isEmpty()) {
                throw error("MARKETPLACE_UPGRADE_CONFLICT", "模板升级存在尚未手动裁决的变更冲突。");
            }
            LocalDateTime now = LocalDateTime.now();
            OrchestrationDraftRevisionEntity draft = new OrchestrationDraftRevisionEntity();
            draft.setTenantId(user.getTenantId());
            draft.setAppId(applicationId);
            draft.setRevisionNo(currentDraft.getRevisionNo() + 1);
            draft.setGraphJson(objectMapper.writeValueAsString(merged));
            draft.setBaseVersionId(currentDraft.getBaseVersionId());
            draft.setChangeSummary("Marketplace 模板平滑升级自动生成");
            draft.setCreatedBy(user.getUserId());
            draft.setCreatedAt(now);
            draftMapper.insert(draft);

            app.setCurrentRevisionId(draft.getId());
            app.setStatus("DRAFT");
            app.setUpdatedAt(now);
            appMapper.updateById(app);

            installation.setDraftRevisionId(draft.getId());
            installation.setTemplateVersion(itemVersion(item));
            installation.setManifestFingerprint(sha256(item.getManifestJson()));
            installation.setInstallationStatus(DRAFT_CREATED);
            installationMapper.updateById(installation);

            return new UpgradeResult(applicationId, draft.getId(), item.getId(), draft.getRevisionNo());
        } catch (ApplicationLifecycleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw error("MARKETPLACE_UPGRADE_FAILED", "模板升级执行失败，事务已回滚。");
        }
    }

    /**
     * 一键安装集市模板（Install）：在租户空间下创建新 Application 及初始 Draft，绑定安装记录。
     *
     * @param user 当前登录用户
     * @param request 安装请求参数（包含依赖替换列表）
     * @return 安装结果契约
     */
    @Transactional
    public InstallResult install(SecurityUser user, MarketplaceInstallRequest request) {
        requireIdentity(user);
        if (request == null || request.marketplaceItemId() == null) {
            throw error("MARKETPLACE_ITEM_REQUIRED", "能力集市模板 ID 不能为空。");
        }
        MarketplaceItemEntity item = requireReadyItem(request.marketplaceItemId());
        try {
            JsonNode manifest = objectMapper.readTree(item.getManifestJson());
            JsonNode graph = manifest.path("graphJson");
            if (!graph.isObject()) {
                throw error("TEMPLATE_GRAPH_INVALID", "模板缺少有效的工作流图定义。");
            }
            List<MissingDependency> missing = inspectDependencies(user.getTenantId(), manifest, request.replacements());
            if (!missing.isEmpty()) {
                throw error("MARKETPLACE_DEPENDENCY_MISSING", "模板依赖未完全满足，请先进行依赖替换配置。");
            }
            String manifestFingerprint = sha256(item.getManifestJson());
            MarketplaceInstallationEntity existing = installationMapper.selectList(new LambdaQueryWrapper<MarketplaceInstallationEntity>()
                    .eq(MarketplaceInstallationEntity::getTenantId, user.getTenantId())
                    .eq(MarketplaceInstallationEntity::getMarketplaceItemId, item.getId())
                    .eq(MarketplaceInstallationEntity::getManifestFingerprint, manifestFingerprint)
                    .orderByDesc(MarketplaceInstallationEntity::getId)).stream().findFirst().orElse(null);
            if (existing != null) {
                return new InstallResult(existing.getApplicationId(), existing.getDraftRevisionId(), existing.getId(),
                        existing.getInstallationStatus(), List.of());
            }

            String suffix = UUID.randomUUID().toString().replace("-", "");
            LocalDateTime now = LocalDateTime.now();
            OrchestrationAppEntity app = new OrchestrationAppEntity();
            app.setTenantId(user.getTenantId());
            app.setAppCode("app-" + suffix);
            app.setAppName(item.getItemName());
            app.setGraphType(manifest.path("graphType").asText("APPLICATION_WORKFLOW"));
            app.setStatus("DRAFT");
            app.setCreatedBy(user.getUserId());
            app.setCreatedAt(now);
            app.setUpdatedAt(now);
            appMapper.insert(app);

            OrchestrationDraftRevisionEntity draft = new OrchestrationDraftRevisionEntity();
            draft.setTenantId(user.getTenantId());
            draft.setAppId(app.getId());
            draft.setRevisionNo(1);
            draft.setGraphJson(objectMapper.writeValueAsString(graph));
            draft.setChangeSummary("Marketplace 模板安装生成");
            draft.setCreatedBy(user.getUserId());
            draft.setCreatedAt(now);
            draftMapper.insert(draft);

            app.setCurrentRevisionId(draft.getId());
            appMapper.updateById(app);

            MarketplaceInstallationEntity installation = new MarketplaceInstallationEntity();
            installation.setTenantId(user.getTenantId());
            installation.setApplicationId(app.getId());
            installation.setDraftRevisionId(draft.getId());
            installation.setMarketplaceItemId(item.getId());
            installation.setTemplateVersion(manifest.path("schemaVersion").asText("v1"));
            installation.setSourceType(SOURCE_MARKETPLACE);
            installation.setManifestFingerprint(manifestFingerprint);
            installation.setInstallationStatus(DRAFT_CREATED);
            installation.setInstalledBy(user.getUsername());
            installation.setInstalledAt(now);
            installationMapper.insert(installation);

            return new InstallResult(app.getId(), draft.getId(), installation.getId(), DRAFT_CREATED, List.of());
        } catch (ApplicationLifecycleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw error("MARKETPLACE_INSTALL_FAILED", "模板安装失败，事务已回滚。");
        }
    }

    /**
     * SHA-256 哈希辅助方法。
     */
    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }

    /**
     * 校验身份。
     */
    private void requireIdentity(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw error("IDENTITY_INVALID", "当前身份无效。");
        }
    }

    /**
     * 校验应用归属。
     */
    private OrchestrationAppEntity requireApplication(Long tenantId, Long applicationId) {
        OrchestrationAppEntity app = appMapper.selectOne(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, tenantId).eq(OrchestrationAppEntity::getId, applicationId));
        if (app == null) {
            throw error("APPLICATION_NOT_FOUND", "应用不存在或不属于当前租户。");
        }
        return app;
    }

    /**
     * 校验安装记录归属。
     */
    private MarketplaceInstallationEntity requireInstallation(Long tenantId, Long applicationId) {
        MarketplaceInstallationEntity installation = installationMapper.selectOne(new LambdaQueryWrapper<MarketplaceInstallationEntity>()
                .eq(MarketplaceInstallationEntity::getTenantId, tenantId)
                .eq(MarketplaceInstallationEntity::getApplicationId, applicationId));
        if (installation == null) {
            throw error("MARKETPLACE_INSTALLATION_NOT_FOUND", "该应用没有可升级的集市模板安装来源。");
        }
        return installation;
    }

    /**
     * 获取最新草稿版本。
     */
    private OrchestrationDraftRevisionEntity currentDraft(Long tenantId, Long applicationId) {
        OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                .eq(OrchestrationDraftRevisionEntity::getTenantId, tenantId)
                .eq(OrchestrationDraftRevisionEntity::getAppId, applicationId)
                .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo).last("LIMIT 1"));
        if (draft == null) {
            throw error("DRAFT_NOT_FOUND", "应用没有可升级的草稿版本。");
        }
        return draft;
    }

    /**
     * 解析草稿 JSON。
     */
    private JsonNode parseDraft(Long draftId) throws Exception {
        OrchestrationDraftRevisionEntity draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw error("DRAFT_NOT_FOUND", "模板来源草稿不存在。");
        }
        return objectMapper.readTree(draft.getGraphJson());
    }

    /**
     * 获取集市模板 Schema 版本。
     */
    private String itemVersion(MarketplaceItemEntity item) throws Exception {
        return objectMapper.readTree(item.getManifestJson()).path("schemaVersion").asText("v1");
    }

    /**
     * 执行三方（3-way）合并判断，保留没有冲突的变更并记录冲突。
     */
    private ObjectNode mergeObject(JsonNode base, JsonNode tenant, JsonNode template, Set<String> resolutions,
                                   List<UpgradeConflict> conflicts, String path) {
        ObjectNode result = objectMapper.createObjectNode();
        Set<String> fields = new HashSet<>();
        base.fieldNames().forEachRemaining(fields::add);
        tenant.fieldNames().forEachRemaining(fields::add);
        template.fieldNames().forEachRemaining(fields::add);

        for (String field : fields) {
            String fieldPath = path + "." + field;
            JsonNode baseValue = base.path(field);
            JsonNode tenantValue = tenant.path(field);
            JsonNode templateValue = template.path(field);

            if (tenantValue.equals(baseValue)) {
                result.set(field, templateValue);
            } else if (templateValue.equals(baseValue) || tenantValue.equals(templateValue)) {
                result.set(field, tenantValue);
            } else if (resolutions.contains(fieldPath + ":TENANT")) {
                result.set(field, tenantValue);
            } else if (resolutions.contains(fieldPath + ":TEMPLATE")) {
                result.set(field, templateValue);
            } else {
                result.set(field, tenantValue);
                conflicts.add(new UpgradeConflict(fieldPath, tenantValue, templateValue));
            }
        }
        return result;
    }

    /**
     * 校验集市项目就绪状态。
     */
    private MarketplaceItemEntity requireReadyItem(Long marketplaceItemId) {
        MarketplaceItemEntity item = itemMapper.selectOne(new LambdaQueryWrapper<MarketplaceItemEntity>()
                .eq(MarketplaceItemEntity::getId, marketplaceItemId)
                .eq(MarketplaceItemEntity::getItemStatus, READY));
        if (item == null) {
            throw error("MARKETPLACE_ITEM_NOT_FOUND", "能力集市模板不存在或尚未发布就绪。");
        }
        return item;
    }

    /**
     * 检查模板声明的依赖项在当前租户下是否存在。
     */
    private List<MissingDependency> inspectDependencies(Long tenantId, JsonNode manifest,
                                                        List<DependencyReplacement> replacements) {
        List<DependencyReplacement> bindings = replacements == null ? List.of() : List.copyOf(replacements);
        List<MissingDependency> missing = new ArrayList<>();
        JsonNode dependencies = manifest.path("dependencies");
        if (!dependencies.isArray()) {
            return missing;
        }

        dependencies.forEach(node -> {
            DependencyType type = parseDependencyType(node.path("type").asText());
            String requestedKey = node.path("key").asText("");
            if (requestedKey.isBlank()) {
                missing.add(new MissingDependency(type, requestedKey, List.of()));
                return;
            }
            String resolvedKey = bindings.stream()
                    .filter(item -> item.type() == type && requestedKey.equals(item.requestedKey()))
                    .map(DependencyReplacement::replacementKey).findFirst().orElse(requestedKey);
            if (resolvedKey == null || resolvedKey.isBlank() || !dependencyExists(tenantId, type, resolvedKey)) {
                missing.add(new MissingDependency(type, requestedKey, availableKeys(tenantId, type)));
            }
        });
        return List.copyOf(missing);
    }

    /**
     * 判断特定依赖在当前租户下是否存在。
     */
    private boolean dependencyExists(Long tenantId, DependencyType type, String key) {
        return switch (type) {
            case MODEL -> modelMapper.selectCount(new LambdaQueryWrapper<SysModelConfigEntity>()
                    .eq(SysModelConfigEntity::getModelKey, key).eq(SysModelConfigEntity::getStatus, ACTIVE)
                    .and(query -> query.eq(SysModelConfigEntity::getTenantId, tenantId)
                            .or().isNull(SysModelConfigEntity::getTenantId))) > 0;
            case KNOWLEDGE -> knowledgeProfileMapper.selectCount(new LambdaQueryWrapper<RagEmbeddingProfileEntity>()
                    .eq(RagEmbeddingProfileEntity::getTenantId, tenantId)
                    .eq(RagEmbeddingProfileEntity::getProfileCode, key)
                    .eq(RagEmbeddingProfileEntity::getStatus, ACTIVE)) > 0;
            case TOOL -> toolConnectorMapper.selectCount(new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                    .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                    .eq(PlatformToolConnectorEntity::getConnectorCode, key)
                    .eq(PlatformToolConnectorEntity::getStatus, ACTIVE)) > 0;
        };
    }

    /**
     * 查询租户下可用的备选依赖 Key 列表。
     */
    private List<String> availableKeys(Long tenantId, DependencyType type) {
        return switch (type) {
            case MODEL -> modelMapper.selectList(new LambdaQueryWrapper<SysModelConfigEntity>()
                            .eq(SysModelConfigEntity::getStatus, ACTIVE)
                            .and(query -> query.eq(SysModelConfigEntity::getTenantId, tenantId)
                                    .or().isNull(SysModelConfigEntity::getTenantId)))
                    .stream().map(SysModelConfigEntity::getModelKey).distinct().limit(10).toList();
            case KNOWLEDGE -> knowledgeProfileMapper.selectList(new LambdaQueryWrapper<RagEmbeddingProfileEntity>()
                            .eq(RagEmbeddingProfileEntity::getTenantId, tenantId)
                            .eq(RagEmbeddingProfileEntity::getStatus, ACTIVE))
                    .stream().map(RagEmbeddingProfileEntity::getProfileCode).distinct().limit(10).toList();
            case TOOL -> toolConnectorMapper.selectList(new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                            .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                            .eq(PlatformToolConnectorEntity::getStatus, ACTIVE))
                    .stream().map(PlatformToolConnectorEntity::getConnectorCode).distinct().limit(10).toList();
        };
    }

    /**
     * 解析依赖类型枚举。
     */
    private DependencyType parseDependencyType(String value) {
        try {
            return DependencyType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw error("TEMPLATE_DEPENDENCY_TYPE_INVALID", "模板包含不支持的依赖类型。");
        }
    }

    /**
     * 构造 ApplicationLifecycleException。
     */
    private ApplicationLifecycleException error(String code, String message) {
        return new ApplicationLifecycleException(code, message);
    }

    /** 依赖类型枚举 */
    public enum DependencyType { MODEL, KNOWLEDGE, TOOL }

    /** 依赖替换关系 Record */
    public record DependencyReplacement(DependencyType type, String requestedKey, String replacementKey) { }

    /** 缺失的依赖分析 Record */
    public record MissingDependency(DependencyType type, String requestedKey, List<String> replacementKeys) { }

    /** 安装预检结果 Record */
    public record InstallationPreflight(Long marketplaceItemId, String templateVersion, boolean installable,
                                        List<MissingDependency> missingDependencies) { }

    /** 安装集市模板请求 Record */
    public record MarketplaceInstallRequest(Long marketplaceItemId, List<DependencyReplacement> replacements) { }

    /** 模板安装结果 Record */
    public record InstallResult(Long applicationId, Long draftRevisionId, Long installationId,
                                String lifecycleStatus, List<MissingDependency> missingDependencies) { }

    /** 升级应用请求 Record */
    public record UpgradeRequest(Long marketplaceItemId, List<UpgradeResolution> resolutions) { }

    /** 升级冲突裁决选项枚举 */
    public enum UpgradeChoice { TENANT, TEMPLATE }

    /** 冲突裁决决议 Record */
    public record UpgradeResolution(String path, UpgradeChoice choice) {
        public String path() {
            return path;
        }
    }

    /** 升级冲突项 Record */
    public record UpgradeConflict(String path, JsonNode tenantValue, JsonNode templateValue) { }

    /** 升级预检结果 Record */
    public record UpgradePreflight(Long applicationId, String currentTemplateVersion, Long nextMarketplaceItemId,
                                   String nextTemplateVersion, List<UpgradeConflict> conflicts) { }

    /** 升级结果 Record */
    public record UpgradeResult(Long applicationId, Long draftRevisionId, Long marketplaceItemId, Integer revisionNo) { }
}

