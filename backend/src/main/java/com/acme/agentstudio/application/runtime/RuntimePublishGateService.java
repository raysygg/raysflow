package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.PublishGateReport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时生产发布门禁综合评估服务（Runtime Publish Gate Service）。
 * 统一执行配置结构、静态依赖规则、离线评测质量指标（任务完成率 taskCompletionRate / RAG 忠实度 groundedAnswerRate / 挂起恢复率 recoveryRate）
 * 以及 P95 响应延迟与单次成本上限等多维门禁评估，生成可直接阻断发布的综合门禁报告（PublishGateReport）。
 */
@Service
public class RuntimePublishGateService {

    /** 任务完成率指标键 */
    private static final String MEASURE_TASK_COMPLETION = "taskCompletionRate";

    /** RAG 忠实度（拒绝幻觉）指标键 */
    private static final String MEASURE_GROUNDEDNESS = "groundedAnswerRate";

    /** 挂起与异常恢复率指标键 */
    private static final String MEASURE_RECOVERY = "recoveryRate";

    /** P95 延迟毫秒数指标键 */
    private static final String MEASURE_LATENCY = "p95LatencyMillis";

    /** 平均微元成本指标键 */
    private static final String MEASURE_COST = "averageCostMicros";

    /**
     * 评估应用发布门禁，收集硬性阻断项（blockers）与软性警告项（warnings）。
     *
     * @param configuration 应用运行配置 Map
     * @param measurements 离线评测生成的指标 Map
     * @param checks 静态发布检查结果 Map&lt;String, Boolean&gt;
     * @param minimumTaskCompletion 最低任务完成率要求 (0.0 - 1.0)
     * @param minimumGroundedness 最低 RAG 忠实度要求 (0.0 - 1.0)
     * @param minimumRecovery 最低故障恢复率要求 (0.0 - 1.0)
     * @param maximumLatencyMillis 最大允许 P95 响应延迟（毫秒）
     * @param maximumCostMicros 最大允许平均成本（微元）
     * @return 发布门禁评估报告 PublishGateReport
     */
    public PublishGateReport evaluate(
            Map<String, Object> configuration,
            Map<String, Number> measurements,
            Map<String, Boolean> checks,
            double minimumTaskCompletion,
            double minimumGroundedness,
            double minimumRecovery,
            long maximumLatencyMillis,
            long maximumCostMicros
    ) {
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Object> values = new LinkedHashMap<>();

        if (configuration == null || configuration.isEmpty()) {
            blockers.add("应用运行配置为空，无法通过发布门禁。");
        }

        if (checks == null || checks.isEmpty()) {
            blockers.add("静态发布检查项尚未执行。");
        } else {
            checks.forEach((name, passed) -> {
                if (!Boolean.TRUE.equals(passed)) {
                    blockers.add("静态发布检查未通过：" + name);
                }
            });
        }

        if (measurements == null) {
            blockers.add("质量与性能离线评测指标尚未生成。");
        } else {
            measurements.forEach(values::put);
            requireMinimum(measurements, MEASURE_TASK_COMPLETION, minimumTaskCompletion, blockers);
            requireMinimum(measurements, MEASURE_GROUNDEDNESS, minimumGroundedness, blockers);
            requireMinimum(measurements, MEASURE_RECOVERY, minimumRecovery, blockers);
            requireMaximum(measurements, MEASURE_LATENCY, maximumLatencyMillis, blockers);
            requireMaximum(measurements, MEASURE_COST, maximumCostMicros, warnings);
        }

        return new PublishGateReport(blockers.isEmpty(), blockers, warnings, values);
    }

    /** 校验下限基线要求（低于基线写入 blockers 阻断发布） */
    private void requireMinimum(Map<String, Number> values, String key, double threshold, List<String> blockers) {
        Number value = values.get(key);
        if (value == null || value.doubleValue() < threshold) {
            blockers.add("质量指标 [" + key + "] 未达到生产发布最低门禁阈值 (" + threshold + ")。");
        }
    }

    /** 校验上限预警要求（高于上限写入 warnings 提出警告） */
    private void requireMaximum(Map<String, Number> values, String key, long threshold, List<String> messages) {
        Number value = values.get(key);
        if (value != null && value.longValue() > threshold) {
            messages.add("性能/成本指标 [" + key + "] (" + value + ") 已超过推荐上限 (" + threshold + ")。");
        }
    }
}

