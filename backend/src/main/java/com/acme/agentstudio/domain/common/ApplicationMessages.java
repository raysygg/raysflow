package com.acme.agentstudio.domain.common;

/**
 * 平台通用业务异常、校验提示与用户友好提示文案集中定义类（Application Messages）。
 * 统一管理 Agent、Workflow、RAG 知识库、权限校验的中文提示，禁止零散写死中文字符串。
 */
public final class ApplicationMessages {

    /** 服务端通用错误前缀："服务端处理失败：" */
    public static final String SERVER_ERROR_PREFIX = "服务端处理失败：";

    /** 通用处理成功提示："处理成功" */
    public static final String SUCCESS = "处理成功";

    /** Agent 创建成功提示："Agent 已创建。" */
    public static final String AGENT_CREATED = "Agent 已创建。";

    /** 知识文档创建成功提示："知识文档已创建。" */
    public static final String KNOWLEDGE_DOCUMENT_CREATED = "知识文档已创建。";

    /** 知识文档索引完成提示："知识文档已完成索引。" */
    public static final String KNOWLEDGE_DOCUMENT_INDEXED = "知识文档已完成索引。";

    /** 工作流创建成功提示："工作流已创建。" */
    public static final String WORKFLOW_CREATED = "工作流已创建。";

    /** 工作流发布成功提示："工作流已发布。" */
    public static final String WORKFLOW_PUBLISHED = "工作流已发布。";

    /** 工作流停用成功提示："工作流已停用。" */
    public static final String WORKFLOW_DISABLED = "工作流已停用。";

    /** 租户 ID 必须传提示："租户 ID 不能为空。" */
    public static final String TENANT_ID_REQUIRED = "租户 ID 不能为空。";

    /** Agent ID 必须传提示："Agent ID 不能为空。" */
    public static final String AGENT_ID_REQUIRED = "Agent ID 不能为空。";

    /** 用户 ID 必须传提示："用户 ID 不能为空。" */
    public static final String USER_ID_REQUIRED = "用户 ID 不能为空。";

    /** 用户消息必须传提示："用户消息不能为空。" */
    public static final String MESSAGE_REQUIRED = "用户消息不能为空。";

    /** Agent 编码必须传提示："Agent 编码不能为空。" */
    public static final String AGENT_CODE_REQUIRED = "Agent 编码不能为空。";

    /** Agent 名称必须传提示："Agent 名称不能为空。" */
    public static final String AGENT_NAME_REQUIRED = "Agent 名称不能为空。";

    /** Agent 类型必须传提示："Agent 类型不能为空。" */
    public static final String AGENT_TYPE_REQUIRED = "Agent 类型不能为空。";

    /** Prompt 模板必须传提示："Agent Prompt 模板不能为空，必须通过数据库动态配置。" */
    public static final String AGENT_PROMPT_TEMPLATE_REQUIRED = "Agent Prompt 模板不能为空，必须通过数据库动态配置。";

    /** 模型编码必须传提示："Agent 模型编码不能为空，必须绑定模型中心中的有效配置。" */
    public static final String AGENT_MODEL_KEY_REQUIRED = "Agent 模型编码不能为空，必须绑定模型中心中的有效配置。";

    /** 找不到 Agent 前缀："未找到 Agent：" */
    public static final String AGENT_NOT_FOUND_PREFIX = "未找到 Agent：";

    /** 租户不匹配提示："Agent 不属于当前租户。" */
    public static final String AGENT_TENANT_MISMATCH = "Agent 不属于当前租户。";

    /** 工作流编码必须传提示："工作流编码不能为空。" */
    public static final String WORKFLOW_CODE_REQUIRED = "工作流编码不能为空。";

    /** 画布 JSON 必须传提示："工作流画布 JSON 不能为空。" */
    public static final String WORKFLOW_GRAPH_REQUIRED = "工作流画布 JSON 不能为空。";

    /** 找不到工作流前缀："未找到工作流：" */
    public static final String WORKFLOW_NOT_FOUND_PREFIX = "未找到工作流：";

    /** 发布版禁止编辑提示："已发布工作流不能直接编辑，请先停用后再创建新的草稿版本。" */
    public static final String WORKFLOW_PUBLISHED_EDIT_DENIED = "已发布工作流不能直接编辑，请先停用后再创建新的草稿版本。";

    /** 工作流操作人必须传提示："工作流操作人不能为空。" */
    public static final String WORKFLOW_OPERATOR_REQUIRED = "工作流操作人不能为空。";

    /** 工作流租户不匹配提示："工作流不属于当前租户。" */
    public static final String WORKFLOW_TENANT_MISMATCH = "工作流不属于当前租户。";

    /** 画布 JSON 非法提示："工作流画布 JSON 格式不合法。" */
    public static final String WORKFLOW_GRAPH_JSON_INVALID = "工作流画布 JSON 格式不合法。";

    /** 画布必须为对象提示："工作流画布必须是 JSON 对象。" */
    public static final String WORKFLOW_GRAPH_OBJECT_REQUIRED = "工作流画布必须是 JSON 对象。";

    /** 节点数组必须传提示："工作流画布必须包含非空 nodes 数组。" */
    public static final String WORKFLOW_GRAPH_NODES_REQUIRED = "工作流画布必须包含非空 nodes 数组。";

