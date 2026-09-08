package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RunStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 运行（Run）核心有限状态机（Run State Machine）。
 * 严密校验与约束 Agent 运行全生命周期中的状态跳转（如 QUEUED -> RUNNING, RUNNING -> WAITING_HUMAN / SUCCEEDED / FAILED），
 * 严格拒绝非法、无序或越权的状态跳转，保证底层引擎的并发状态安全。
 */
public final class RunStateMachine {

    /** 状态可允许跳转规则映射表 Map&lt;RunStatus, Set&lt;RunStatus&gt;&gt; */
    private static final Map<RunStatus, Set<RunStatus>> TRANSITIONS = transitions();

    /**
     * 私有构造函数，防止实例化状态机工具类。
     */
    private RunStateMachine() {
    }

    /**
     * 判断当前状态（from）是否允许合法转换为目标状态（to）。
     *
     * @param from 当前状态
     * @param to 期望转换的目标状态
     * @return true 表示合法允许转换，false 表示转换非法
     */
    public static boolean canTransition(RunStatus from, RunStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * 执行状态转换校验并返回目标状态，若转换非法则抛出 IllegalStateException。
     *
     * @param from 当前状态
     * @param to 期望目标状态
     * @return 转换后的目标状态 to
     */
    public static RunStatus transition(RunStatus from, RunStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("检测到非法的 Run 运行状态转换跳转：" + from + " -> " + to);
        }
        return to;
    }

    /** 构建初始化的状态转换规则图 */
    private static Map<RunStatus, Set<RunStatus>> transitions() {
        EnumMap<RunStatus, Set<RunStatus>> transitions = new EnumMap<>(RunStatus.class);

        transitions.put(
                RunStatus.QUEUED,
                EnumSet.of(RunStatus.RUNNING, RunStatus.CANCELLED, RunStatus.EXPIRED)
        );
        transitions.put(
                RunStatus.RUNNING,
                EnumSet.of(
                        RunStatus.WAITING_TOOL,
                        RunStatus.WAITING_HUMAN,
                        RunStatus.PAUSED,
                        RunStatus.SUCCEEDED,
                        RunStatus.FAILED,
                        RunStatus.CANCELLED,
                        RunStatus.EXPIRED
                )
        );
        transitions.put(
                RunStatus.WAITING_TOOL,
                EnumSet.of(RunStatus.RUNNING, RunStatus.FAILED, RunStatus.CANCELLED, RunStatus.EXPIRED)
        );
        transitions.put(
                RunStatus.WAITING_HUMAN,
                EnumSet.of(RunStatus.RUNNING, RunStatus.FAILED, RunStatus.CANCELLED, RunStatus.EXPIRED)
        );
        transitions.put(
                RunStatus.PAUSED,
                EnumSet.of(RunStatus.QUEUED, RunStatus.CANCELLED, RunStatus.EXPIRED)
        );

        return Map.copyOf(transitions);
    }
}

