package com.acme.agentstudio.domain.runtime.model;

import java.util.List;
import java.util.Map;

/**
 * 应用版本发布前必须通过的质量与安全发布门禁评估报告 Record（Publish Gate Report）。
 * 包含门禁评估是否通过 passed、阻断强拦截原因 blockers、软告警提示 warnings 以及各项基线测试数据 measurements。
 *
 * @param passed 是否允许准予发布上线
 * @param blockers 阻断发布的错误红线提示列表 List&lt;String&gt;
 * @param warnings 非阻断提示性告警列表 List&lt;String&gt;
 * @param measurements 各质量指标得分及对比数据 Map
 */
public record PublishGateReport(
        boolean passed,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> measurements
) {
    /** 紧凑构造函数做防空保护 */
    public PublishGateReport {
        blockers = (blockers == null) ? List.of() : List.copyOf(blockers);
        warnings = (warnings == null) ? List.of() : List.copyOf(warnings);
        measurements = (measurements == null) ? Map.of() : Map.copyOf(measurements);
    }
}

