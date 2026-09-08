package com.acme.agentstudio.infrastructure.rag.vector;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.knowledge.model.LexicalSearchCandidate;
import com.acme.agentstudio.domain.knowledge.model.LexicalSearchRequest;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RetrievalHitReason;
import com.acme.agentstudio.domain.knowledge.model.SparseVectorData;
import com.acme.agentstudio.domain.knowledge.model.VectorIndexPoint;
import com.acme.agentstudio.domain.knowledge.model.VectorSearchCandidate;
import com.acme.agentstudio.domain.knowledge.model.VectorSearchRequest;
import com.acme.agentstudio.domain.knowledge.model.VectorCollectionInspection;
import com.acme.agentstudio.domain.knowledge.port.VectorIndexProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Qdrant 向量数据库的基础设施端口实现类（Qdrant Vector Index Provider）。
 * 负责向量 Collection 创建、管理、Point 增删改查 Upsert 以及稠密/稀疏向量搜索。
 * Qdrant 只承载可重建索引，不承载知识正文。
 */
@Component
public class QdrantVectorIndexProvider implements VectorIndexProvider {
    private static final Logger LOG = LoggerFactory.getLogger(QdrantVectorIndexProvider.class);
    private static final String DENSE_VECTOR = "dense_vector";
    private static final String LEXICAL_SPARSE_VECTOR = "lexical_sparse";
    private static final String SPARSE_MODIFIER_IDF = "idf";
    private static final String PAYLOAD_INDEX_INTEGER = "integer";
    private static final String PAYLOAD_INDEX_KEYWORD = "keyword";
    private static final int MAX_QDRANT_BATCH_SIZE = 512;
    private static final String PAYLOAD_TENANT_ID = "tenant_id";
    private static final String PAYLOAD_DOCUMENT_ID = "document_id";
    private static final String PAYLOAD_CHUNK_ID = "chunk_id";
    private static final String PAYLOAD_GENERATION_ID = "index_generation_id";
    private static final String PAYLOAD_PARENT_CHUNK_ID = "parent_chunk_id";
    private static final String PAYLOAD_LANGUAGE = "language";
    private static final String PAYLOAD_CHUNK_NO = "chunk_no";
    private static final String PAYLOAD_SECTION_PATH = "section_path";
    private static final String PAYLOAD_EXACT_KEYS = "exact_keys";

    private final RagProperties properties;
    private final RestClient client;

