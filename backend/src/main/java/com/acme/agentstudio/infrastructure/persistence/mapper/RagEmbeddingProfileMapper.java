package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.RagEmbeddingProfileEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * RagEmbeddingProfile 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
@Mapper
/**
 * RagEmbeddingProfile 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 RagEmbeddingProfile 数据库读写方法。
 */
public interface RagEmbeddingProfileMapper extends BaseMapper<RagEmbeddingProfileEntity> {
}

