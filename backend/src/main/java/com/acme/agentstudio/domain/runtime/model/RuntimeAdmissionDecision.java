package com.acme.agentstudio.domain.runtime.model;

import java.util.List;

/**
 * Agent Runtime 任务运行前置准入校验决策 Record（Runtime Admission Decision）。
 * 在后端实际触发 Worker 分发和模型调用前必须通过此准入门禁校验。
 *
 * @param allowed 是否允许运行准入（true 表示通过放行）
 * @param reasons 阻断或提示的中文具体原因列表 List&lt;String&gt;
 */
public record RuntimeAdmissionDecision(
        boolean allowed,
        List<String> reasons
) {
    /** 紧凑构造函数防护列表 null 值 */
    public RuntimeAdmissionDecision {
        reasons = (reasons == null) ? List.of() : List.copyOf(reasons);
    }
}

