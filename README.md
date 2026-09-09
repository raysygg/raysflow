# Enterprise Agent Studio · 光流智能引擎 (Raysflow OS)

Enterprise Agent Studio 是一个面向企业复杂业务场景的 Agent 操作系统、知识库检索增强（RAG）和可视化工作流编排平台。

系统秉持“**生产数据严密隔离、版本快照不可变、执行过程强可观测、业务事实真实落库**”的工程原则：
- **统一应用生命周期**：从草稿演进、候选版本固化、自动化质量评测、准入门禁判定，到不可变 Release 版本发布与生产指针秒级回滚。
- **混合 RAG 与知识运营**：采用 Parent-Child 双层切块架构，基于 Qdrant 高性能向量索引与多语言词法检索加权混合召回，支持防幻觉空召回拒答与引用源溯源。
- **多渠道应用入口**：统一发布 Chat 对话、表单交互、OpenAPI、Webhook 回调与 Cron 定时调度等多渠道入口，所有生产调用全量锚定活动 Release。
- **原生 Agent 运行时**：具备任务执行租约、心跳机制、分步节点事件追踪、人工审批挂起恢复、幂等去重与故障恢复重放能力。
- **SaaS 商业化与数据治理**：提供多租户隔离、细粒度组织与 IAM 授权、套餐权益配额准入、用量成本核算、数据保留策略与 Legal Hold 法律冻结。

---

## 1. 核心能力总览

### 1.1 统一业务应用生命周期 (Application Lifecycle)
- **草稿与候选版本**：应用配置与画布流程支持反复编辑草稿；支持一键固化为不可变的候选版本（Release Candidate），并提供拓扑结构 Diff 比对。
- **自动化评测与基线对比**：针对候选版本关联版本化测试集（Evaluation Suite），支持多样本并发评测，评测指标包含任务成功率、Groundedness 引用忠实度、平均耗时与模型成本。
- **质量门禁卡点 (Release Gate)**：系统基于评测证据自动比对准入阈值；支持授权管理员在合规前提下执行人工审批豁免（Override）。
- **不可变版本与秒级回滚**：通过门禁后生成全局唯一的不可变正式版本（Release）；生产指针（`FOLLOW_PRODUCTION`）支持秒级一键回滚，历史版本永久归档不可篡改。

### 1.2 可视化工作流编排 (Workflow Studio)
工作流节点完全由数据库节点目录（`orchestration_node_type`）动态驱动，支持丰富的企业级编排能力：
- **基础流程控制**：开始（START）、结束（END）、条件分支（CONDITION）、并行分支（PARALLEL）、并行聚合（JOIN）、循环（LOOP）、迭代子图（ITERATION）、智能路由（ROUTER）。
- **会话与提示词**：用户输入（USER_INPUT）、直接回显（DIRECT_REPLY）、提示词模板（PROMPT_TEMPLATE）、上下文组装（CONTEXT_BUILDER）、会话记忆窗口（SESSION_MEMORY）、意图分类器（QUESTION_CLASSIFIER）、参数提取器（PARAMETER_EXTRACTOR）。
- **模型与知识**：大语言模型生成（LLM，支持主备模型、Prompt 编排、结构化 JSON 输出与重试策略）、知识检索（RAG，支持混合检索、语言策略与文档范围过滤）。
- **Agent 与协作**：单个 Agent 调用（AGENT）、多角色协作团队（AGENT_TEAM）、图编排运行时（GRAPH_ORCHESTRATOR，对接 LangGraph / AutoGen / Crew 拓扑）。
- **数据处理与转换**：数据转换（TRANSFORM）、Jinja2 模板渲染（TEMPLATE_TRANSFORM）、变量聚合（VARIABLE_AGGREGATOR）、变量赋值（VARIABLE_ASSIGNMENT）、列表算子（LIST_OPERATOR）、文档解析提取（DOCUMENT_EXTRACTOR）。
- **外部集成与连接器**：HTTP 请求（HTTP_REQUEST）、OpenAPI 规范工具（OPENAPI）、MCP 服务器协议（MCP）、Webhook 回调（WEBHOOK）、租户内部 API（INTERNAL_API）、网页抓取（WEB_CRAWLER）、代码执行沙箱（CODE）。
- **人在回路 (Human-in-the-loop)**：人工审批节点（HUMAN），支持指定审批团队、风险级别，执行时挂起等待人工决策。

