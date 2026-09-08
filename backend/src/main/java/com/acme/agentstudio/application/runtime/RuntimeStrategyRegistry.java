package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.RuntimeStrategyContracts;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时策略注册中心与能力注册表（Runtime Strategy Registry）。
 * 自动收集 Spring 容器中所有的 RuntimeStrategy 实现类，按 RuntimeMode 进行唯一映射校验（重复配置抛出 IllegalStateException）；
 * 提供 require() 策略强校验、isRunnable() 可运行诊断、descriptors() 策略能力描述以及 templates() 配置模板清单列表。
 */
@Component
public class RuntimeStrategyRegistry {

    /** 策略只读 EnumMap 映射表 */
    private final Map<RuntimeMode, RuntimeStrategy> strategies;

    /**
     * 构造函数自动注入所有已注入的 RuntimeStrategy 列表并组装注册表。
     *
     * @param availableStrategies 依赖注入的所有策略实现类列表
     */
    public RuntimeStrategyRegistry(List<RuntimeStrategy> availableStrategies) {
        EnumMap<RuntimeMode, RuntimeStrategy> resolved = new EnumMap<>(RuntimeMode.class);
        for (RuntimeStrategy strategy : availableStrategies) {
            if (resolved.put(strategy.mode(), strategy) != null) {
                throw new IllegalStateException("检测到运行模式 [" + strategy.mode() + "] 存在重复的策略实现类，无法进行唯一注册。");
            }
        }
        this.strategies = Map.copyOf(resolved);
    }

    /**
     * 根据指定的运行模式 RuntimeMode 获取对应的策略对象 RuntimeStrategy。
     *
     * @param mode 运行模式 RuntimeMode
     * @return 匹配的 RuntimeStrategy 策略实现
     */
    public RuntimeStrategy require(RuntimeMode mode) {
        RuntimeStrategy strategy = strategies.get(mode);
        if (strategy == null) {
            throw new IllegalArgumentException("请求的运行模式 [" + mode + "] 在当前注册中心中尚未配置或启用对应的策略实现。");
        }
        return strategy;
    }

    /**
     * 判断当前注册表中是否支持并注册了特定模式 RuntimeMode。
     *
     * @param mode 运行模式 RuntimeMode
     * @return true 表示已注册支持，false 表示未注册
     */
    public boolean supports(RuntimeMode mode) {
        return (mode != null) && strategies.containsKey(mode);
    }

    /**
     * 判断策略是否已经接入真实的模型/工具/流程执行器并且处于 READY 就绪状态。
     *
     * @param mode 运行模式 RuntimeMode
     * @return true 表示可真实提交运行，false 表示尚处于占位诊断阶段
     */
    public boolean isRunnable(RuntimeMode mode) {
        RuntimeStrategy strategy = strategies.get(mode);
        return (strategy != null) && strategy.descriptor().ready();
    }

    /**
     * 返回当前注册表中所有策略的能力描述符 Descriptor 列表。
     *
     * @return 策略能力描述符列表 List&lt;RuntimeStrategyContracts.Descriptor&gt;
     */
    public List<RuntimeStrategyContracts.Descriptor> descriptors() {
        return strategies.values().stream()
                .map(RuntimeStrategy::descriptor)
                .toList();
    }

    /**
     * 返回所有策略的配置模板 StrategyTemplate 列表，供创建向导、编排器和发布门禁共同使用。
     *
     * @return 策略模板列表 List&lt;RuntimeStrategyContracts.StrategyTemplate&gt;
     */
    public List<RuntimeStrategyContracts.StrategyTemplate> templates() {
        return strategies.values().stream()
                .map(RuntimeStrategy::template)
                .toList();
    }
}

