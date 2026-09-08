package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * KnowledgeChunk 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
@Mapper
/**
 * KnowledgeChunk 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 KnowledgeChunk 数据库读写方法。
 */
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkEntity> {
    @Insert({"<script>",
            "INSERT INTO knowledge_chunk (id, document_id, index_generation_id, parent_chunk_id, chunk_role, chunk_no, chunk_text, vector_key, section_path, page_no, token_count, content_hash, embedding_status, late_vector_status, created_at) VALUES",
            "<foreach collection='chunks' item='chunk' separator=','>",
            "(#{chunk.id}, #{chunk.documentId}, #{chunk.indexGenerationId}, #{chunk.parentChunkId}, #{chunk.chunkRole}, #{chunk.chunkNo}, #{chunk.chunkText}, #{chunk.vectorKey}, #{chunk.sectionPath}, #{chunk.pageNo}, #{chunk.tokenCount}, #{chunk.contentHash}, #{chunk.embeddingStatus}, #{chunk.lateVectorStatus}, #{chunk.createdAt})",
            "</foreach>",
            "</script>"})
    int insertBatch(@Param("chunks") List<KnowledgeChunkEntity> chunks);

    @Update({"<script>",
            "UPDATE knowledge_chunk SET embedding_status = #{embeddingStatus}, late_vector_status = #{lateVectorStatus}",
            "WHERE id IN",
            "<foreach collection='chunkIds' item='chunkId' open='(' separator=',' close=')'>#{chunkId}</foreach>",
            "</script>"})
    int updateVectorStatus(@Param("chunkIds") List<Long> chunkIds,
                           @Param("embeddingStatus") String embeddingStatus,
                           @Param("lateVectorStatus") String lateVectorStatus);
}
