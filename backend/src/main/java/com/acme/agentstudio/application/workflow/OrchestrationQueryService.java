package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.domain.workflow.model.GraphNode;
import com.acme.agentstudio.domain.workflow.model.OrchestrationAppSummary;
import com.acme.agentstudio.domain.workflow.model.OrchestrationVersionSummary;
import com.acme.agentstudio.domain.workflow.model.VersionComparison;
import com.acme.agentstudio.domain.workflow.model.WorkflowReleaseBundle;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationDraftRevisionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationEnvironmentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationDraftRevisionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationEnvironmentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationVersionMapper;
import com.acme.agentstudio.infrastructure.workflow.GraphDefinitionParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 编排应用读模型查询服务（Orchestration Query Service）。
 * 负责应用列表摘要组装（融合草稿最新修订号与线上生产环境绑定版本）、历史发布版本列表查询以及两个指定版本间的图节点/连线/依赖 Diff 比对。
 */
@Service
public class OrchestrationQueryService {

    /** 默认环境 Code */
    private static final String DEFAULT_ENVIRONMENT = "PRODUCTION";

    /** 编排应用 Mapper */
    private final OrchestrationAppMapper appMapper;

    /** 草稿修订 Mapper */
    private final OrchestrationDraftRevisionMapper draftMapper;

    /** 环境绑定 Mapper */
    private final OrchestrationEnvironmentMapper environmentMapper;

    /** 正式版本 Mapper */
    private final OrchestrationVersionMapper versionMapper;

    /** 图解析器 */
    private final GraphDefinitionParser graphParser;

    /** Jackson JSON 映射器 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入查询所需依赖组件。
     */
    public OrchestrationQueryService(
            OrchestrationAppMapper appMapper,
            OrchestrationDraftRevisionMapper draftMapper,
            OrchestrationEnvironmentMapper environmentMapper,
            OrchestrationVersionMapper versionMapper,
            GraphDefinitionParser graphParser,
            ObjectMapper objectMapper
    ) {
        this.appMapper = appMapper;
        this.draftMapper = draftMapper;
        this.environmentMapper = environmentMapper;
        this.versionMapper = versionMapper;
        this.graphParser = graphParser;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询当前租户下全部编排应用列表，并附带最新草稿与线上生产环境版本摘要。
     *
     * @param user 当前操作用户
     * @return 编排应用摘要列表
     */
    public List<OrchestrationAppSummary> listApps(SecurityUser user) {
        requireUser(user);
        return appMapper.selectList(new LambdaQueryWrapper<OrchestrationAppEntity>()
                        .eq(OrchestrationAppEntity::getTenantId, user.getTenantId())
                        .orderByDesc(OrchestrationAppEntity::getUpdatedAt))
                .stream()
                .map(app -> toSummary(user.getTenantId(), app))
                .toList();
    }

    /**
     * 查询指定编排应用的历史已发布版本列表（按版本号降序）。
     *
     * @param user 当前操作用户
     * @param appId 应用 ID
     * @return 历史版本摘要列表
     */
    public List<OrchestrationVersionSummary> versions(SecurityUser user, Long appId) {
        requireUser(user);
        String currentVersionId = currentVersionId(user.getTenantId(), appId);
        return versionMapper.selectList(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationVersionEntity::getAppId, appId)
                .orderByDesc(OrchestrationVersionEntity::getVersionNo)).stream()
                .map(version -> toVersionSummary(version, currentVersionId))
                .toList();
    }

    /**
     * 比对两个发布版本之间的流程图 JSON 结构、节点修改明细及依赖差异。
     *
     * @param user 当前操作用户
     * @param appId 应用 ID
     * @param left 左侧对比版本 ID
     * @param right 右侧对比版本 ID
     * @return 类型化版本差异 VersionComparison 对象
     */
    public VersionComparison compare(SecurityUser user, Long appId, String left, String right) {
        requireUser(user);
        OrchestrationVersionEntity a = findVersion(user, appId, left);
        OrchestrationVersionEntity b = findVersion(user, appId, right);
        if (a == null || b == null) {
            throw new IllegalArgumentException("参与比对的某一方发布版本不存在或已被删除。");
        }

        GraphDefinition leftGraph = graphParser.read(a.getGraphJson());
        GraphDefinition rightGraph = graphParser.read(b.getGraphJson());
        Map<String, GraphNode> leftNodes = nodesById(leftGraph);
        Map<String, GraphNode> rightNodes = nodesById(rightGraph);

        List<String> added = rightNodes.keySet().stream().filter(id -> !leftNodes.containsKey(id)).sorted().toList();
        List<String> removed = leftNodes.keySet().stream().filter(id -> !rightNodes.containsKey(id)).sorted().toList();
        List<String> modified = leftNodes.keySet().stream()
                .filter(rightNodes::containsKey)
                .filter(id -> !leftNodes.get(id).equals(rightNodes.get(id)))
                .sorted()
                .toList();

        String currentVersionId = currentVersionId(user.getTenantId(), appId);
        return new VersionComparison(
                toVersionSummary(a, currentVersionId),
                toVersionSummary(b, currentVersionId),
                added,
                removed,
                modified,
                !leftGraph.edges().equals(rightGraph.edges()),
                !leftGraph.inputSchema().equals(rightGraph.inputSchema()),
                !leftGraph.outputSchema().equals(rightGraph.outputSchema()),
                !dependencies(a).equals(dependencies(b))
        );
    }

