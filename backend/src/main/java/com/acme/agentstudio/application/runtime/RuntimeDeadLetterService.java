package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeDeadLetter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存型死信队列与人工重放服务（Runtime Dead Letter Service）。
 * 用于缓存无法通过常规重试恢复的终端错误任务（RuntimeDeadLetter），提供死信查询、记录与人工重放时的安全提取卸载（removeForReplay）。
 */
@Service
public class RuntimeDeadLetterService {

    /** 内存死信映射 Map（Key 为 taskId） */
    private final Map<String, RuntimeDeadLetter> letters = new ConcurrentHashMap<>();

    /**
     * 记录一条死信任务信息。
     *
     * @param letter 死信实体 RuntimeDeadLetter
     * @return 记录成功的死信实体 RuntimeDeadLetter
     */
    public RuntimeDeadLetter record(RuntimeDeadLetter letter) {
        if (letter == null) {
            throw new IllegalArgumentException("写入死信队列时，死信记录对象 RuntimeDeadLetter 不能为空。");
        }
        letters.put(letter.taskId(), letter);
        return letter;
    }

    /**
     * 获取当前内存队列中的所有死信记录列表。
     *
     * @return 死信列表 List&lt;RuntimeDeadLetter&gt;
     */
    public List<RuntimeDeadLetter> list() {
        return letters.values().stream().toList();
    }

    /**
     * 按任务尝试 ID 检索死信实体，不存在时抛出 IllegalArgumentException。
     *
     * @param taskId 任务 ID
     * @return 死信实体 RuntimeDeadLetter
     */
    public RuntimeDeadLetter require(String taskId) {
        RuntimeDeadLetter letter = letters.get(taskId);
        if (letter == null) {
            throw new IllegalArgumentException("指定任务 ID [" + taskId + "] 的死信记录不存在。");
        }
        return letter;
    }

    /**
     * 提取死信任务并从死信队列中移除（准备进行重放）。
     *
     * @param taskId 任务 ID
     * @return 被移除的死信实体 RuntimeDeadLetter
     */
    public RuntimeDeadLetter removeForReplay(String taskId) {
        RuntimeDeadLetter letter = require(taskId);
        letters.remove(taskId);
        return letter;
    }
}

