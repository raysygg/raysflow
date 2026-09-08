package com.acme.agentstudio.domain.knowledge.model;

/**
 * KnowledgeIndexingProgress 领域异步事件通知对象。
 */
/**
 * KnowledgeIndexingProgress 领域事件通知对象。
 */
public record KnowledgeIndexingProgressEvent(
        Long documentId,
        String phase,
        String phaseLabel,
        int progressPercent,
        int processedChunks,
        int totalChunks,
        int queuePosition,
        String message,
        long sequence,
        long timestamp
) {
    public static KnowledgeIndexingProgressEvent of(Long documentId, String phase, String phaseLabel,
                                                   int progressPercent, int processedChunks, int totalChunks,
                                                   int queuePosition, String message) {
        return new KnowledgeIndexingProgressEvent(
                documentId, phase, phaseLabel, progressPercent, processedChunks, totalChunks,
                queuePosition, message, 0L, System.currentTimeMillis()
        );
    }

    public static KnowledgeIndexingProgressEvent of(Long documentId, String phase, String phaseLabel,
                                                   int progressPercent, int processedChunks, int totalChunks,
                                                   String message) {
        return of(documentId, phase, phaseLabel, progressPercent, processedChunks, totalChunks, 0, message);
    }

    public KnowledgeIndexingProgressEvent withSequence(long nextSequence) {
        return new KnowledgeIndexingProgressEvent(documentId, phase, phaseLabel, progressPercent,
                processedChunks, totalChunks, queuePosition, message, nextSequence, timestamp);
    }

        /**
         * terminal 方法。
         * @return boolean 返回对象
         */
    public boolean terminal() {
        return "COMPLETED".equals(phase) || "FAILED".equals(phase);
    }
}