### 1.3 企业级知识库与多语言混合 RAG
- **双层切块架构**：支持 Parent-Child 父子切块，以精细子块实现高准确度召回，以完整父块提供丰富上下文。
- **Qdrant 向量索引**：使用 Qdrant 高性能向量数据库存储切块向量，支持元数据过滤、租户级集合隔离与多实例部署。
- **多语言混合检索**：融合向量相似度召回（Vector Recall）、词法稀疏检索（Lexical Search）与精确匹配提权（Exact Recall Boost），动态加权打分。
- **重排序与忠实度校验**：支持 Reranker 二次重排；当召回相似度不足时主动触发空命中拒答，并对模型回答进行 Groundedness 引用真实度核验，杜绝幻觉。
- **知识快照与生命周期**：索引生成任务（Generation）版本化管理，支持多版本快照、无缝热切换与过期索引按策略自动归档清理。

### 1.4 多渠道应用入口 (Multi-Channel Entrypoints)
- **应用工作台 (Chat Console)**：支持流式 SSE 与 WebSocket 双向会话、富文本 Markdown / 安全 HTML 实时渲染、思考过程展开与引用源精准溯源。
- **表单填报入口 (Form)**：基于 JSON Schema 自动渲染动态表单，支持立即执行与异步轮询。
- **开放 API (OpenAPI REST)**：支持第三方系统基于 API Key / Bearer 鉴权调用应用，提供同步响应与异步任务模式。
- **Webhook 事件接收**：支持 HMAC 签名防伪造校验，便于与企业外部系统或自建系统快速打通。
- **定时调度任务 (Schedule)**：支持按 Cron 表达式自动周期触发。
- **版本锚定策略**：所有外部入口支持固定跟随生产指针或锚定特定版本号，杜绝草稿未发布内容意外上线。

### 1.5 原生 Agent 运行时与运维容灾 (Native Runtime)
- **任务状态机与租约**：任务执行过程支持租约获取（Lease）、状态心跳保活、超时自动释放与故障检测。
- **链路级全量追踪**：节点输入、输出、错误堆栈、执行耗时与重试次数落库持久化，支持在“运行中心”展开全流程甘特图与调用拓扑。
- **故障处理与任务重放**：支持任务暂停、恢复、取消；在“故障处理中心”可针对失败节点或超时任务一键重试与重放。
- **人工审批中心**：专属任务中心承载挂起的人工审批，展示审批材料与决策上下文，审批通过后流程自动恢复执行。

### 1.6 模型中心与价格核算
- **平台共享与租户模型**：超级管理员配置平台共享模型，普通租户可补充配置私有模型；凭证加密存储，租户侧脱敏使用。
- **多协议适配与上游映射**：兼容 OpenAI 标准协议、Anthropic 协议及本地嵌入模型；支持配置上游真实模型名映射，解耦内部标识与上游透传名。
- **模型连接诊断**：提供连通性与能力在线诊断，实时验证网络连通性与 Key 有效性。
- **模型单价与费用账本**：支持针对输入/输出 Token 分别配置阶梯计费规则，实时统计每次调用的 Token 消耗与核算成本。

### 1.7 SaaS 商业治理与企业数据合规
- **企业身份治理 (IAM)**：支持组织架构、部门、团队与成员管理，支持直属上级防循环绑定；支持 OIDC、SAML、SCIM 协议及双因子认证（MFA）。
- **细粒度数据范围与审批范围**：角色支持按部门、本级或全部组织约束数据范围与审批权限；支持设备会话管理与异常强退。
- **套餐、权益与用量账本**：支持按版本管理商业套餐，按 Model Token、Workflow Run、知识库存储等维度设置软阈值与硬阈值；提供账期汇总与月度账单。
- **数据合规治理**：支持数据保留策略（Retention Policy）、数据脱敏导出、删除审批流程与 Legal Hold 法律冻结，支持跨 MySQL、Redis、Qdrant 与本地存储的协同清理。

---

## 2. 技术架构

