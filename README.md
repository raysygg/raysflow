# Enterprise Agent Studio

Enterprise Agent Studio 是一个面向企业内部场景的 Agent、知识库和工作流编排平台。它的核心原则是：业务数据落 MySQL，节点能力由数据库目录驱动，草稿和正式版本分离，正式运行必须经过租户、资源和权限校验。

## 1. 能力总览

### 1.1 企业与安全

- 多租户隔离：所有业务数据按 `tenant_id` 隔离。
- 登录与会话：登录接口签发短期访问令牌，并使用 HttpOnly refresh token 管理会话续期。
- 组织架构：企业、部门、团队、成员、直属上级和主组织。
- 角色权限：角色菜单权限、资源例外授权、工作流草稿/发布/运行权限。
- 运维安全：API 限流、合规规则、API Key、审计日志和设备会话管理。

### 1.2 Agent 与对话

- Agent 从数据库读取模型、团队和工作流资源。
- Agent 可以是对话型或工作流型。
- 对话型 Agent 通过 WebSocket 实时返回消息和执行状态。
- 工作流型 Agent 只能运行生产环境绑定的正式版本，不直接运行未发布草稿。

### 1.3 知识库与 RAG

- 文档元数据、分片、标签和索引任务落 MySQL。
- 向量索引当前使用本地文件持久化，文件位置由 `app.rag.vector-store-file` 配置。
- 检索使用向量 + 关键词混合召回，结果包含来源文档和分片信息，便于回答追溯。
- 会话预览、工作流与评测在空命中时拒答，并对模型回答做 grounded 引用校验。
- 每次检索写入 `rag_retrieval_metric`，运营中心展示真实命中率。
- 文档上传、切块、索引、检索和失败重试均有状态记录。

### 1.4 工作流编排

工作流由以下部分组成：

- 节点：业务处理单元，例如开始、结束、模型生成、知识检索、Agent 调用、条件分支、人工审批、并行、循环和连接器调用。
- 边：节点之间的有向连接，保存稳定的节点 ID 和端口。
- 变量：节点之间传递的输入、输出和中间值。
- 节点目录：保存在 `orchestration_node_type`，包含显示名称、说明、能力、权限、端口、配置 schema 和执行器标识。
- 草稿：可反复编辑的修订版本。
- 正式版本：发布后不可变的版本快照。
- 环境指针：例如 `PRODUCTION` 指向当前正式版本，回滚只切换指针，不修改历史版本。

### 1.5 执行与观测

- 执行上下文、节点事件、会话消息和调用事实落库。
- 支持幂等键，重复提交同一个业务请求不会重复执行。
- 支持执行租约、心跳、暂停、恢复、取消和可恢复任务查询。
- 支持人工审批挂起和决策后继续执行。
- 工作流执行中心展示节点输入、输出、错误、尝试次数和追踪信息。

## 2. 技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、Vue Router、Axios |
| 后端 | Java 17、Spring Boot 3.3、Spring Security、WebSocket、SSE |
| 持久化 | MySQL、MyBatis-Plus |
| 缓存与限流 | Redis |
| 模型与 RAG | LangChain4j、OpenAI 兼容接口、BGE 中文 embedding |
| 本地运行 | Node.js、Maven |

## 3. 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+
- Redis 6+
- PowerShell 5+（Windows 下执行校验脚本需要）

## 4. 第一次初始化

### 4.1 创建数据库

先创建数据库，不要直接把多个历史 migration 文件拼在一起执行：

