package com.acme.agentstudio.infrastructure.rag.model;

/**
 * 平台进程内本地预置嵌入 Embedding 模型目录枚举（Local Embedding Model）。
 * 模型编码和维度必须与实际 ONNX 实现一致，禁止再用配置名称冒充另一个模型。
 */
public enum LocalEmbeddingModel {
    MULTILINGUAL_MINILM("local-all-minilm-l6-v2", "本地多语言 MiniLM", 384),
    BGE_SMALL_ZH("local-bge-small-zh", "本地中文 BGE Small", 512);

    private final String modelKey;
    private final String modelName;
    private final int vectorDimension;

    LocalEmbeddingModel(String modelKey, String modelName, int vectorDimension) {
        this.modelKey = modelKey;
        this.modelName = modelName;
        this.vectorDimension = vectorDimension;
    }

        /**
         * modelKey 方法。
         * @return String 返回对象
         */
    public String modelKey() {
        return modelKey;
    }

        /**
         * modelName 方法。
         * @return String 返回对象
         */
    public String modelName() {
        return modelName;
    }

        /**
         * vectorDimension 方法。
         * @return int 返回对象
         */
    public int vectorDimension() {
        return vectorDimension;
    }

        /**
         * require 方法。
         *
         * @param modelKey modelKey 参数
         * @return static LocalEmbeddingModel 返回对象
         */
    public static LocalEmbeddingModel require(String modelKey) {
        for (LocalEmbeddingModel model : values()) {
            if (model.modelKey.equals(modelKey)) return model;
        }
        throw new IllegalArgumentException("不支持的本地 Embedding 模型：" + modelKey);
    }
}
