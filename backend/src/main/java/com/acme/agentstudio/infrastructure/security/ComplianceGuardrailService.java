package com.acme.agentstudio.infrastructure.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.acme.agentstudio.infrastructure.persistence.entity.AuditLogEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysComplianceRuleEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AuditLogMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysComplianceRuleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ComplianceGuardrail 业务服务接口。
 * 定义 ComplianceGuardrail 相关的核心业务契约与流程接口。
 */
/**
 * 企业安全合规网关服务类
 * 负责在请求输入前及生成输出后对文本内容进行敏感词和黑名单正则拦截，记录高危审计事件，或执行掩码脱敏
 */
@Service
/**
 * 合规护栏服务。
 * 在模型调用前后执行租户配置的敏感内容规则，命中规则时阻断或记录审计，
 * 不负责替代模型本身的安全策略。
 */
/**
 * ComplianceGuardrail 业务逻辑服务接口。
 * 负责 ComplianceGuardrail 核心业务逻辑与流程编排。
 */
public class ComplianceGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceGuardrailService.class);

    private final SysComplianceRuleMapper sysComplianceRuleMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ComplianceGuardrailService(SysComplianceRuleMapper sysComplianceRuleMapper, AuditLogMapper auditLogMapper) {
        this.sysComplianceRuleMapper = sysComplianceRuleMapper;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 校验请求输入文本是否合规
     */
    public String checkInput(Long tenantId, String username, String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        // 加载当前租户下所有启用的过滤规则
        List<SysComplianceRuleEntity> rules = sysComplianceRuleMapper.selectList(
                new QueryWrapper<SysComplianceRuleEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("status", "ACTIVE")
        );

        String resultText = text;

        for (SysComplianceRuleEntity rule : rules) {
            String sensitiveWordConfig = rule.getSensitiveWord();
            if (sensitiveWordConfig == null || sensitiveWordConfig.isBlank()) {
                continue;
            }

            // 规则支持逗号分隔配置多个敏感正则匹配
            String[] words = sensitiveWordConfig.split(",");
            for (String patternStr : words) {
                patternStr = patternStr.trim();
                if (patternStr.isEmpty()) continue;

                try {
                    Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(resultText);

                    if (matcher.find()) {
                        if ("BLOCK".equalsIgnoreCase(rule.getActionType())) {
                            log.warn("[安全网关] 检测到合规敏感词触发。命中规则: '{}'，原始输入内容: '{}'", rule.getRuleName(), text);
                            
                            // 1. 记录高危行为审计（P1 级安全警报）
                            recordComplianceAudit(tenantId, username, "COMPLIANCE_BLOCK", "INPUT_BREACH", 
                                    "触发安全合规规则拦截，匹配词: " + patternStr, text);

                            // 2. 抛出中文业务异常进行直接拦截阻断
                            throw new IllegalArgumentException("您的输入触发了安全合规审计黑名单，已被拦截。敏感字串: " + patternStr);
                        } else if ("DESENSITIZE".equalsIgnoreCase(rule.getActionType())) {
                            // 对敏感词匹配串执行掩码星号替换脱敏
                            resultText = matcher.replaceAll(m -> {
                                String val = m.group();
                                if (val.length() <= 4) {
                                    return "****";
                                } else {
                                    // 保留首尾，对中间进行掩码（例如手机号 13812345678 -> 138****5678）
                                    return val.substring(0, 3) + "****" + val.substring(val.length() - 4);
                                }
                            });
                            log.info("[安全网关] 触发掩码脱敏规则: '{}'，脱敏后输出文本: '{}'", rule.getRuleName(), resultText);
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    throw ex; // 直接向上抛出安全异常
                } catch (Exception e) {
                    log.error("[安全网关] 执行合规敏感正则规则 '{}' 匹配校验时发生异常", patternStr, e);
                }
            }
        }

        return resultText;
    }

    /**
     * 校验模型输出内容是否合规
     */
    public String checkOutput(Long tenantId, String username, String text) {
        // 输出内容过滤复用输入校验策略
        return checkInput(tenantId, username, text);
    }

    /**
     * 内部方法：写入高危拦截审计日志
     */
    private void recordComplianceAudit(Long tenantId, String username, String actionType, String targetType, String reason, String payload) {
        try {
            AuditLogEntity audit = new AuditLogEntity();
            audit.setTenantId(tenantId);
            audit.setOperatorId(username);
            audit.setActionType(actionType);
            audit.setTargetType(targetType);
            audit.setTargetId("GUARDRAIL");
            audit.setRiskLevel("P1"); // P1 代表最高风险合规等级
            
            Map<String, Object> details = new HashMap<>();
            details.put("reason", reason);
            details.put("payload_preview", payload.length() > 256 ? payload.substring(0, 256) + "..." : payload);
            
            audit.setDetailJson(objectMapper.writeValueAsString(details));
            audit.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(audit);
        } catch (Exception e) {
            log.error("[安全网关] 记录合规拦截行为审计日志到数据库失败", e);
        }
    }
}
