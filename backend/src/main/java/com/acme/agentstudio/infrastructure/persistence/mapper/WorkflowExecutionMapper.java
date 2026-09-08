package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.WorkflowExecutionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * WorkflowExecution 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
/**
 * 工作流执行实例 Mapper 接口
 */
@Mapper
/**
 * WorkflowExecution 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 WorkflowExecution 数据库读写方法。
 */
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecutionEntity> {

    /**
     * 更新运行中工作流执行实例的心跳时间
     *
     * @param id 工作流执行实例 ID
     * @return 影响行数（1 表示心跳更新成功）
     */
    @Update("UPDATE workflow_execution SET heartbeat_at = CURRENT_TIMESTAMP WHERE id = #{id} AND status = 'RUNNING'")
    int heartbeat(Long id);
}
