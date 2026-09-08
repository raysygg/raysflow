package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.application.knowledge.KnowledgeApplicationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.ApplicationMessages;
import com.acme.agentstudio.interfaces.rest.dto.CreateKnowledgeDocumentRequest;
import com.acme.agentstudio.interfaces.rest.dto.CreateKnowledgeTagRequest;
import com.acme.agentstudio.interfaces.rest.dto.EmbeddingPreviewRequest;
import com.acme.agentstudio.interfaces.rest.dto.KnowledgeSearchRequest;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeDocumentPageQuery;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingModelOption;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;

/**
 * 知识库管理 REST 控制器。
 * 负责提供知识库文档的增删改查、文本/文件切片向量化上传、索引重建、知识标签挂载以及 RAG 检索测试接口。
 */
@Tag(name = "知识库管理", description = "知识库文档、索引和检索资源管理")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    /** 知识库应用层核心服务 */
    private final KnowledgeApplicationService knowledgeApplicationService;

    /**
     * 构造函数注入知识库应用服务。
     *
     * @param knowledgeApplicationService 知识库服务
     */
    public KnowledgeController(KnowledgeApplicationService knowledgeApplicationService) {
        this.knowledgeApplicationService = knowledgeApplicationService;
    }

    /**
     * 分页查询当前租户内的知识文档资产。
     *
     * @param user 当前登录用户
     * @param page 请求页码（默认第 1 页）
     * @param size 每页大小（默认 20 条）
     * @param keyword 标题搜索关键字（可选）
     * @param language 目标自然语言类型（可选）
     * @param status 索引状态筛选（可选）
     * @return 分页知识文档列表数据
     */
    @Operation(summary = "分页获取知识文档", description = "按租户和权限范围分页读取知识资产，并返回当前索引状态摘要。单页最多 100 条。")
    @GetMapping("/documents")
    public ApiResponse<?> listDocuments(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) KnowledgeLanguage language,
            @RequestParam(required = false) String status
    ) {
        KnowledgeDocumentPageQuery query = new KnowledgeDocumentPageQuery(page, size, keyword, language, status);
        return ApiResponse.ok(knowledgeApplicationService.pageDocuments(
                user.getTenantId(), user.getUserId(), user.getRole(), query));
    }

    /**
     * 读取特定知识文档的实时索引状态与元数据。
     *
     * @param user 当前登录用户
     * @param id 目标知识文档 ID
     * @return 包含索引版本、模型 Profile 的详细状态对象
     */
    @Operation(summary = "获取文档索引详情", description = "读取文档当前索引版本、模型 Profile 和最近失败原因，不返回 Qdrant 凭证或模型连接地址。")
    @GetMapping("/documents/{id}/index-state")
    public ApiResponse<?> documentIndexState(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        return ApiResponse.ok(knowledgeApplicationService.documentIndexDetail(user.getTenantId(), id));
    }

    /**
     * 批量读取知识文档的摘要选项，主要用于前端远程下拉框选择。
     *
     * @param user 当前登录用户
     * @param ids 目标知识文档 ID 集合
     * @return 选中的文档基础属性列表
     */
    @Operation(summary = "批量读取文档选项", description = "用于远程文档选择器回显已选项，最多接收 100 个文档标识。")
    @GetMapping("/documents/options")
    public ApiResponse<?> documentOptions(@AuthenticationPrincipal SecurityUser user,
                                          @RequestParam List<Long> ids) {
        return ApiResponse.ok(knowledgeApplicationService.documentOptions(user.getTenantId(), ids));
    }

    /**
     * 获取租户当前生效的向量索引代别与底座配置。
     *
     * @param user 当前登录用户
     * @param modelSource 指定模型来源（可选）
     * @param modelId 指定模型 ID（可选）
     * @param modelKey 指定模型唯一 Key（可选）
     * @return 索引代别与容量摘要数据
     */
    @Operation(summary = "获取当前索引版本", description = "读取当前租户生效的 Embedding Profile、Qdrant 索引版本和容量摘要。")
    @GetMapping("/index-generations/current")
    public ApiResponse<?> currentIndexGeneration(@AuthenticationPrincipal SecurityUser user,
                                                 @RequestParam(required = false) RagModelSource modelSource,
                                                 @RequestParam(required = false) Long modelId,
                                                 @RequestParam(required = false) String modelKey) {
        return ApiResponse.ok(knowledgeApplicationService.currentIndexGeneration(user.getTenantId(),
                selection(modelSource, modelId, modelKey)));
    }

    /**
     * 查询租户可选的 Embeddings 向量模型列表。
     *
     * @param user 当前登录用户
     * @return 包含私有、共享与本地向量模型的选项列表
     */
    @Operation(summary = "获取 Embedding 模型选项", description = "返回租户私有、平台共享和本地兜底模型，不返回接口地址或凭证。")
    @GetMapping("/embedding-models")
    public ApiResponse<List<RagEmbeddingModelOption>> embeddingModels(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(knowledgeApplicationService.embeddingModels(user.getTenantId()));
    }

    /**
     * 异步提交全量知识库索引重建任务。
     *
     * @param user 当前登录用户
     * @param modelSource 向量模型来源
     * @param modelId 向量模型 ID
     * @param modelKey 向量模型 Key
     * @return 任务提交成功状态
     */
    @Operation(summary = "提交批量重建", description = "只提交后台协调任务，由 Worker 使用主键游标分页拆分文档重建任务，HTTP 请求不直接调用模型或 Qdrant。")
    @PostMapping("/index-generations/rebuild")
    public ApiResponse<?> rebuildIndexGeneration(@AuthenticationPrincipal SecurityUser user,
                                                 @RequestParam(required = false) RagModelSource modelSource,
                                                 @RequestParam(required = false) Long modelId,
                                                 @RequestParam(required = false) String modelKey) {
        return ApiResponse.ok("批量重建任务已提交。",
                knowledgeApplicationService.submitBatchReindex(user.getTenantId(),
                        selection(modelSource, modelId, modelKey)));
    }

    /**
     * 创建纯文本或线上地址类的知识库文档。
     *
     * @param user 当前登录用户
     * @param request 文档创建请求体
     * @return 创建结果
     */
    @Operation(summary = "创建知识文档", description = "按当前登录用户的租户范围创建知识库文档条目。")
    @PostMapping("/documents")
    public ApiResponse<?> createDocument(@AuthenticationPrincipal SecurityUser user, @RequestBody CreateKnowledgeDocumentRequest request) {
        return ApiResponse.ok(ApplicationMessages.KNOWLEDGE_DOCUMENT_CREATED, knowledgeApplicationService.createDocument(user.getTenantId(), request));
    }

    /**
     * 上传知识库文件并异步触发切片与向量化。
     *
     * @param user 当前登录用户
     * @param file 上传的文件对象
     * @param language 自然语言
     * @param modelSource 指定向量模型来源
     * @param modelId 指定模型 ID
     * @param modelKey 指定模型 Key
     * @return 上传排队响应
     */
    @Operation(summary = "上传知识文档", description = "保存原始文件并提交持久化索引任务，解析、Embedding 和 Qdrant 写入不占用 HTTP 请求线程。")
    @PostMapping("/documents/upload")
    public ApiResponse<?> uploadAndIndex(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) KnowledgeLanguage language,
            @RequestParam(value = "modelSource", required = false) RagModelSource modelSource,
            @RequestParam(value = "modelId", required = false) Long modelId,
            @RequestParam(value = "modelKey", required = false) String modelKey
    ) {
        return ApiResponse.ok("知识文档已上传并进入索引队列。",
                knowledgeApplicationService.uploadAndIndex(user.getTenantId(), file, language,
                        selection(modelSource, modelId, modelKey)));
    }

    /**
     * 根据 ID 删除指定的知识库文档。
     *
     * @param user 当前登录用户
     * @param id 目标文档 ID
     * @return 操作成功响应
     */
    @Operation(summary = "删除知识文档", description = "按文档 ID 删除对应的知识库文档。")
    @DeleteMapping("/documents/{id}")
    public ApiResponse<?> deleteDocument(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        knowledgeApplicationService.deleteDocument(user.getTenantId(), id);
        return ApiResponse.ok("知识文档已删除。", null);
    }

    /**
     * 同步重新索引单篇知识文档。
     *
     * @param user 当前登录用户
     * @param id 文档 ID
     * @param modelSource 模型来源
     * @param modelId 模型 ID
     * @param modelKey 模型 Key
     * @return 重索引结果
     */
    @Operation(summary = "重新索引知识文档", description = "对指定知识库文档重新执行切片与索引操作。")
    @PostMapping("/documents/{id}/reindex")
    public ApiResponse<?> reindexDocument(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                          @RequestParam(required = false) RagModelSource modelSource,
                                          @RequestParam(required = false) Long modelId,
                                          @RequestParam(required = false) String modelKey) {
        return ApiResponse.ok("知识文档已重新索引。", knowledgeApplicationService.reindexDocument(user.getTenantId(), id,
                selection(modelSource, modelId, modelKey)));
    }

    /**
     * 异步提交单篇文档的重索引请求。
     *
     * @param user 当前登录用户
     * @param id 文档 ID
     * @param modelSource 模型来源
     * @param modelId 模型 ID
     * @param modelKey 模型 Key
     * @return 操作成功结果
     */
    @Operation(summary = "异步提交重索引任务", description = "将知识库文档重新索引任务加入异步处理队列。")
    @PostMapping("/documents/{id}/reindex-async")
    public ApiResponse<?> enqueueReindex(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                         @RequestParam(required = false) RagModelSource modelSource,
                                         @RequestParam(required = false) Long modelId,
                                         @RequestParam(required = false) String modelKey) {
        knowledgeApplicationService.enqueueReindex(user.getTenantId(), id, selection(modelSource, modelId, modelKey));
        return ApiResponse.ok("知识文档已进入重索引队列。", null);
    }

    /**
     * 查询指定知识文档历史索引任务的执行记录。
     *
     * @param user 当前登录用户
     * @param id 文档 ID
     * @return 索引任务记录列表
     */
    @Operation(summary = "获取文档索引任务状态", description = "查询指定知识文档的异步索引任务执行进度。")
    @GetMapping("/documents/{id}/index-tasks")
    public ApiResponse<?> indexTasks(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        return ApiResponse.ok(knowledgeApplicationService.listIndexTasks(user.getTenantId(), id));
    }

    /**
     * 获取租户空间下的知识分类与属性标签。
     *
     * @param user 当前登录用户
     * @return 知识标签集合
     */
    @Operation(summary = "获取知识标签列表", description = "获取当前租户及用户权限范围内的知识标签。")
    @GetMapping("/tags")
    public ApiResponse<?> tags(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(knowledgeApplicationService.listTags(user.getTenantId(), user.getUserId(), user.getRole()));
    }

    /**
     * 新建知识标签。
     *
     * @param user 当前登录用户
     * @param request 包含标签名与颜色编码的请求体
     * @return 新建标签结果
     */
    @Operation(summary = "创建知识标签", description = "新建知识库分类或属性标签。")
    @PostMapping("/tags")
    public ApiResponse<?> createTag(@AuthenticationPrincipal SecurityUser user,
                                    @RequestBody CreateKnowledgeTagRequest request) {
        return ApiResponse.ok("知识标签已创建。", knowledgeApplicationService.createTag(
                user.getTenantId(), request.tagName(), request.tagColor()));
    }

    /**
     * 为知识文档绑定分类标签。
     *
     * @param user 当前登录用户
     * @param documentId 目标文档 ID
     * @param tagId 目标标签 ID
     * @return 绑定成功结果
     */
    @Operation(summary = "绑定文档与标签", description = "为指定的知识库文档关联标签。")
    @PostMapping("/documents/{documentId}/tags/{tagId}")
    public ApiResponse<?> bindTag(@AuthenticationPrincipal SecurityUser user, @PathVariable Long documentId, @PathVariable Long tagId) {
        knowledgeApplicationService.bindTag(user.getTenantId(), documentId, tagId, user.getUserId(), user.getRole());
        return ApiResponse.ok("知识文档标签已绑定。", null);
    }

    /**
     * 预览特定文本在当前分词与 Embeddings 算法下的切片效果。
     *
     * @param user 当前登录用户
     * @param request 包含待预览文本的请求体
     * @return 文本切片预览集合
     */
    @Operation(summary = "预览文本向量化拆分", description = "对输入文本进行切片与 Embedding 向量化效果预览。")
    @PostMapping("/embedding-preview")
    public ApiResponse<?> embeddingPreview(@AuthenticationPrincipal SecurityUser user,
                                           @RequestBody EmbeddingPreviewRequest request) {
        return ApiResponse.ok(knowledgeApplicationService.previewEmbeddingFlow(user.getTenantId(), request.text()));
    }

    /**
     * 执行全功能 RAG 向量检索与混合重排测试。
     *
     * @param user 当前登录用户
     * @param request 检索调试请求对象
     * @return 召回的知识切片及关联文档片段
     */
    @Operation(summary = "检索知识库内容", description = "使用当前 RAG Profile 执行多语言稠密召回、重排和父级上下文还原。")
    @PostMapping("/search")
    public ApiResponse<?> search(@AuthenticationPrincipal SecurityUser user, @RequestBody KnowledgeSearchRequest request) {
        return ApiResponse.ok(knowledgeApplicationService.search(user.getTenantId(), request.query(), request.queryLanguage(), request.modelSelection()));
    }

    /** 辅助构造模型选择组合结构 */
    private RagModelSelection selection(RagModelSource source, Long modelId, String modelKey) {
        return source == null ? null : new RagModelSelection(source, modelId, modelKey);
    }
}

