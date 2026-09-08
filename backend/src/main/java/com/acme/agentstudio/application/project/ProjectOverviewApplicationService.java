package com.acme.agentstudio.application.project;

import com.acme.agentstudio.domain.project.model.ProjectOverview;
import com.acme.agentstudio.infrastructure.persistence.entity.AgentProfileEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AgentProfileMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 工作台与控制台项目资源分布概览（Project Overview）分析服务。
 * 针对 SUPER_ADMIN 视角汇总全平台租户、Agent、知识库文档与工作流数量；针对普通租户视角按 Tenant ID 严格做多租户数据隔离统计。
 */
@Service
public class ProjectOverviewApplicationService {

    /** 租户 Persistence Mapper */
    private final TenantMapper tenantMapper;

    /** 智能体 Profile Mapper */
    private final AgentProfileMapper agentProfileMapper;

    /** 知识库文档 Mapper */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 工作流应用 Mapper */
    private final OrchestrationAppMapper orchestrationAppMapper;

    /**
     * 构造函数注入项目概览所需组件依赖。
     */
    public ProjectOverviewApplicationService(
            TenantMapper tenantMapper,
            AgentProfileMapper agentProfileMapper,
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            OrchestrationAppMapper orchestrationAppMapper
    ) {
        this.tenantMapper = tenantMapper;
        this.agentProfileMapper = agentProfileMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.orchestrationAppMapper = orchestrationAppMapper;
    }

    /**
     * 获取指定租户及用户角色视角下的项目资源概览统计数据。
     *
     * @param tenantId 租户 ID
     * @param role 角色标识（如 SUPER_ADMIN / ADMIN / USER）
     * @return 包含各类 AI 资源项数量计数的 ProjectOverview 统计对象
     */
    public ProjectOverview getOverview(Long tenantId, String role) {
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
            return new ProjectOverview(
                    "Enterprise Agent Studio (Super Admin)",
                    Map.of(
                            "tenants", tenantMapper.selectCount(null),
                            "agents", agentProfileMapper.selectCount(null),
                            "knowledgeDocuments", knowledgeDocumentMapper.selectCount(null),
                            "workflows", orchestrationAppMapper.selectCount(null)
                    )
            );
        } else {
            return new ProjectOverview(
                    "Enterprise Agent Studio",
                    Map.of(
                            "tenants", 1L,
                            "agents", agentProfileMapper.selectCount(new LambdaQueryWrapper<AgentProfileEntity>()
                                    .eq(AgentProfileEntity::getTenantId, tenantId)),
                            "knowledgeDocuments", knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                                    .eq(KnowledgeDocumentEntity::getTenantId, tenantId)),
                            "workflows", orchestrationAppMapper.selectCount(new LambdaQueryWrapper<OrchestrationAppEntity>()
                                    .eq(OrchestrationAppEntity::getTenantId, tenantId))
                    )
            );
        }
    }
}