| 层次 | 选型与组件 | 核心用途与说明 |
| --- | --- | --- |
| **前端框架** | Vue 3 + Vite 6 + Vue Router 4 | 现代化响应式单页面架构，前后端分离 |
| **前端组件库** | Element Plus | 紧凑型企业级 SaaS UI 组件体系与全中文交互 |
| **后端框架** | Spring Boot 3.3.1 + Java 17 | 核心应用服务端，提供 REST API、WebSocket 与任务调度 |
| **安全与认证** | Spring Security + JWT + HttpOnly Cookie | 访问令牌 + 刷新令牌机制、MFA、数据权限拦截 |
| **ORM 与持久化** | MyBatis-Plus 3.5.7 + MySQL 8.0+ | 强类型 Lambda 表达式，租户隔离，业务事实不可篡改存储 |
| **缓存与实时能力** | Redis 6+ | 分布式限流、执行租约与心跳、临时 Ticket、会话缓存 |
| **向量检索数据库** | Qdrant 1.7+ | 知识库切块向量存储、相似度高效检索与多租户集合隔离 |
| **大模型框架** | LangChain4j 0.33.0 | OpenAI 兼容接口、Anthropic 适配、BGE/MiniLM 文本嵌入 |
| **文档解析与提取** | Apache Tika (LangChain4j Document Parser) | 支持 PDF、DOCX、TXT、MD 等多格式企业文档解析 |
| **API 文档与规范** | Springdoc OpenAPI 3.0 / Swagger UI | `/swagger-ui.html` 交互式接口文档与测试 |

---

## 3. 环境准备

- **JDK**: 17+
- **Maven**: 3.9+
- **Node.js**: 18+ (推荐 Node 20+)
- **MySQL**: 8.0+ (字符集务必使用 `utf8mb4`)
- **Redis**: 6.0+
- **Qdrant**: 1.7+ (企业级向量数据库，可通过 Docker 一键启动)
- **PowerShell**: 5.1+ (用于执行 Windows 环境下的静态校验脚本)

---

## 4. 快速初始化与配置

### 4.1 启动依赖中间件

在本地或测试服务器确保 MySQL、Redis 和 Qdrant 正常运行。以 Docker 为例启动 Qdrant：

```bash
docker run -d --name qdrant -p 6333:6333 -p 6334:6334 -v $(pwd)/data/qdrant:/qdrant/storage qdrant/qdrant
```

### 4.2 创建数据库并初始化

创建数据库（注意指定 utf8mb4 字符集）：

```sql
CREATE DATABASE enterprise_agent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

针对初始化脚本，项目提供两种初始化方式：

#### 方式一：一键导入基线结构与初始数据（推荐快速上手）
项目根目录下的 `enterprise_agent.sql` 包含了当前系统的完整表结构、节点目录、初始菜单、系统租户与超管账户：

```bash
mysql -h localhost -P 3306 -u root -p enterprise_agent < enterprise_agent.sql
```

若有最新补丁脚本（如 `backend/sql/044-20260907-model-upstream-name.sql`），在导入基线后增量执行即可。

#### 方式二：按顺序执行迁移与种子脚本
如需从纯净脚本逐步构建，请遵循 `backend/sql/SQL-执行顺序.md` 规范：
1. 结构与迁移脚本：执行 `000-schema-base.sql` 至 `036-saas-governance-commercial-closure.sql`。
2. 基础数据脚本：执行 `000-data-base.sql` 与 `018-data-reconciliation.sql`。
3. 清理与标准种子：执行 `037-development-data-clear.sql`，再执行 `038-development-clean-seed.sql`（写入租户、角色、菜单、超管）与 `039-development-node-catalog-seed.sql`（写入完整节点库）。
4. （可选）冒烟用例：执行 `040-development-smoke-seed.sql`（写入基础回显测试应用 dev-smoke-app）。
5. 执行最新增量补丁 `043` 与 `044`。

#### 默认初始化账号
- **所属租户**：系统租户（租户编码：`system`，租户 ID：`1`）
- **管理员账号**：`admin`
- **初始密码**：`admin123`
- *注：初始密码仅用于本地开发测试，生产环境首次登录后务必及时修改！*

### 4.3 配置环境变量

复制根目录下的 `.env.example` 为 `.env`，根据实际环境调整配置参数：

```dotenv
# MySQL 数据库连接
DB_URL=jdbc:mysql://localhost:3306/enterprise_agent?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# 生产环境务必替换为高强度随机密钥，两者建议不同
JWT_SECRET=replace-with-a-very-long-random-secret-key-32chars
APP_SECRET_KEY=replace-with-a-different-long-secret-key-32chars

# Redis 缓存与分布式限流
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# Qdrant 向量数据库连接
QDRANT_BASE_URL=http://127.0.0.1:6333
QDRANT_API_KEY=

