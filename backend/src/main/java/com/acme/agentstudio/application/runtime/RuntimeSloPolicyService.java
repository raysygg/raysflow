package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.SliSnapshot;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.SliType;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeSloPolicyEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeSloPolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运行时 SLO 监控策略与服务质量评估服务（Runtime SLO Policy Service）。
 * 针对特定的租户、应用和发布版本定义强类型 SLO 指标阈值目标（SloTarget），包含成功率 successRate、失败率 failureRate、P95 响应延迟 p95LatencyMs 以及恢复率 recoveryRate。
 * 结合滑动窗口内遥测快照 SliSnapshot 自动评估指标违例（breached），触发报警，并支持新发布版本与历史基线版本的全量对比（ReleaseComparison）。
 */
@Service
public class RuntimeSloPolicyService {

    /** SLO 策略定义 Mapper */
    private final RuntimeSloPolicyMapper policyMapper;

    /** 运行时遥测指标服务 */
    private final RuntimeTelemetryService telemetryService;

    /** SLO 评估与告警服务 */
    private final RuntimeSloService sloService;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入依赖服务。
     */
    public RuntimeSloPolicyService(
            RuntimeSloPolicyMapper policyMapper,
            RuntimeTelemetryService telemetryService,
            RuntimeSloService sloService,
            ObjectMapper objectMapper
    ) {
        this.policyMapper = policyMapper;
        this.telemetryService = telemetryService;
        this.sloService = sloService;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存或更新一条 SLO 监控策略定义。
     *
     * @param user 当前登录 SecurityUser
     * @param command SLO 策略保存指令 PolicyCommand
     * @return 保存后的策略视图对象 PolicyView
     */
    @Transactional
    public PolicyView save(SecurityUser user, PolicyCommand command) {
        requireUser(user);
        if (command == null || command.code() == null || command.code().isBlank() || command.target() == null) {
            throw new IllegalArgumentException("保存 SLO 策略时，策略 Code 和目标阈值 SloTarget 均不能为空。");
        }

        try {
            RuntimeSloPolicyEntity current = policyMapper.selectOne(new LambdaQueryWrapper<RuntimeSloPolicyEntity>()
                    .eq(RuntimeSloPolicyEntity::getTenantId, user.getTenantId())
                    .eq(RuntimeSloPolicyEntity::getApplicationId, command.applicationId())
                    .eq(RuntimeSloPolicyEntity::getPolicyCode, command.code()));

            LocalDateTime now = LocalDateTime.now();
            RuntimeSloPolicyEntity entity = (current == null) ? new RuntimeSloPolicyEntity() : current;
            entity.setTenantId(user.getTenantId());
            entity.setApplicationId(command.applicationId());
            entity.setReleaseId(command.releaseId());
            entity.setPolicyCode(command.code());
            entity.setTargetJson(objectMapper.writeValueAsString(command.target()));
            entity.setWindowMinutes(Math.max(1, command.windowMinutes()));
            entity.setEnabled(command.enabled());
            entity.setUpdatedAt(now);

            if (entity.getId() == null) {
                entity.setCreatedAt(now);
                policyMapper.insert(entity);
            } else {
                policyMapper.updateById(entity);
            }

            return toView(entity);
        } catch (Exception exception) {
            throw new IllegalStateException("保存 SLO 策略失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 查询当前租户归属下的 SLO 策略定义列表。
     *
     * @param user 当前登录 SecurityUser
     * @param applicationId 应用 ID（可选）
     * @return SLO 策略视图列表 List&lt;PolicyView&gt;
     */
    public List<PolicyView> list(SecurityUser user, Long applicationId) {
        requireUser(user);
        LambdaQueryWrapper<RuntimeSloPolicyEntity> query = new LambdaQueryWrapper<RuntimeSloPolicyEntity>()
                .eq(RuntimeSloPolicyEntity::getTenantId, user.getTenantId())
                .orderByAsc(RuntimeSloPolicyEntity::getPolicyCode);

        query.eq(applicationId != null, RuntimeSloPolicyEntity::getApplicationId, applicationId);
        return policyMapper.selectList(query).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 评估特定 SLO 策略在滑动窗口内的实测遥测快照与违例状态。
     *
     * @param user 当前登录 SecurityUser
     * @param applicationId 应用 ID
     * @param releaseId 发布版本 ID
     * @param policyCode 策略唯一代码
     * @return 提取并评估后的 SliSnapshot 结果列表
     */
    public List<SliSnapshot> evaluate(SecurityUser user, Long applicationId, String releaseId, String policyCode) {
        requireUser(user);
        RuntimeSloPolicyEntity policy = policyMapper.selectOne(new LambdaQueryWrapper<RuntimeSloPolicyEntity>()
                .eq(RuntimeSloPolicyEntity::getTenantId, user.getTenantId())
                .eq(RuntimeSloPolicyEntity::getApplicationId, applicationId)
                .eq(RuntimeSloPolicyEntity::getPolicyCode, policyCode)
                .eq(RuntimeSloPolicyEntity::getEnabled, true));

        if (policy == null) {
            throw new IllegalArgumentException("执行评估失败，找不到已启用且 Code 为 [" + policyCode + "] 的 SLO 策略。");
        }

        try {
            SloTarget target = objectMapper.readValue(policy.getTargetJson(), SloTarget.class);
            List<SliSnapshot> snapshots = telemetryService.snapshots(
                    user.getTenantId(),
                    applicationId,
                    releaseId,
                    null,
                    Duration.ofMinutes(policy.getWindowMinutes())
            );

            for (SliSnapshot snapshot : snapshots) {
                Double expected = target.value(snapshot.type());
                if (expected == null) {
                    continue;
                }

                boolean breached;
                if (snapshot.type() == SliType.P95_LATENCY || snapshot.type() == SliType.FAILURE_RATE) {
                    breached = snapshot.value() > expected;
                } else {
                    breached = snapshot.value() < expected;
                }

                String key = applicationId + ":" + releaseId + ":" + policyCode + ":" + snapshot.type();
                sloService.evaluate(user.getTenantId(), applicationId, releaseId, key, snapshot, expected, breached);
            }

            return snapshots;
        } catch (Exception exception) {
            throw new IllegalStateException("执行 SLO 策略计算评估失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 对比当前发布版本与历史基线版本（baselineReleaseId）在特定时间窗口内的 SLI 遥测指标变化与增量。
     *
     * @param user 当前登录 SecurityUser
     * @param applicationId 应用 ID
     * @param releaseId 当前发布版本 ID
     * @param baselineReleaseId 历史基线版本 ID
     * @param windowMinutes 时间窗口分钟数
     * @return 版本对比结果 ReleaseComparison
     */
    public ReleaseComparison compare(
            SecurityUser user,
            Long applicationId,
            String releaseId,
            String baselineReleaseId,
            int windowMinutes
    ) {
        requireUser(user);
        Duration window = Duration.ofMinutes(Math.max(1, windowMinutes));
        List<SliSnapshot> current = telemetryService.snapshots(user.getTenantId(), applicationId, releaseId, null, window);
        List<SliSnapshot> baseline = telemetryService.snapshots(user.getTenantId(), applicationId, baselineReleaseId, null, window);

        List<MetricDelta> deltas = current.stream().map(item -> {
            double base = baseline.stream()
                    .filter(previous -> previous.type() == item.type())
                    .mapToDouble(SliSnapshot::value)
                    .findFirst()
                    .orElse(0.0D);
            return new MetricDelta(item.type(), item.value(), base, item.value() - base);
        }).toList();

        return new ReleaseComparison(releaseId, baselineReleaseId, deltas);
    }

    /** 将实体对象转为 PolicyView */
    private PolicyView toView(RuntimeSloPolicyEntity entity) {
        try {
            return new PolicyView(
                    entity.getId(),
                    entity.getApplicationId(),
                    entity.getReleaseId(),
                    entity.getPolicyCode(),
                    objectMapper.readValue(entity.getTargetJson(), SloTarget.class),
                    entity.getWindowMinutes(),
                    Boolean.TRUE.equals(entity.getEnabled())
            );
        } catch (Exception exception) {
            throw new IllegalStateException("转换 SLO 策略实体失败，JSON 数据格式无效。", exception);
        }
    }

    /** 安全身份校验 */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null) {
            throw new IllegalArgumentException("当前登录用户身份信息无效，请重新登录。");
        }
    }

    /** 强类型 SLO 目标配置 Record */
    public record SloTarget(Double successRate, Double failureRate, Double p95LatencyMs, Double recoveryRate) {
        Double value(SliType type) {
            if (type == null) {
                return null;
            }
            return switch (type) {
                case SUCCESS_RATE -> successRate;
                case FAILURE_RATE -> failureRate;
                case P95_LATENCY -> p95LatencyMs;
                case RECOVERY_RATE -> recoveryRate;
                default -> null;
            };
        }
    }

    /** 策略保存指令 Record */
    public record PolicyCommand(Long applicationId, String releaseId, String code, SloTarget target, int windowMinutes, boolean enabled) {
    }

    /** 策略展示视图 Record */
    public record PolicyView(Long id, Long applicationId, String releaseId, String code, SloTarget target, int windowMinutes, boolean enabled) {
    }

    /** 指标增量对比 Record */
    public record MetricDelta(SliType type, double currentValue, double baselineValue, double delta) {
    }

    /** 版本遥测对比结果 Record */
    public record ReleaseComparison(String releaseId, String baselineReleaseId, List<MetricDelta> metrics) {
    }
}

