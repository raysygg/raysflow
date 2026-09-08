package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.ApprovalTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

/**
 * ApprovalTask 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
/**
 * 审批工单任务 Mapper 接口
 */
@Mapper
/**
 * ApprovalTask 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 ApprovalTask 数据库读写方法。
 */
public interface ApprovalTaskMapper extends BaseMapper<ApprovalTaskEntity> {

    /**
     * 将处于 PENDING 待办状态的审批任务原子更新为最终决策状态（避免重复审批）
     *
     * @param id 审批工单 ID
     * @param tenantId 租户 ID
     * @param status 目标审批状态（如 APPROVED / REJECTED）
     * @return 影响行数（1 表示更新决策成功）
     */
    @Update("UPDATE approval_task SET approval_status=#{status}, updated_at=CURRENT_TIMESTAMP WHERE id=#{id} AND tenant_id=#{tenantId} AND approval_status='PENDING'")
    int markDecision(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("status") String status);
}
