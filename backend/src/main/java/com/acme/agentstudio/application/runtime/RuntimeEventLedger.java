package com.acme.agentstudio.application.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 运行事件序列连续性与幂等去重账本（Runtime Event Ledger）。
 * 保证在单次 Run 内只严格接收严格单调递增的下一个连续事件（sequence = expected），拒绝乱序、重复或遗漏的事件，防止前端事件消费紊乱与重复记账。
 */
public final class RuntimeEventLedger {

    /** 各 Run 当前已确认的最末连续事件序号映射 Map&lt;runId, sequence&gt; */
    private final Map<String, Long> lastSequences = new HashMap<>();

    /** 已经接收过的全局事件 ID 判重 Set&lt;eventId&gt; */
    private final Set<String> eventIds = new HashSet<>();

    /**
     * 尝试向账本提交一条 RuntimeEvent 事件。
     * 若事件已存在（以 eventId 判重）或序号不满足严格连续递增（sequence == lastSequence + 1），则直接拒绝并返回 false。
     *
     * @param event 待提交的运行事件 RuntimeEvent
     * @return true 表示成功挂载并更新游标，false 表示被判定为重复或乱序事件
     */
    public synchronized boolean accept(RuntimeEvent event) {
        if (event == null) {
            return false;
        }
        if (!eventIds.add(event.eventId())) {
            return false;
        }

        long expected = lastSequences.getOrDefault(event.runId(), -1L) + 1;
        if (event.sequence() != expected) {
            eventIds.remove(event.eventId());
            return false;
        }

        lastSequences.put(event.runId(), event.sequence());
        return true;
    }

    /**
     * 查询指定 Run 当前账本已接收的最末连续事件序号。
     *
     * @param runId 运行 ID
     * @return 最末连续事件序号（初始未接收时返回 -1L）
     */
    public synchronized long lastSequence(String runId) {
        return lastSequences.getOrDefault(runId, -1L);
    }
}

