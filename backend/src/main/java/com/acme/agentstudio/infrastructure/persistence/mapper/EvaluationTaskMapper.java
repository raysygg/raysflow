package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.EvaluationTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * EvaluationTask 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
/**
 * 智能体效果评测任务 Mapper 接口
 */
@Mapper
/**
 * EvaluationTask 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 EvaluationTask 数据库读写方法。
 */
public interface EvaluationTaskMapper extends BaseMapper<EvaluationTaskEntity> {

    /**
     * 将处于草稿或排队状态的评测任务原子更新为运行中状态（防止重复并发执行）
     *
     * @param taskId 评测任务 ID
     * @return 影响行数（1 表示抢占/切换状态成功）
     */
    @Update("UPDATE evaluation_task SET evaluation_status='RUNNING', updated_at=CURRENT_TIMESTAMP WHERE id=#{taskId} AND evaluation_status IN ('DRAFT','QUEUED')")
    int markRunning(@Param("taskId") Long taskId);
}