```sql
CREATE DATABASE enterprise_agent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 4.2 执行结构和基础数据

新环境只需要按以下顺序执行两个文件：

```bash
mysql -h your-host -P 3306 -u your-user -p enterprise_agent < backend/sql/schema.sql
mysql -h your-host -P 3306 -u your-user -p enterprise_agent < backend/sql/data.sql
```

Windows PowerShell 也可以使用：

```powershell
Get-Content -Raw backend/sql/schema.sql | mysql -h your-host -P 3306 -u your-user -p enterprise_agent
Get-Content -Raw backend/sql/data.sql | mysql -h your-host -P 3306 -u your-user -p enterprise_agent
```

`schema.sql` 只创建表、索引和约束；`data.sql` 写入租户、角色、菜单、节点目录和演示基础数据。已有业务数据的环境不要重复执行 `data.sql`，应按发布流程执行对应 migration。

### 4.3 配置环境变量

复制根目录 `.env.example` 为 `.env`，至少填写：

```dotenv
DB_URL=jdbc:mysql://localhost:3306/enterprise_agent?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=replace-with-a-long-random-secret
APP_SECRET_KEY=replace-with-a-different-random-secret
REDIS_HOST=localhost
REDIS_PORT=6379
```

生产环境必须使用独立的随机 `JWT_SECRET` 和 `APP_SECRET_KEY`，不要把 `.env` 提交到 Git。

## 5. 启动项目

### 5.1 启动后端

在项目根目录执行：

```powershell
cd backend
mvn spring-boot:run
```

默认后端地址为 `http://localhost:8090`。

### 5.2 启动前端

另开一个终端：

```powershell
cd frontend
npm install
npm run dev
```

默认前端地址为 `http://localhost:5176`。开发环境 Vite 会将 `/api` 和 `/ws` 代理到 `VITE_DEV_BACKEND_URL`，默认是 `http://localhost:8090`。

### 5.3 构建前端

```powershell
cd frontend
npm run build
```

## 6. 推荐业务操作顺序

1. 使用租户管理员登录，确认企业、角色和组织架构。
2. 在“模型底座”配置真实模型连接，并完成一次可用性检查。
3. 在“知识库”上传文档，等待索引状态变为可用。
4. 在“Agent 工作台”创建 Agent，选择数据库中的模型、团队和已发布工作流。
5. 在“工作流编排”创建草稿，添加节点、填写节点属性并连接节点。
6. 点击“保存草稿”，查看“发布检查”返回的阻断项。
7. 解决所有必填字段、资源权限和执行器问题后发布。
8. 在“运行预览”调试单节点或执行生产版本。
9. 在“工作流执行”查看执行链路、错误和节点输出。
10. 若流程暂停在人工审批，到“人工审批中心”提交意见后继续执行。

## 7. 工作流编排使用说明

### 7.1 创建画布

- 工作流名称：给业务人员看的名称。
- 唯一编码：系统识别用，创建后不建议修改。
- 运行模式：`业务工作流` 用于自动化任务；`对话流` 用于聊天入口和多轮会话。
- 左侧节点库由后端节点目录生成，不是前端写死的完整能力列表。
- 点击节点可以添加到画布，也可以拖到画布指定位置。

### 7.2 配置节点

单击画布节点后，右侧会打开悬浮配置面板，不会压缩画布。配置面板包含：

- 显示名称：只影响画布和执行记录展示。
- 基础配置：后端 schema 标记的必填字段。
- 高级设置：超时、重试、上下文、输出结构等可选项。
- 真实资源选择：模型、知识文档、Agent、组织审批组和连接器均从后端接口读取。
- 帮助文案：说明字段用途、示例和为空时的下一步。

运行时使用 ID 或稳定编码关联资源，节点名称和模型显示名称不会作为运行时关联键。

### 7.3 常用节点

| 节点 | 用途 | 配置重点 |
| --- | --- | --- |
| 开始 | 工作流入口 | 通常无需额外配置 |
| 结束 | 工作流出口 | 确保所有有效路径可以到达 |
| 模型生成 | 调用模型生成文本 | 主模型、备用模型、Prompt、输出结构 |
| 知识检索 | 检索已索引文档 | 文档范围、检索策略、召回数量、元数据过滤 |
| Agent 调用 | 调用另一个 Agent | 选择已配置 Agent 和输入变量 |
| 条件分支 | 根据值选择路径 | 判断变量、操作符、满足/不满足目标 |
| 人工审批 | 挂起等待人工决策 | 审批标题、说明、审批组、风险级别 |
| 并行/聚合 | 并行执行并汇总 | 分支和聚合关系、输出合并规则 |
| HTTP/OpenAPI/MCP | 调用外部工具 | 连接器、方法、参数、超时、重试和错误策略 |