    public QdrantVectorIndexProvider(RagProperties properties) {
        this.properties = properties;
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory())
                .baseUrl(properties.getQdrant().getBaseUrl());
        if (properties.getQdrant().getApiKey() != null && !properties.getQdrant().getApiKey().isBlank()) {
            builder.defaultHeader("api-key", properties.getQdrant().getApiKey());
        }
        this.client = builder.build();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        int timeoutMs = Math.max(1, properties.getQdrant().getTimeoutSeconds()) * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return factory;
    }

        /**
         * 校验checkHealth 业务逻辑处理。
         */
    @Override
    public void checkHealth() {
        try {
            client.get().uri("/readyz").retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Qdrant 向量服务不可用，请检查服务地址和访问凭证。", exception);
        }
    }

        /**
         * ensureCollection 方法。
         *
         * @param collectionName collectionName 参数
         * @param profile profile 参数
         */
    @Override
    public void ensureCollection(String collectionName, RagEmbeddingProfile profile) {
        if (collectionExists(collectionName)) {
            VectorCollectionInspection inspection = inspectCollection(collectionName, profile);
            if (!inspection.sparseAvailable() || !inspection.exactIndexAvailable()) {
                throw new IllegalStateException("Qdrant 集合缺少 Sparse/Exact 索引，请创建新 Generation 重建知识索引。");
            }
            return;
        }
        client.put().uri("/collections/{collection}", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateCollectionRequest(new NamedVectorConfig(
                        new VectorConfig(profile.vectorDimension(), normalizeDistance(profile.distanceMetric()))),
                        new NamedSparseVectorConfig(new SparseVectorConfig(
                                new SparseIndexConfig(true), SPARSE_MODIFIER_IDF))))
                .retrieve().toBodilessEntity();
        createPayloadIndex(collectionName, PAYLOAD_TENANT_ID, PAYLOAD_INDEX_INTEGER);
        createPayloadIndex(collectionName, PAYLOAD_DOCUMENT_ID, PAYLOAD_INDEX_INTEGER);
        createPayloadIndex(collectionName, PAYLOAD_GENERATION_ID, PAYLOAD_INDEX_INTEGER);
        createPayloadIndex(collectionName, PAYLOAD_EXACT_KEYS, PAYLOAD_INDEX_KEYWORD);
        LOG.info("Qdrant 集合创建完成，collection={}, dimension={}, distance={}",
                collectionName, profile.vectorDimension(), profile.distanceMetric());
    }

        /**
         * inspectCollection 方法。
         *
         * @param collectionName collectionName 参数
         * @param profile profile 参数
         * @return VectorCollectionInspection 返回对象
         */
    @Override
    public VectorCollectionInspection inspectCollection(String collectionName, RagEmbeddingProfile profile) {
        try {
            JsonNode response = client.get().uri("/collections/{collection}", collectionName)
                    .retrieve().body(JsonNode.class);
            JsonNode result = response == null ? null : response.path("result");
            JsonNode vectors = result == null ? null : result.path("config").path("params").path("vectors");
            JsonNode dense = vectors == null ? null : vectors.path(DENSE_VECTOR);
            int dimension = dense == null ? 0 : dense.path("size").asInt(0);
            long count = result == null ? 0L : result.path("points_count").asLong(0L);
            JsonNode sparseVectors = result == null ? null
                    : result.path("config").path("params").path("sparse_vectors");
            boolean sparseAvailable = sparseVectors != null && sparseVectors.has(LEXICAL_SPARSE_VECTOR);
            boolean exactIndexAvailable = result != null
                    && result.path("payload_schema").has(PAYLOAD_EXACT_KEYS);
            return new VectorCollectionInspection(true, dimension, count,
                    sparseAvailable, exactIndexAvailable);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) return VectorCollectionInspection.unavailable();
            throw new IllegalStateException("检查 Qdrant 集合失败。", exception);
        }
    }

        /**
         * upsert 方法。
         *
         * @param collectionName collectionName 参数
         * @param points points 参数
         */
    @Override
    public void upsert(String collectionName, List<VectorIndexPoint> points) {
        if (points == null || points.isEmpty()) return;
        int batchSize = qdrantBatchSize();
        for (int start = 0; start < points.size(); start += batchSize) {
            List<VectorIndexPoint> batch = points.subList(start, Math.min(start + batchSize, points.size()));
            client.put().uri("/collections/{collection}/points?wait=true", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new UpsertPointsRequest(batch.stream().map(this::pointBody).toList()))
                    .retrieve().toBodilessEntity();
        }
        LOG.info("Qdrant 向量写入完成，collection={}, points={}", collectionName, points.size());
    }

        /**
         * 检索search 业务逻辑处理。
         *
         * @param request request 参数
         * @return List<VectorSearchCandidate> 返回对象
         */
    @Override
    public List<VectorSearchCandidate> search(VectorSearchRequest request) {
        SearchPointsRequest body = new SearchPointsRequest(request.queryVector(), DENSE_VECTOR,
                searchFilter(request), request.limit(), true);
        JsonNode response = client.post()
                .uri("/collections/{collection}/points/query", request.collectionName())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(JsonNode.class);
        return parseCandidates(response);
    }

        /**
         * 删除deleteDocument 业务逻辑处理。
         *
         * @param collectionName collectionName 参数
         * @param tenantId tenantId 参数
         * @param documentId documentId 参数
         */
    @Override
    public void deleteDocument(String collectionName, Long tenantId, Long documentId) {
        QdrantFilter filter = new QdrantFilter(List.of(
                matchValue(PAYLOAD_TENANT_ID, tenantId),
                matchValue(PAYLOAD_DOCUMENT_ID, documentId)));
        client.post().uri("/collections/{collection}/points/delete?wait=true", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeleteByFilterRequest(filter))
                .retrieve().toBodilessEntity();
    }

        /**
         * 删除deletePoints 业务逻辑处理。
         *
         * @param collectionName collectionName 参数
         * @param pointIds pointIds 参数
         */
    @Override
    public void deletePoints(String collectionName, List<String> pointIds) {
        if (pointIds == null || pointIds.isEmpty()) return;
        int batchSize = qdrantBatchSize();
        for (int start = 0; start < pointIds.size(); start += batchSize) {
            List<String> batch = pointIds.subList(start, Math.min(start + batchSize, pointIds.size()));
            client.post().uri("/collections/{collection}/points/delete?wait=true", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DeletePointsRequest(batch))
                    .retrieve().toBodilessEntity();
        }
    }

        /**
         * 删除deleteCollection 业务逻辑处理。
         *
         * @param collectionName collectionName 参数
         */
    @Override
    public void deleteCollection(String collectionName) {
        try {
            client.delete().uri("/collections/{collection}", collectionName).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw new IllegalStateException("删除 Qdrant 历史集合失败。", exception);
            }
        }
    }

    private boolean collectionExists(String collectionName) {
        try {
            client.get().uri("/collections/{collection}", collectionName).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) return false;
            throw new IllegalStateException("检查 Qdrant 集合失败，请确认向量服务可用。", exception);
        }
    }

    private QdrantPoint pointBody(VectorIndexPoint point) {
        QdrantPayload payload = new QdrantPayload(point.tenantId(), point.documentId(), point.chunkId(),
                point.indexGenerationId(), point.parentChunkId(),
                point.language() == null ? "OTHER" : point.language().name(), point.chunkNo(),
                point.sectionPath() == null ? "" : point.sectionPath(), point.exactKeys());
        return new QdrantPoint(point.pointId(), new NamedVectors(
                point.denseVector(), point.lexicalVector()), payload);
    }

    /**
     * 所有召回都必须同时过滤租户、索引版本和文档范围。
     * 缺少任一条件都可能把旧模型空间或其他租户的数据带入候选集，因此过滤在 Provider 内强制组装，
     * 上层协调器不能绕过该边界直接提交自由 JSON。
     */
    private QdrantFilter searchFilter(VectorSearchRequest request) {
        return searchFilter(request.tenantId(), request.indexGenerationId(), request.documentIds().stream().toList());
    }

    private QdrantFilter searchFilter(LexicalSearchRequest request) {
        return searchFilter(request.tenantId(), request.indexGenerationId(), request.documentIds().stream().toList());
    }

    private QdrantFilter searchFilter(Long tenantId, Long generationId, List<Long> documentIds) {
        List<FilterCondition> must = new ArrayList<>();
        must.add(matchValue(PAYLOAD_TENANT_ID, tenantId));
        must.add(matchValue(PAYLOAD_GENERATION_ID, generationId));
        if (!documentIds.isEmpty()) {
            must.add(matchAny(PAYLOAD_DOCUMENT_ID, documentIds));
        }
        return new QdrantFilter(List.copyOf(must));
    }

    private FilterCondition matchValue(String key, Object value) {
        return new FilterCondition(key, new MatchCondition(value, null));
    }

    private FilterCondition matchAny(String key, List<?> values) {
        return new FilterCondition(key, new MatchCondition(null, List.copyOf(values)));
    }

        /**
         * 检索searchSparse 业务逻辑处理。
         *
         * @param request request 参数
         * @return List<LexicalSearchCandidate> 返回对象
         */
    @Override
    public List<LexicalSearchCandidate> searchSparse(LexicalSearchRequest request) {
        SparseSearchPointsRequest body = new SparseSearchPointsRequest(request.queryVector(),
                LEXICAL_SPARSE_VECTOR, searchFilter(request), request.limit(), true);
        JsonNode response = client.post()
                .uri("/collections/{collection}/points/query", request.collectionName())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(JsonNode.class);
        return parseLexicalCandidates(response, false);
    }

        /**
         * 检索searchExact 业务逻辑处理。
         *
         * @param request request 参数
         * @return List<LexicalSearchCandidate> 返回对象
         */
    @Override
    public List<LexicalSearchCandidate> searchExact(LexicalSearchRequest request) {
        List<FilterCondition> must = new ArrayList<>(searchFilter(request).must());
        must.add(matchAny(PAYLOAD_EXACT_KEYS, request.exactKeys()));
        ScrollPointsRequest body = new ScrollPointsRequest(new QdrantFilter(List.copyOf(must)),
                request.limit(), true, false);
        JsonNode response = client.post()
                .uri("/collections/{collection}/points/scroll", request.collectionName())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(JsonNode.class);
        return parseLexicalCandidates(response, true);
    }

    private List<LexicalSearchCandidate> parseLexicalCandidates(JsonNode response, boolean exact) {
        if (response == null) return List.of();
        JsonNode result = response.path("result");
        JsonNode points = result.has("points") ? result.path("points") : result;
        if (!points.isArray()) return List.of();
        List<LexicalSearchCandidate> candidates = new ArrayList<>();
        for (JsonNode point : points) {
            JsonNode payload = point.path("payload");
            double sparseScore = exact ? 0D : point.path("score").asDouble(0D);
            candidates.add(new LexicalSearchCandidate(point.path("id").asText(),
                    longValue(payload, PAYLOAD_DOCUMENT_ID), longValue(payload, PAYLOAD_CHUNK_ID),
                    longValue(payload, PAYLOAD_PARENT_CHUNK_ID), integerValue(payload, PAYLOAD_CHUNK_NO),
                    payload.path(PAYLOAD_SECTION_PATH).asText(""), sparseScore,
                    exact ? 1D : 0D, exact ? RetrievalHitReason.EXACT_IDENTIFIER
                    : RetrievalHitReason.SPARSE_TERM_MATCH));
        }
        return List.copyOf(candidates);
    }

    private void createPayloadIndex(String collectionName, String fieldName, String type) {
        client.put().uri("/collections/{collection}/index?wait=true", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePayloadIndexRequest(fieldName, new PayloadIndexSchema(type, true)))
                .retrieve().toBodilessEntity();
    }

    private List<VectorSearchCandidate> parseCandidates(JsonNode response) {
        if (response == null) return List.of();
        JsonNode result = response.path("result");
        JsonNode points = result.has("points") ? result.path("points") : result;
        if (!points.isArray()) return List.of();
        List<VectorSearchCandidate> candidates = new ArrayList<>();
        for (JsonNode point : points) {
            JsonNode payload = point.path("payload");
            candidates.add(new VectorSearchCandidate(point.path("id").asText(),
                    longValue(payload, PAYLOAD_DOCUMENT_ID), longValue(payload, PAYLOAD_CHUNK_ID),
                    longValue(payload, PAYLOAD_PARENT_CHUNK_ID), integerValue(payload, PAYLOAD_CHUNK_NO),
                    payload.path(PAYLOAD_SECTION_PATH).asText(""), point.path("score").asDouble()));
        }
        return List.copyOf(candidates);
    }

    private Long longValue(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private Integer integerValue(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private String normalizeDistance(String distance) {
        if (distance == null || distance.isBlank()) return "Cosine";
        return switch (distance.toUpperCase()) {
            case "DOT" -> "Dot";
            case "EUCLID", "EUCLIDEAN" -> "Euclid";
            case "MANHATTAN" -> "Manhattan";
            default -> "Cosine";
        };
    }

    private int qdrantBatchSize() {
        return Math.max(1, Math.min(MAX_QDRANT_BATCH_SIZE, properties.getQdrant().getUpsertBatchSize()));
    }

    private record CreateCollectionRequest(NamedVectorConfig vectors,
                                           @JsonProperty("sparse_vectors") NamedSparseVectorConfig sparseVectors) {
    }

    private record NamedVectorConfig(@JsonProperty(DENSE_VECTOR) VectorConfig denseVector) {
    }

    private record VectorConfig(int size, String distance) {
    }

    private record NamedSparseVectorConfig(
            @JsonProperty(LEXICAL_SPARSE_VECTOR) SparseVectorConfig lexicalSparseVector) {
    }

    private record SparseVectorConfig(SparseIndexConfig index, String modifier) {
    }

    private record SparseIndexConfig(@JsonProperty("on_disk") boolean onDisk) {
    }

    private record UpsertPointsRequest(List<QdrantPoint> points) {
    }

    private record QdrantPoint(String id, NamedVectors vector, QdrantPayload payload) {
    }

    private record NamedVectors(@JsonProperty(DENSE_VECTOR) List<Float> denseVector,
                                @JsonProperty(LEXICAL_SPARSE_VECTOR) SparseVectorData lexicalSparseVector) {
    }

    private record QdrantPayload(
            @JsonProperty(PAYLOAD_TENANT_ID) Long tenantId,
            @JsonProperty(PAYLOAD_DOCUMENT_ID) Long documentId,
            @JsonProperty(PAYLOAD_CHUNK_ID) Long chunkId,
            @JsonProperty(PAYLOAD_GENERATION_ID) Long indexGenerationId,
            @JsonProperty(PAYLOAD_PARENT_CHUNK_ID) Long parentChunkId,
            @JsonProperty(PAYLOAD_LANGUAGE) String language,
            @JsonProperty(PAYLOAD_CHUNK_NO) Integer chunkNo,
            @JsonProperty(PAYLOAD_SECTION_PATH) String sectionPath,
            @JsonProperty(PAYLOAD_EXACT_KEYS) List<String> exactKeys
    ) {
    }

    private record SearchPointsRequest(
            List<Float> query,
            String using,
            QdrantFilter filter,
            int limit,
            @JsonProperty("with_payload") boolean withPayload
    ) {
    }

    private record SparseSearchPointsRequest(
            SparseVectorData query,
            String using,
            QdrantFilter filter,
            int limit,
            @JsonProperty("with_payload") boolean withPayload
    ) {
    }

    private record ScrollPointsRequest(
            QdrantFilter filter,
            int limit,
            @JsonProperty("with_payload") boolean withPayload,
            @JsonProperty("with_vector") boolean withVector
    ) {
    }

    private record QdrantFilter(List<FilterCondition> must) {
    }

    private record FilterCondition(String key, MatchCondition match) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record MatchCondition(Object value, List<?> any) {
    }

    private record DeleteByFilterRequest(QdrantFilter filter) {
    }

    private record DeletePointsRequest(List<String> points) {
    }

    private record CreatePayloadIndexRequest(@JsonProperty("field_name") String fieldName,
                                             @JsonProperty("field_schema") PayloadIndexSchema fieldSchema) {
    }

    private record PayloadIndexSchema(String type, @JsonProperty("on_disk") boolean onDisk) {
    }
}
