package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.AlertStatus;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.SliSnapshot;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.SliType;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.SloAlert;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeSloAlertEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeSloAlertMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 运行时 SLO 告警状态流转与去重服务（Runtime SLO Service）。
 * 基于 RuntimeSloAlertEntity 持久化记录实现跨 Worker 实例的告警去重。
 * 支持在指标越界时创建或维持 OPEN 告警状态，在指标恢复正常后自动转换为 RECOVERED 状态，并支持人工确认（acknowledge）标记为 ACKNOWLEDGED。
 */
@Service
public class RuntimeSloService {

    /** 违例告警摘要说明 */
    private static final String SUMMARY_BREACH = "运行质量指标已越界，超过配置的 SLO 允许范围";

    /** 恢复正常摘要说明 */
    private static final String SUMMARY_RECOVERED = "运行质量指标已恢复正常，处于 SLO 期望范围内";

    /** SLO 告警 Mapper */
    private final RuntimeSloAlertMapper alertMapper;

    /**
     * 构造函数注入依赖 Mapper。
     */
    public RuntimeSloService(RuntimeSloAlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    /**
     * 评估单项 SLI 遥测指标，更新或自动恢复 SLO 告警状态。
     *
     * @param tenantId 租户 ID
     * @param applicationId 应用 ID
     * @param releaseId 发布版本 ID
     * @param alertKey 告警唯一主键
     * @param snapshot 当前遥测指标快照
     * @param target SLO 期望目标值
     * @param belowTarget 是否违例（true 表示触发告警）
     * @return 最新的告警契约对象 SloAlert（无告警时返回 null）
     */
    @Transactional
    public SloAlert evaluate(
            Long tenantId,
            Long applicationId,
            String releaseId,
            String alertKey,
            SliSnapshot snapshot,
            double target,
            boolean belowTarget
    ) {
        if (tenantId == null || alertKey == null || alertKey.isBlank() || snapshot == null) {
            throw new IllegalArgumentException("评估 SLO 告警时，租户 ID、alertKey 与 SliSnapshot 均不能为空。");
        }

        RuntimeSloAlertEntity current = alertMapper.selectOne(new LambdaQueryWrapper<RuntimeSloAlertEntity>()
                .eq(RuntimeSloAlertEntity::getTenantId, tenantId)
                .eq(RuntimeSloAlertEntity::getAlertKey, alertKey));

        LocalDateTime now = LocalDateTime.now();

        if (belowTarget) {
            if (current != null && (AlertStatus.OPEN.name().equals(current.getAlertStatus())
                    || AlertStatus.ACKNOWLEDGED.name().equals(current.getAlertStatus()))) {
                return toContract(current);
            }

            RuntimeSloAlertEntity next = (current == null) ? new RuntimeSloAlertEntity() : current;
            next.setTenantId(tenantId);
            next.setApplicationId(applicationId);
            next.setReleaseId(releaseId);
            next.setAlertKey(alertKey);
            next.setSliType(snapshot.type().name());
            next.setAlertStatus(AlertStatus.OPEN.name());
            next.setObservedValue(snapshot.value());
            next.setTargetValue(target);
            next.setSafeSummary(SUMMARY_BREACH);

            if (next.getOpenedAt() == null) {
                next.setOpenedAt(now);
            }
            next.setRecoveredAt(null);
            next.setUpdatedAt(now);

            if (next.getId() == null) {
                alertMapper.insert(next);
            } else {
                alertMapper.updateById(next);
            }

            return toContract(next);
        }

        if (current == null || AlertStatus.RECOVERED.name().equals(current.getAlertStatus())) {
            return null;
        }

        alertMapper.update(null, new LambdaUpdateWrapper<RuntimeSloAlertEntity>()
                .eq(RuntimeSloAlertEntity::getTenantId, tenantId)
                .eq(RuntimeSloAlertEntity::getAlertKey, alertKey)
                .set(RuntimeSloAlertEntity::getAlertStatus, AlertStatus.RECOVERED.name())
                .set(RuntimeSloAlertEntity::getObservedValue, snapshot.value())
                .set(RuntimeSloAlertEntity::getRecoveredAt, now)
                .set(RuntimeSloAlertEntity::getSafeSummary, SUMMARY_RECOVERED)
                .set(RuntimeSloAlertEntity::getUpdatedAt, now));

        current.setAlertStatus(AlertStatus.RECOVERED.name());
        current.setObservedValue(snapshot.value());
        current.setRecoveredAt(now);
        current.setSafeSummary(SUMMARY_RECOVERED);

        return toContract(current);
    }

    /**
     * 人工确认开方的 SLO 告警（标记为 ACKNOWLEDGED）。
     *
     * @param user 当前登录 SecurityUser
     * @param alertId 告警记录 ID
     */
    @Transactional
    public void acknowledge(SecurityUser user, Long alertId) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("确认 SLO 告警时，当前登录用户身份信息无效。");
        }

        alertMapper.update(null, new LambdaUpdateWrapper<RuntimeSloAlertEntity>()
                .eq(RuntimeSloAlertEntity::getTenantId, user.getTenantId())
                .eq(RuntimeSloAlertEntity::getId, alertId)
                .eq(RuntimeSloAlertEntity::getAlertStatus, AlertStatus.OPEN.name())
                .set(RuntimeSloAlertEntity::getAlertStatus, AlertStatus.ACKNOWLEDGED.name())
                .set(RuntimeSloAlertEntity::getAcknowledgedAt, LocalDateTime.now())
                .set(RuntimeSloAlertEntity::getAcknowledgedBy, user.getUserId()));
    }

    /** 转换为 SloAlert 强类型契约对象 */
    private SloAlert toContract(RuntimeSloAlertEntity e) {
        return new SloAlert(
                e.getAlertKey(),
                SliType.valueOf(e.getSliType()),
                AlertStatus.valueOf(e.getAlertStatus()),
                e.getObservedValue(),
                e.getTargetValue(),
                e.getSafeSummary(),
                (e.getOpenedAt() == null) ? null : e.getOpenedAt().atZone(ZoneId.systemDefault()).toInstant(),
                (e.getRecoveredAt() == null) ? null : e.getRecoveredAt().atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}

