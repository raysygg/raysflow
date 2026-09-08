package com.acme.agentstudio.application.knowledge;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.QualityFindingStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeQualityFindingEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeQualityFindingMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库质量扫描缺陷发现项（Knowledge Quality Finding）处置服务。
 * 负责提供扫描出的低质量切块、孤立文档、格式异常或重复文本质量项的状态更替、责任人指派与审计备注写入功能。
 */
@Service
public class KnowledgeQualityFindingService {

    /** 知识质量发现项 Mapper */
    private final KnowledgeQualityFindingMapper findingMapper;

    /**
     * 构造函数注入质量问题 Mapper 依赖。
     */
    public KnowledgeQualityFindingService(KnowledgeQualityFindingMapper findingMapper) {
        this.findingMapper = findingMapper;
    }

    /**
     * 更新指定质量缺陷发现项的处置状态、责任人与审计意见。
     *
     * @param user 当前登录用户
     * @param findingId 缺陷项 ID
     * @param status 新处置状态枚举（如 RESOLVED / IGNORED）
     * @param assignee 被指派人用户 ID
     * @param auditNote 处置审计意见
     */
    @Transactional
    public void updateStatus(SecurityUser user, Long findingId, QualityFindingStatus status,
                             Long assignee, String auditNote) {
        requireTenant(user);
        int updated = findingMapper.update(null, new LambdaUpdateWrapper<KnowledgeQualityFindingEntity>()
                .eq(KnowledgeQualityFindingEntity::getTenantId, user.getTenantId())
                .eq(KnowledgeQualityFindingEntity::getId, findingId)
                .set(KnowledgeQualityFindingEntity::getFindingStatus, status.name())
                .set(KnowledgeQualityFindingEntity::getAssignee, assignee)
                .set(KnowledgeQualityFindingEntity::getAuditNote, auditNote));
        if (updated != 1) {
            throw new IllegalArgumentException("质量发现项不存在或不属于当前租户");
        }
    }

    /**
     * 校验租户身份。
     */
    private void requireTenant(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
    }
}

