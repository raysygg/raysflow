package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ApplicationReleaseCandidate 数据库持久化实体对象。
 * 对应数据库中 ApplicationReleaseCandidate 数据表的字段结构映射。
 */
@Data
@TableName("application_release_candidate")
/**
 * ApplicationReleaseCandidate 数据表持久化实体类。
 * 映射数据库对应的 ApplicationReleaseCandidate 表结构。
 */
public class ApplicationReleaseCandidateEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** draft Revision 主键 ID 标识 属性 */
    private Long draftRevisionId;
    /** draft Revision No 属性 */
    private Integer draftRevisionNo;
    /** snapshot Json 属性 */
    private String snapshotJson;
    /** snapshot Fingerprint 属性 */
    private String snapshotFingerprint;
    /** base Release 主键 ID 标识 属性 */
    private String baseReleaseId;
    /** change Summary 属性 */
    private String changeSummary;
    /** candidate 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String candidateStatus;
    /** 创建人唯一标识 */
    private Long createdBy;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
    /** lock 运行版本标识 属性 */
    private Integer lockVersion;
}