    /** 节点键值 Map 映射 */
    private Map<String, GraphNode> nodesById(GraphDefinition graph) {
        return graph.nodes().stream().collect(Collectors.toMap(GraphNode::nodeId, Function.identity()));
    }

    /** 提取版本对应的冻结依赖配置 */
    private Object dependencies(OrchestrationVersionEntity version) {
        if (version.getReleaseBundleJson() == null || version.getReleaseBundleJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(version.getReleaseBundleJson(), WorkflowReleaseBundle.class).dependencies();
        } catch (Exception exception) {
            throw new IllegalArgumentException("指定发布版本的快照 Bundle 无法解析，请重新发布该版本。", exception);
        }
    }

    /** 查询生产环境指针 VersionId */
    private String currentVersionId(Long tenantId, Long appId) {
        OrchestrationEnvironmentEntity environment = environmentMapper.selectOne(
                new LambdaQueryWrapper<OrchestrationEnvironmentEntity>()
                        .eq(OrchestrationEnvironmentEntity::getTenantId, tenantId)
                        .eq(OrchestrationEnvironmentEntity::getAppId, appId)
                        .eq(OrchestrationEnvironmentEntity::getEnvironmentCode, DEFAULT_ENVIRONMENT)
        );
        return environment == null ? null : environment.getCurrentVersionId();
    }

    /** 转换为 VersionSummary 对象 */
    private OrchestrationVersionSummary toVersionSummary(OrchestrationVersionEntity version, String currentVersionId) {
        return new OrchestrationVersionSummary(
                version.getAppId(),
                version.getVersionId(),
                version.getVersionNo(),
                DEFAULT_ENVIRONMENT,
                version.getStatus(),
                version.getReleasedBy(),
                version.getReleasedAt(),
                version.getGraphJson(),
                version.getReleaseBundleHash(),
                version.getVersionId().equals(currentVersionId)
        );
    }

    /** 按 VersionId 查询正式版本 */
    private OrchestrationVersionEntity findVersion(SecurityUser user, Long appId, String versionId) {
        return versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationVersionEntity::getAppId, appId)
                .eq(OrchestrationVersionEntity::getVersionId, versionId));
    }

    /** 校验登录身份 */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null) {
            throw new IllegalArgumentException("当前操作用户的安全身份上下文无效。");
        }
    }

    /** 转换为 AppSummary 读模型 */
    private OrchestrationAppSummary toSummary(Long tenantId, OrchestrationAppEntity app) {
        OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                .eq(OrchestrationDraftRevisionEntity::getTenantId, tenantId)
                .eq(OrchestrationDraftRevisionEntity::getAppId, app.getId())
                .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo)
                .last("LIMIT 1"));

        OrchestrationEnvironmentEntity environment = environmentMapper.selectOne(new LambdaQueryWrapper<OrchestrationEnvironmentEntity>()
                .eq(OrchestrationEnvironmentEntity::getTenantId, tenantId)
                .eq(OrchestrationEnvironmentEntity::getAppId, app.getId())
                .eq(OrchestrationEnvironmentEntity::getEnvironmentCode, DEFAULT_ENVIRONMENT));

        OrchestrationVersionEntity version = environment == null || environment.getCurrentVersionId() == null
                ? null
                : versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                        .eq(OrchestrationVersionEntity::getTenantId, tenantId)
                        .eq(OrchestrationVersionEntity::getAppId, app.getId())
                        .eq(OrchestrationVersionEntity::getVersionId, environment.getCurrentVersionId())
                        .eq(OrchestrationVersionEntity::getStatus, BusinessStatus.PUBLISHED));

        return new OrchestrationAppSummary(
                app.getId(),
                app.getAppCode(),
                app.getAppName(),
                app.getGraphType(),
                draft == null ? 0 : draft.getRevisionNo(),
                app.getStatus(),
                DEFAULT_ENVIRONMENT,
                environment == null ? null : environment.getCurrentVersionId(),
                version == null ? null : version.getVersionNo(),
                version == null ? null : version.getReleasedAt()
        );
    }
}

