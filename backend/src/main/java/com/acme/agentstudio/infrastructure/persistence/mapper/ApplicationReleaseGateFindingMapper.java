package com.acme.agentstudio.infrastructure.persistence.mapper;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationReleaseGateFindingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
/**
 * ApplicationReleaseGateFinding 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
@Mapper
/**
 * ApplicationReleaseGateFinding 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 ApplicationReleaseGateFinding 数据库读写方法。
 */
public interface ApplicationReleaseGateFindingMapper extends BaseMapper<ApplicationReleaseGateFindingEntity> { }
