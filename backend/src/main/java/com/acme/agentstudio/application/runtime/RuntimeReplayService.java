package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeReplaySnapshot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * 运行时历史重放快照捕获与版本一致性校验服务（Runtime Replay Service）。
 * 从给定的 RuntimeContext 运行上下文解析并捕获只读的历史重放快照（RuntimeReplaySnapshot），
 * 冻结当时的原始输入、Prompt 模板配置、知识库绑定与工具参数，确保后续重放或者复盘时不受新线上发布版本的干扰污染。
 */
@Service
public class RuntimeReplayService {

    /** Prompt 提示词快照 Key */
    private static final String PROMPT_KEY = "prompt";

    /** 知识库绑定快照 Key */
    private static final String KNOWLEDGE_KEY = "knowledge";

    /**
     * 从给定的 RuntimeContext 捕获不可变的历史重放快照。
     *
     * @param context 运行上下文对象 RuntimeContext
     * @return 构建好的历史重放快照实体 RuntimeReplaySnapshot
     */
    public RuntimeReplaySnapshot capture(RuntimeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("捕获重放快照时，运行上下文 RuntimeContext 不能为空。");
        }
        Map<String, Object> snapshot = context.resolvedSnapshot();
        return new RuntimeReplaySnapshot(
                context.runId(),
                context.releaseId(),
                context.input(),
                map(snapshot, PROMPT_KEY),
                map(snapshot, KNOWLEDGE_KEY),
                context.tools(),
                context.policy(),
                Instant.now()
        );
    }

    /**
     * 判断重放快照是否归属于特定的发布版本 releaseId。
     *
     * @param snapshot 重放快照对象 RuntimeReplaySnapshot
     * @param releaseId 发布版本 ID
     * @return true 表示版本号一致归属，false 表示不匹配
     */
    public boolean belongsToRelease(RuntimeReplaySnapshot snapshot, String releaseId) {
        if (snapshot == null || releaseId == null) {
            return false;
        }
        return releaseId.equals(snapshot.releaseId());
    }

    /** 安全提取 Map 中的子 Map */
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return (value instanceof Map<?, ?> map) ? (Map<String, Object>) map : Map.of();
    }
}

