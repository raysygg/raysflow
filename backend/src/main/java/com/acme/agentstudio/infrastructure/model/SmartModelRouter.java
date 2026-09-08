package com.acme.agentstudio.infrastructure.model;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelRouterRuleEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelRouterRuleMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/**
 * 智能大模型路由中心组件（Smart Model Router）。
 * 根据租户配置的正则路由规则（SysModelRouterRuleEntity）对输入指令进行文本匹配，
 * 自动分流至高性价比或旗舰模型，并生成带备用模型（Backup Model）的降级故障转移列表 RoutedModel。
 */
@Component
public class SmartModelRouter {

    private static final Logger log = LoggerFactory.getLogger(SmartModelRouter.class);

    private final SysModelRouterRuleMapper sysModelRouterRuleMapper;
    private final ChatModelRegistry chatModelRegistry;

    public SmartModelRouter(SysModelRouterRuleMapper sysModelRouterRuleMapper, ChatModelRegistry chatModelRegistry) {
        this.sysModelRouterRuleMapper = sysModelRouterRuleMapper;
        this.chatModelRegistry = chatModelRegistry;
    }

    /**
     * 依据分流路由策略，解析获取目标大模型实例
     */
    public ChatLanguageModel route(Long tenantId, String defaultModelKey, String userMessage) {
        return routeCandidates(tenantId, defaultModelKey, userMessage, "").get(0).model();
    }

    /**
     * 返回按优先级排列的候选模型。候选模型既包含全局智能路由规则，
     * 也包含工作流 LLM 节点配置的备用模型，实际调用失败时由调用方继续尝试下一个。
     */
    public List<RoutedModel> routeCandidates(Long tenantId, String defaultModelKey, String userMessage, String nodeBackupModelKey) {
        LinkedHashSet<String> modelKeys = new LinkedHashSet<>();
        List<SysModelRouterRuleEntity> rules = sysModelRouterRuleMapper.selectList(
                new QueryWrapper<SysModelRouterRuleEntity>().eq("tenant_id", tenantId).eq("status", "ACTIVE"));
        for (SysModelRouterRuleEntity rule : rules) {
            try {
                if (Pattern.compile(rule.getPatternRegex()).matcher(userMessage == null ? "" : userMessage).find()) {
                    modelKeys.add(rule.getPrimaryModelKey());
                    modelKeys.add(rule.getBackupModelKey());
                    break;
                }
            } catch (Exception exception) {
                log.warn("[智能路由] 路由规则 '{}' 无法匹配，已跳过。", rule.getRuleName());
            }
        }
        modelKeys.add(defaultModelKey);
        modelKeys.add(nodeBackupModelKey);

        List<RoutedModel> candidates = new ArrayList<>();
        IllegalStateException lastFailure = null;
        for (String modelKey : modelKeys) {
            if (modelKey == null || modelKey.isBlank()) {
                continue;
            }
            try {
                candidates.add(new RoutedModel(modelKey, chatModelRegistry.getModel(tenantId, modelKey)));
            } catch (IllegalStateException exception) {
                lastFailure = exception;
                log.warn("[智能路由] 模型 '{}' 不可用，继续尝试下一个候选模型。", modelKey);
            }
        }

        if (candidates.isEmpty()) {
            throw (lastFailure == null) ? new IllegalStateException("没有可用的模型底座。") : lastFailure;
        }

        return candidates;
    }

    /** 路由得到的模型与 ModelKey 绑定实体 Record */
    public record RoutedModel(String modelKey, ChatLanguageModel model) {
    }
}

