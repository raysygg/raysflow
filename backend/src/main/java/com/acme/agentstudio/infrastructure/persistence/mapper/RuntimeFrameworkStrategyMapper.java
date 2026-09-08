package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeFrameworkStrategyEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * RuntimeFrameworkStrategy 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
/** 原生框架策略注册表数据访问接口。 */
@Mapper
public interface RuntimeFrameworkStrategyMapper extends BaseMapper<RuntimeFrameworkStrategyEntity> {
}