# 文件上传与任务队列
UPLOAD_DIR=data/uploads
TASK_QUEUE_POLL_DELAY_MS=1000
TASK_QUEUE_HEARTBEAT_TIMEOUT_SECONDS=120
TASK_QUEUE_RETRY_DELAY_SECONDS=30
```

---

## 5. 项目启动与访问

### 5.1 启动后端服务

在项目根目录下进入 `backend` 目录，通过 Maven 启动服务：

```powershell
cd backend
mvn spring-boot:run
```

- 后端服务默认端口：`8090`
- 健康检查端点：`http://localhost:8090/actuator/health`
- Swagger UI 接口文档：`http://localhost:8090/swagger-ui.html`
- OpenAPI 规范 JSON：`http://localhost:8090/v3/api-docs`

### 5.2 启动前端工程

另起一个终端，进入 `frontend` 目录安装依赖并启动开发服务器：

```powershell
cd frontend
npm install
npm run dev
```

- 前端服务默认地址：`http://localhost:5176`
- 开发模式下，Vite 自动将 `/api` 与 `/ws` 反向代理到后端 `http://localhost:8090`。

### 5.3 生产编译与打包

```powershell
# 前端静态资源构建（产物位于 frontend/dist）
cd frontend
npm run build

# 后端可执行 Jar 包构建（产物位于 backend/target）
cd backend
mvn clean package -DskipTests
```

---

## 6. 端到端典型使用流程

```mermaid
flowchart LR
    A[1.模型配置] --> B[2.知识库切块入库]
    B --> C[3.业务应用与工作流编排]
    C --> D[4.候选版本固化]
    D --> E[5.自动化评测与门禁比对]
    E --> F[6.不可变版本发布]
    F --> G[7.多渠道入口交付使用]
    G --> H[8.运行观测与故障重放]
```

1. **配置模型底座**：登录后进入“模型中心”，配置平台共享模型或租户私有模型（例如 `gpt-4o-mini` 或兼容模型服务），完成连通性检测。
2. **构建企业知识库**：进入“知识库”，创建知识库并上传企业文档；系统使用 Parent-Child 切块并将向量安全写入 Qdrant。
3. **编排业务应用**：在“业务应用”中新建应用并进入“应用执行流程”，按业务场景自由组合开始、提示词模板、模型生成、知识检索、条件分支与人工审批等节点。
4. **生成候选版本**：编排保存草稿后，点击固化生成发布候选版本（Candidate），直观比对与上一版本的拓扑结构差异。
5. **自动化评测与门禁**：在“评测中心”运行针对该候选版本的评测任务；门禁系统将自动比对任务成功率、Groundedness 忠实度与耗时，生成发布门禁证据。
6. **发布正式版本**：门禁通过后，一键生成不可变正式 Release，生产指针自动平滑切换。
7. **多渠道应用接入**：在“应用入口”获取对话链接、动态表单地址、开放 REST 接口契约或 Webhook 密钥，分发至业务系统。
8. **监控与运营闭环**：在“运行中心”追踪任务执行链路，在“任务中心”完成审批挂起决策，在“故障处理”处理任务重试，在“套餐与权益”观测用量与成本消耗。

---

## 7. 项目目录结构

```text
enterprise-agent-studio/
├── AGENTS.md                               # 项目架构说明与编码守则
├── README.md                               # 项目说明与快速起步文档
├── enterprise_agent.sql                    # 数据库完整表结构与初始种子转储
├── .env.example                            # 环境变量配置模板
├── backend/                                # 后端 Spring Boot 工程
│   ├── pom.xml                             # 后端 Maven 依赖配置
│   ├── sql/                                # 顺序结构迁移与种子脚本目录 (000 ~ 044)
│   │   └── SQL-执行顺序.md                 # 数据库迁移执行顺序规范
│   └── src/main/java/com/acme/agentstudio/
│       ├── interfaces/rest/                # REST 控制器 (应用生命周期、编排、运行时、鉴权、SaaS等)
│       ├── application/                    # 应用服务层 (组织事务、跨领域流程、门禁判定、执行编排)
│       ├── domain/                         # 领域模型 (强类型契约、状态枚举、业务规则、不可变事实)
│       ├── infrastructure/                 # 基础设施层 (MyBatis-Plus、Qdrant 向量适配、LangChain4j)
│       ├── config/                         # 系统配置 (Spring Security、WebSocket、线程池、RagProperties)
│       └── common/                         # 公共组件 (统一响应封装、全局异常处理、错误码)
├── frontend/                               # 前端 Vue 3 + Vite 工程
│   ├── package.json                        # 前端依赖与脚本配置
│   ├── vite.config.js                      # Vite 构建与代理配置
│   └── src/
│       ├── views/                          # 任务功能页面 (共 34 个独立业务工作台页面)
│       │   ├── AgentStudioView.vue         # 业务应用列表
│       │   ├── WorkflowStudioView.vue      # 可视化工作流画布
│       │   ├── RuntimeWorkspaceView.vue    # 运行工作区 (候选版本/门禁/发布/拓扑比对)
│       │   ├── ApplicationLaunchView.vue   # 应用多渠道入口管理
│       │   ├── ChatConsoleView.vue         # 应用对话工作台
│       │   ├── KnowledgeBaseView.vue       # 知识库与 RAG 检索
│       │   ├── ModelCenterView.vue         # 模型中心与连通性诊断
│       │   ├── EvaluationCenterView.vue    # 评测中心与版本测试集
│       │   ├── WorkflowExecutionView.vue   # 运行中心与链路追踪
│       │   ├── RuntimeOperationsView.vue   # 故障处理与任务重放
│       │   ├── ApprovalCenterView.vue      # 任务中心与人工审批
│       │   ├── EntitlementUsageView.vue    # 套餐权益与用量账本
│       │   └── DataGovernanceView.vue      # 数据治理与合规冻结
│       ├── components/                     # 通用组件与工作流节点配置抽屉组件
│       ├── api/                            # 按业务领域强类型封装的 HTTP 接口层
│       ├── router/                         # 路由配置与菜单权限守卫
│       └── layouts/                        # 全局侧边栏与页面框架布局
├── scripts/                                # 自动化测试与静态合规校验脚本
└── docs/                                   # 架构设计、部署规范、RAG技术方案与操作手册
```

