package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SysModelConfig 数据库持久化实体对象。
 * 对应数据库中 SysModelConfig 数据表的字段结构映射。
 */
@Data
@TableName("sys_model_config")
public class SysModelConfigEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 模型唯一标识 Key（平台内部路由与工作流寻址键） */
    private String modelKey;
    /** 上游供应商/中转真实模型名称，若为空则默认使用 modelKey */
    private String upstreamModelName;
    /** 模型展示名称 */
    private String modelName;
    /** 模型供应商服务商 */
    private String provider;
    /** 模型分类能力（CHAT, EMBEDDING, RERANKER） */
    private String modelCapability;
    /** vector 向量 Embeddings 维度大小属性 */
    private Integer vectorDimension;
    /** 模型调用凭证引用 ID，明文与密文均不进入模型配置表。 */
    private Long credentialRefId;
    /** 接口 Endpoint 访问地址 */
    private String baseUrl;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 是否已配置关联凭证引用（非数据库字段） */
    @TableField(exist = false)
    private Boolean credentialConfigured;

    /**
     * 获取发送给上游供应商/中转的实际模型标识。
     * 若配置了 upstreamModelName 则优先使用，否则平滑回退使用 modelKey。
     *
     * @return 实际发送给上游的真实模型名
     */
    public String resolveUpstreamModelName() {
        if (upstreamModelName != null && !upstreamModelName.isBlank()) {
            return upstreamModelName.trim();
        }
        return modelKey != null ? modelKey.trim() : "";
    }
}
