package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCalibration;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate;
import java.util.List;

/**
 * 检索相似度得分空间校准与相关性阈值校准端口接口（Retrieval Score Calibrator）。
 * 根据配置的动态阈值进行切分过滤与无命中 (No Hit) 拦截防护。
 */
public interface RetrievalScoreCalibrator {

    /**
     * 对多路召回/重排后的候选结果列表实施分值校准与阈值过滤。
     *
     * @param profile 向量与重排配置 Profile
     * @param candidates 候选结果列表 List&lt;RetrievalCandidate&gt;
     * @return 包含过滤后候选点与无命中标志的 RetrievalCalibration 校准对象
     */
    RetrievalCalibration calibrate(RagEmbeddingProfile profile, List<RetrievalCandidate> candidates);
}