---

## 8. 校验与质量保障脚本

项目在 `scripts/` 目录下提供了用于自动化质量与合规检查的脚本集合：

- **编排规范静态校验**（检查节点目录、前端组件映射与连线发布契约）：
  ```powershell
  cd frontend
  npm run validate:orchestration
  ```
- **企业平台能力全项校验**：
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/validate-enterprise-platform.ps1
  ```
- **运行时强类型契约校验**：
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/validate-runtime-contract.ps1
  ```
- **核心生命周期冒烟流程演练**：
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/run-core-lifecycle-smoke.ps1
  ```

---

## 9. 常见问题排查 (FAQ)

### 9.1 登录提示用户名或密码错误
- 确认是否已成功导入 `enterprise_agent.sql` 或执行了 `038-development-clean-seed.sql`。
- 确认所选租户是否为系统租户（编码 `system`），默认管理员用户名为 `admin`，密码为 `admin123`。
- 检查后端日志中是否存在数据库连接拒绝、时区异常或 JWT 密钥未配置的报错。

### 9.2 知识库切块提示向量写入失败
- 确认 Qdrant 向量服务是否已在后台启动（默认端口 `6333`）。可通过访问 `http://localhost:6333/dashboard` 验证连通性。
- 检查 `.env` 中的 `QDRANT_BASE_URL` 配置是否正确；若 Qdrant 设置了 API Key，需同步填写 `QDRANT_API_KEY`。

### 9.3 工作流发布检查未通过
- 检查是否存在孤立节点：除开始和结束节点外，所有业务节点必须从开始节点可达，并能最终连通至结束节点。
- 检查节点必填项：模型生成节点是否已绑定有效模型并填写 Prompt；人工审批节点是否已指派审批组；RAG 检索节点是否已选定嵌入模型。
- 检查当前登录用户是否拥有该应用的发布权限以及所引用模型的调用权限。

### 9.4 流程执行处于等待（PENDING / WAITING）状态
- 若流程包含“人工审批”节点，流程会在该节点自动挂起；请使用审批组成员账号登录系统，前往“任务中心 (Approval)”提交审批结论后流程将自动推进。
- 若处于异步队列调度中，确认后端任务轮询线程（Task Queue Worker）是否正常工作。

### 9.5 模型调用返回凭证缺失或未配置
- 系统默认遵循安全原则，不伪造模型回答；若模型连接未填写真实有效 API Key，后端将抛出明确的业务异常。请在“模型中心”填写真实凭证后再试。

---

## 10. 安全与合规说明

1. **凭证脱敏与加密**：所有模型 API Key、第三方 Webhook Secret 与连接器凭证在落库前均通过 `APP_SECRET_KEY` 进行对称加密处理，界面展示与日志输出全流程脱敏。
2. **多租户强制边界**：所有业务查询与数据流转必须显式附带 `tenant_id` 过滤，严禁跨租户越权。
3. **不可变审计凭据**：任务调用详情、费用计量、人工审批决策和发布门禁报告全量记录不可篡改的业务审计事实，满足企业合规审计要求。
