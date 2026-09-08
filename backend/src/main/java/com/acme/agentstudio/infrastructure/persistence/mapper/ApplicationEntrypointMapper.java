package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * ApplicationEntrypoint 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
@Mapper
/**
 * ApplicationEntrypoint 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 ApplicationEntrypoint 数据库读写方法。
 */
public interface ApplicationEntrypointMapper extends BaseMapper<ApplicationEntrypointEntity> {
    @Update("UPDATE application_entrypoint SET schedule_lease_owner=#{owner}, schedule_lease_until=DATE_ADD(NOW(), INTERVAL #{seconds} SECOND) " +
            "WHERE id=#{id} AND enabled=1 AND next_fire_at<=NOW() AND (schedule_lease_until IS NULL OR schedule_lease_until<NOW())")
    int claimSchedule(@Param("id") Long id, @Param("owner") String owner, @Param("seconds") int seconds);
}
