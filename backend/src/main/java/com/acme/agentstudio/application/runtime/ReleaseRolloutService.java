package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.ReleaseRolloutPolicy;
import com.acme.agentstudio.domain.runtime.model.ReleaseRolloutRecord;
import com.acme.agentstudio.domain.runtime.model.RolloutStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用发布金丝雀灰度放量与自动回滚服务（Release Rollout Service）。
 * 维护应用发布版本的灰度放量状态机（CANARY -> ROLLING_OUT -> ACTIVE / PAUSED / ROLLED_BACK）。
 * 结合实时运行成功率（successRate）与指标降级阀门（minimumSuccessRate），在成功率掉线时自动触发暂停或一键秒级回滚到上一个稳定 Release 生产指针。
 */
@Service
public class ReleaseRolloutService {

    /** 内存中应用灰度发布记录 Map */
    private final Map<String, ReleaseRolloutRecord> rollouts = new ConcurrentHashMap<>();

    /**
     * 启动金丝雀灰度发布（Canary Deployment），分配初始流量比例。
     *
     * @param applicationId 应用 ID
     * @param releaseId 拟发布的新版本 ID
     * @param previousReleaseId 历史稳定版本 ID
     * @param policy 灰度放量与回滚策略 ReleaseRolloutPolicy
     * @return 灰度记录实体 ReleaseRolloutRecord
     */
    public ReleaseRolloutRecord startCanary(
            String applicationId,
            String releaseId,
            String previousReleaseId,
            ReleaseRolloutPolicy policy
    ) {
        ReleaseRolloutPolicy effective = (policy == null) ? ReleaseRolloutPolicy.defaults() : policy;
        ReleaseRolloutRecord record = new ReleaseRolloutRecord(
                applicationId,
                releaseId,
                previousReleaseId,
                RolloutStatus.CANARY,
                effective.canaryPercentage(),
                Instant.now()
        );
        rollouts.put(applicationId, record);
        return record;
    }

    /**
     * 根据实时监控指标成功率，阶梯式推进行放量（Advance Rollout）或阻断暂停/自动回滚。
     *
     * @param applicationId 应用 ID
     * @param successRate 当前灰度流量的业务成功率 (0.0 - 1.0)
     * @param policy 放量策略 ReleaseRolloutPolicy
     * @return 推进更新后的灰度记录 ReleaseRolloutRecord
     */
    public ReleaseRolloutRecord advance(String applicationId, double successRate, ReleaseRolloutPolicy policy) {
        if (successRate < 0.0D || successRate > 1.0D) {
            throw new IllegalArgumentException("放量调拨检测时的成功率参数 successRate 必须在 0.0 至 1.0 之间。");
        }
        ReleaseRolloutRecord current = require(applicationId);
        ReleaseRolloutPolicy effective = (policy == null) ? ReleaseRolloutPolicy.defaults() : policy;

        if (current.status() == RolloutStatus.PAUSED || current.status() == RolloutStatus.ROLLED_BACK) {
            throw new IllegalStateException("当前灰度发布处于 " + current.status() + " 状态，不允许继续进行阶梯放量。");
        }

        if (successRate < effective.minimumSuccessRate()) {
            if (effective.autoRollback() && current.previousReleaseId() != null) {
                return rollback(applicationId);
            }
            ReleaseRolloutRecord paused = replace(current, RolloutStatus.PAUSED, current.trafficPercentage());
            rollouts.put(applicationId, paused);
            return paused;
        }

        int nextTraffic = Math.min(100, current.trafficPercentage() + effective.rolloutStepPercentage());
        RolloutStatus nextStatus = (nextTraffic >= 100) ? RolloutStatus.ACTIVE : RolloutStatus.ROLLING_OUT;
        ReleaseRolloutRecord updated = replace(current, nextStatus, nextTraffic);
        rollouts.put(applicationId, updated);
        return updated;
    }

    /**
     * 紧急切回历史稳定版本指针，执行一键回滚。
     *
     * @param applicationId 应用 ID
     * @return 回滚后的灰度记录 ReleaseRolloutRecord
     */
    public ReleaseRolloutRecord rollback(String applicationId) {
        ReleaseRolloutRecord current = require(applicationId);
        if (current.previousReleaseId() == null || current.previousReleaseId().isBlank()) {
            throw new IllegalStateException("当前灰度发布记录中不存在有效的历史上一发布版本 previousReleaseId，无法执行自动回滚。");
        }
        ReleaseRolloutRecord updated = replace(current, RolloutStatus.ROLLED_BACK, 0);
        rollouts.put(applicationId, updated);
        return updated;
    }

    /** 校验是否存在在线灰度发布 */
    private ReleaseRolloutRecord require(String applicationId) {
        ReleaseRolloutRecord record = rollouts.get(applicationId);
        if (record == null) {
            throw new IllegalArgumentException("当前应用 [" + applicationId + "] 尚未开启任何进行中的灰度发布流程。");
        }
        return record;
    }

    /** 替换灰度放量状态与流量比例 */
    private ReleaseRolloutRecord replace(ReleaseRolloutRecord current, RolloutStatus status, int traffic) {
        return new ReleaseRolloutRecord(
                current.applicationId(),
                current.releaseId(),
                current.previousReleaseId(),
                status,
                traffic,
                Instant.now()
        );
    }
}

