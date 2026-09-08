package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeIndexingProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 知识库 FIFO 公平处理队列与排队感知引擎（Knowledge Indexing Task Queue）。
 * 采用先进先出 (FIFO) 顺序排队处理文档索引任务，彻底消除并发锁争用与写盘冲突。
 * 通过统一 WebSocket 链路实时推送排队位置与当前处理阶段。
 */
@Component
public class KnowledgeIndexingTaskQueue {

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeIndexingTaskQueue.class);

    private final KnowledgeProgressBroadcaster broadcaster;
    private final ConcurrentLinkedQueue<IndexingTask> queue = new ConcurrentLinkedQueue<>();
    private final Map<Long, Integer> documentPositions = new ConcurrentHashMap<>();
    private final ExecutorService singleWorker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public record IndexingTask(
            Long documentId,
            Runnable taskAction,
            String documentTitle
    ) {}

    public KnowledgeIndexingTaskQueue(KnowledgeProgressBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

        /**
         * submit 方法。
         *
         * @param documentId documentId 参数
         * @param title title 参数
         * @param taskAction taskAction 参数
         */
    public void submit(Long documentId, String title, Runnable taskAction) {
        IndexingTask task = new IndexingTask(documentId, taskAction, title);
        queue.add(task);
        updateQueuePositionsAndBroadcast();
        triggerWorker();
    }

        /**
         * 获取getQueuePosition 业务逻辑处理。
         *
         * @param documentId documentId 参数
         * @return int 返回对象
         */
    public int getQueuePosition(Long documentId) {
        return documentPositions.getOrDefault(documentId, 0);
    }

    private void updateQueuePositionsAndBroadcast() {
        int pos = 0;
        for (IndexingTask t : queue) {
            pos++;
            documentPositions.put(t.documentId(), pos);
            if (pos > 1) {
                int ahead = pos - 1;
                broadcaster.broadcast(KnowledgeIndexingProgressEvent.of(
                        t.documentId(),
                        "QUEUED",
                        "前方排队中",
                        0,
                        0,
                        0,
                        ahead,
                        "前面还有 " + ahead + " 份文档正在处理，请稍等片刻，马上就轮到您啦~"
                ));
            }
        }
    }

    private void triggerWorker() {
        if (isProcessing.compareAndSet(false, true)) {
            singleWorker.submit(this::processQueue);
        }
    }

    private void processQueue() {
        try {
            while (!queue.isEmpty()) {
                IndexingTask current = queue.poll();
                if (current == null) break;

                documentPositions.remove(current.documentId());
                updateQueuePositionsAndBroadcast();

                LOG.info("【知识队列】开始处理任务: documentId={}, title={}", current.documentId(), current.documentTitle());
                broadcaster.broadcast(KnowledgeIndexingProgressEvent.of(
                        current.documentId(),
                        "PARSING",
                        "准备就绪，开始读取",
                        5,
                        0,
                        0,
                        0,
                        "轮到您啦！正在为您认真读取并解析文档内容..."
                ));

                try {
                    current.taskAction().run();
                } catch (Exception ex) {
                    LOG.error("【FIFO 队列】任务执行异常 documentId={}: {}", current.documentId(), ex.getMessage(), ex);
                }
            }
        } finally {
            isProcessing.set(false);
            if (!queue.isEmpty()) {
                triggerWorker();
            }
        }
    }
}
