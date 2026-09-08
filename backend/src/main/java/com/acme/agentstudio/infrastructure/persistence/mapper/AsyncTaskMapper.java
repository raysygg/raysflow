package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.AsyncTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

/**
 * AsyncTask 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
/**
 * 异步任务持久化与分布式锁抢占 Mapper 接口
 */
@Mapper
/**
 * AsyncTask 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 AsyncTask 数据库读写方法。
 */
public interface AsyncTaskMapper extends BaseMapper<AsyncTaskEntity> {

    /**
     * 使用状态条件完成原子认领，避免多节点重复消费
     *
     * @param id 任务 ID
     * @param workerId 工作节点标识
     * @return 影响行数（1 表示抢占成功）
     */
    @Update("UPDATE async_task SET status='RUNNING', worker_id=#{workerId}, heartbeat_at=CURRENT_TIMESTAMP WHERE id=#{id} AND status='QUEUED'")
    int claim(@Param("id") Long id, @Param("workerId") String workerId);

    /**
     * 刷新执行中异步任务的心跳维持时间
     *
     * @param id 任务 ID
     * @param workerId 工作节点标识
     * @return 影响行数
     */
    @Update("UPDATE async_task SET heartbeat_at=CURRENT_TIMESTAMP WHERE id=#{id} AND status='RUNNING' AND worker_id=#{workerId}")
    int heartbeat(@Param("id") Long id, @Param("workerId") String workerId);

    /**
     * 接管超时卡死的任务并计入重试次数
     *
     * @param timeoutSeconds 超时秒数
     * @param delaySeconds 重试延迟秒数
     * @param errorMessage 错误说明信息
     * @return 接管失效的任务数量
     */
    @Update("UPDATE async_task SET retry_count=retry_count+1, status=CASE WHEN retry_count+1 >= max_retries THEN 'FAILED' ELSE 'QUEUED' END, worker_id=NULL, heartbeat_at=NULL, available_at=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL #{delaySeconds} SECOND), error_message=#{errorMessage}, finished_at=CASE WHEN retry_count+1 >= max_retries THEN CURRENT_TIMESTAMP ELSE NULL END WHERE status='RUNNING' AND heartbeat_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL #{timeoutSeconds} SECOND)")
    int reclaimExpired(@Param("timeoutSeconds") int timeoutSeconds, @Param("delaySeconds") int delaySeconds, @Param("errorMessage") String errorMessage);

    /**
     * 标记异步任务执行成功完成
     *
     * @param id 任务 ID
     * @param workerId 工作节点标识
     * @return 影响行数
     */
    @Update("UPDATE async_task SET status='COMPLETED', finished_at=CURRENT_TIMESTAMP, heartbeat_at=CURRENT_TIMESTAMP WHERE id=#{id} AND status='RUNNING' AND worker_id=#{workerId}")
    int complete(@Param("id") Long id, @Param("workerId") String workerId);

    /**
     * 标记异步任务失败或退避重试
     *
     * @param id 任务 ID
     * @param workerId 工作节点标识
     * @param delaySeconds 重试延迟秒数
     * @param errorMessage 失败日志说明
     * @return 影响行数
     */
    @Update("UPDATE async_task SET retry_count=retry_count+1, status=CASE WHEN retry_count+1 >= max_retries THEN 'FAILED' ELSE 'QUEUED' END, worker_id=NULL, heartbeat_at=NULL, available_at=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL #{delaySeconds} SECOND), error_message=#{errorMessage}, finished_at=CASE WHEN retry_count+1 >= max_retries THEN CURRENT_TIMESTAMP ELSE NULL END WHERE id=#{id} AND status='RUNNING' AND worker_id=#{workerId}")
    int failOrRetry(@Param("id") Long id, @Param("workerId") String workerId, @Param("delaySeconds") int delaySeconds, @Param("errorMessage") String errorMessage);
}
