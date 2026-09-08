package com.acme.agentstudio.domain.runtime.model;

import java.util.List;
import java.util.Map;

/**
 * 外部工具调用前安全与入参脱敏校验结果 Record（Tool Validation Result）。
 * 明确是否放行 allowed、是否必须人工确认审批 requiresConfirmation、输入/输出内容可信分级 contentTrust (ContentTrust)、
 * 消毒脱敏后的规范参数 sanitizedArguments 及阻断或告警原因列表 reasons。
 *
 * @param allowed 是否通过安全校验放行调用
 * @param requiresConfirmation 是否挂起等待人工二次审批确认
 * @param contentTrust 内容安全可信度分级（TRUSTED / UNTRUSTED / SUSPICIOUS）
 * @param sanitizedArguments 剥离注入脚本与危险命令后的参数 Map
 * @param reasons 拦截或提示的原因列表 List&lt;String&gt;
 */
public record ToolValidationResult(
        boolean allowed,
        boolean requiresConfirmation,
        ContentTrust contentTrust,
        Map<String, Object> sanitizedArguments,
        List<String> reasons
) {
    /** 紧凑构造函数做输入属性校验 */
    public ToolValidationResult {
        if (contentTrust == null) {
            throw new IllegalArgumentException("内容可信等级不能为空");
        }
        sanitizedArguments = (sanitizedArguments == null) ? Map.of() : Map.copyOf(sanitizedArguments);
        reasons = (reasons == null) ? List.of() : List.copyOf(reasons);
    }
}