### 7.4 连线和发布

- 连接点左侧是输入，右侧是输出。
- 也可以在左侧“连接节点”区域选择上游和下游节点后添加连线。
- 一个有效业务图必须有且只有一个开始节点和一个结束节点。
- 节点不能自连，边不能重复，所有业务节点必须可从开始节点到达并最终到达结束节点。
- 发布前会检查节点目录状态、执行器、必填配置、端口、资源和权限。
- 发布生成不可变正式版本，并把环境指针更新到该版本。
- 回滚只切换环境指针，历史版本保留不删除。

## 8. 数据和代码边界

### 8.1 应该落数据库的数据

- 租户、用户、组织、角色和权限。
- 模型连接、路由规则、配额和合规规则。
- Agent、工作流草稿、正式版本、环境指针。
- 节点目录、节点 schema、节点配置和图边。
- 知识文档、分片、索引任务和标签。
- 会话、消息、执行上下文、节点事件、调用指标和审计日志。

### 8.2 允许在代码中的内容

- Java 枚举和协议常量，例如状态值、权限动作和节点类型代码。
- 安全校验、执行器分派和不可变业务规则。
- 前端仅用于显示的中文标签映射；真实选项和资源仍由接口/schema 提供。

### 8.3 RAG 向量文件

MySQL 保存文档和分片元数据；当前向量索引文件由 `app.rag.vector-store-file` 指定。生产多实例部署前，需要将向量存储替换为共享向量数据库或共享持久化存储。

## 9. 目录说明

```text
frontend/src/views/                 页面和业务交互
frontend/src/components/workflow/   节点配置组件
frontend/src/api/                   HTTP API 封装
backend/src/main/java/.../interfaces REST、WebSocket、SSE 入口
backend/src/main/java/.../application 业务编排和执行服务
backend/src/main/java/.../domain    领域模型和业务对象
backend/src/main/java/.../infrastructure 数据库、节点目录、RAG、连接器实现
backend/sql/schema.sql              数据库结构
backend/sql/data.sql                初始基础数据
scripts/                            静态校验脚本
docs/                               需求、设计、部署和审计资料
```

## 10. 校验与排障

### 10.1 编排静态校验

```powershell
npm.cmd run validate:orchestration
```

该命令只检查迁移、节点目录、前端组件和发布契约是否存在，不连接 MySQL，也不能替代真实发布检查。

### 10.2 常见问题

**登录失败**

- 检查数据库是否执行了 `schema.sql` 和 `data.sql`。
- 检查租户编码、账号和密码是否与基础数据一致。
- 检查浏览器是否保留了旧 token；必要时退出并清理站点数据。
- 检查后端日志中的数据库连接和 JWT 配置错误。

**节点库为空**

- 检查 `orchestration_node_type` 是否有基础数据。
- 检查后端 `/api/orchestration/node-types` 是否返回成功。
- 检查当前工作流模式是否匹配节点支持的 graph type。

**节点配置没有资源**

- 模型节点需要先配置模型连接。
- 知识节点需要先上传并完成索引。
- Agent 节点需要先创建 Agent。
- 审批节点需要先创建组织团队。
- 工具节点需要先接入启用状态的连接器。

**无法发布**

- 先查看发布检查清单，不要只看前端按钮状态。
- 确认所有必填字段已保存到草稿。
- 确认节点目录为 ACTIVE 且存在可用执行器。
- 确认当前用户拥有工作流发布权和所引用资源的使用权。

**执行停留在等待状态**

- 人工审批节点需要在人工审批中心提交决策。
- 异步任务需要确认持久化任务 worker 正常运行。
- 检查执行中心中的 lease、heartbeat、错误码和节点事件。

## 11. 当前边界

- 当前项目提供完整的企业 Agent 平台骨架和主要运行链路，但连接器、代码沙箱、部分高级节点仍需根据生产安全策略接入真实执行环境。
- 启动前必须对目标 MySQL 执行结构和基础数据脚本；不要把“代码能启动”当作数据库初始化完成。
- 上线前仍应验证真实模型、Redis、多租户隔离、SSE/WebSocket、人工审批和异常恢复链路。