    /** 连线数组必须传提示："多个节点的工作流必须包含非空 edges 数组。" */
    public static final String WORKFLOW_GRAPH_EDGES_REQUIRED = "多个节点的工作流必须包含非空 edges 数组。";

    /** 节点 ID 必须传提示："工作流节点 ID 不能为空。" */
    public static final String WORKFLOW_GRAPH_NODE_ID_REQUIRED = "工作流节点 ID 不能为空。";

    /** 节点类型必须传提示："工作流节点类型不能为空。" */
    public static final String WORKFLOW_GRAPH_NODE_TYPE_REQUIRED = "工作流节点类型不能为空。";

    /** 连线来源节点必须传提示："工作流连线来源节点不能为空。" */
    public static final String WORKFLOW_GRAPH_EDGE_SOURCE_REQUIRED = "工作流连线来源节点不能为空。";

    /** 连线目标节点必须传提示："工作流连线目标节点不能为空。" */
    public static final String WORKFLOW_GRAPH_EDGE_TARGET_REQUIRED = "工作流连线目标节点不能为空。";

    /** 重复节点前缀："工作流节点 ID 重复：" */
    public static final String WORKFLOW_GRAPH_DUPLICATE_NODE_PREFIX = "工作流节点 ID 重复：";

    /** 起终点校验提示："工作流必须且只能包含一个 START 节点和一个 END 节点。" */
    public static final String WORKFLOW_GRAPH_START_END_REQUIRED = "工作流必须且只能包含一个 START 节点和一个 END 节点。";

    /** 连线引用无效节点提示："工作流连线引用了不存在的节点。" */
    public static final String WORKFLOW_GRAPH_EDGE_NODE_MISSING = "工作流连线引用了不存在的节点。";

    /** 禁止自环连线提示："工作流连线不能指向自身。" */
    public static final String WORKFLOW_GRAPH_SELF_LOOP_DENIED = "工作流连线不能指向自身。";

    /** START 禁止有入边提示："START 节点不能存在入边。" */
    public static final String WORKFLOW_GRAPH_START_INCOMING_DENIED = "START 节点不能存在入边。";

    /** END 禁止有出边提示："END 节点不能存在出边。" */
    public static final String WORKFLOW_GRAPH_END_OUTGOING_DENIED = "END 节点不能存在出边。";

    /** 不可达节点前缀："工作流节点缺少入边，无法从 START 到达：" */
    public static final String WORKFLOW_GRAPH_UNREACHABLE_NODE_PREFIX = "工作流节点缺少入边，无法从 START 到达：";

    /** 死胡同节点前缀："工作流节点缺少出边，无法流转到 END：" */
    public static final String WORKFLOW_GRAPH_DEAD_END_NODE_PREFIX = "工作流节点缺少出边，无法流转到 END：";

    /** START 无法到达前缀："工作流节点无法从 START 到达：" */
    public static final String WORKFLOW_GRAPH_NOT_REACHABLE_FROM_START_PREFIX = "工作流节点无法从 START 到达：";

    /** 无法流转到 END 前缀："工作流节点无法流转到 END：" */
    public static final String WORKFLOW_GRAPH_NOT_REACHABLE_TO_END_PREFIX = "工作流节点无法流转到 END：";

    /** 审计序列化失败提示："审计详情序列化失败。" */
    public static final String AUDIT_DETAIL_SERIALIZE_FAILED = "审计详情序列化失败。";

    /** 知识文档标题必须传提示："知识文档标题不能为空。" */
    public static final String KNOWLEDGE_TITLE_REQUIRED = "知识文档标题不能为空。";

    /** 知识文件必须传提示："知识文件不能为空。" */
    public static final String KNOWLEDGE_FILE_REQUIRED = "知识文件不能为空。";

    /** 待向量化文本必须传提示："待向量化文本不能为空。" */
    public static final String KNOWLEDGE_TEXT_REQUIRED = "待向量化文本不能为空。";

    /** 检索问题必须传提示："检索问题不能为空。" */
    public static final String KNOWLEDGE_QUERY_REQUIRED = "检索问题不能为空。";

    /** 文档保存失败提示："保存上传知识文档失败。" */
    public static final String UPLOADED_DOCUMENT_SAVE_FAILED = "保存上传知识文档失败。";

    /** 索引生成失败提示："知识文档索引失败。" */
    public static final String KNOWLEDGE_INDEX_FAILED = "知识文档索引失败。";

    /** 链路序列化失败提示："会话调试链路序列化失败。" */
    public static final String CHAT_TRACE_SERIALIZE_FAILED = "会话调试链路序列化失败。";

    /** RAG 无命中友好回复："当前知识库未检索到可支撑该问题的内容..." */
    public static final String RAG_NO_HIT_REPLY = "当前知识库未检索到可支撑该问题的内容，无法基于企业知识给出可靠回答。请补充相关文档后重试，或改用人工处理。";

    /** RAG 信息不足友好拒答："检索到的知识片段无法充分支撑本次回答..." */
    public static final String RAG_UNSUPPORTED_REPLY = "检索到的知识片段无法充分支撑本次回答，为避免编造信息，系统已拒答。请完善知识库或改写问题后重试。";

    /** 私有构造函数，防止工具类被实例化 */
    private ApplicationMessages() {
    }
}


