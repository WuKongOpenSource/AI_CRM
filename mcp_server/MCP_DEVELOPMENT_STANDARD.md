# AI_CRM MCP 化开发规范

本文档用于规范如何将 AI_CRM 现有业务代码整理、封装并发布为 MCP 能力。目标是让 AI 助手可以安全、稳定、可审计地调用 CRM 业务能力，同时避免绕过现有后端权限、租户和业务规则。

适用范围：

- 主业务后端 `backend`
- 独立 MCP 适配层 `mcp_server`
- 面向 MCP Client 的 tools、resources、prompts

不适用范围：

- `sync_data` 数据同步模块，除非后续有明确需求单独规划
- 直接暴露数据库表、内部 DAO、Mapper 或 SQL
- 绕过后端 Service、权限注解、租户上下文的工具

## 1. 基本原则

### 1.1 MCP Server 是适配层，不是业务层

MCP Server 只负责把外部 AI 调用转换为 AI_CRM 后端已有 HTTP API 调用。

推荐链路：

```text
MCP Client
  -> AI_CRM MCP Server
  -> AI_CRM backend
  -> Service / Permission / Tenant / Database
```

禁止链路：

```text
MCP Client
  -> AI_CRM MCP Server
  -> Database
```

原因：

- 复用后端已有权限控制
- 复用后端已有租户隔离
- 复用后端已有参数校验和业务规则
- 避免 MCP 层变成第二套业务系统

### 1.2 优先只读，谨慎写入

MCP 能力应按风险分层开放：

| 类型 | 示例 | 开放策略 |
| --- | --- | --- |
| 只读查询 | 查客户、查任务、查知识库 | 第一优先级 |
| 低风险写入 | 新增跟进、创建任务、创建日程 | 需要确认与审计 |
| 高风险写入 | 删除客户、转移客户、批量修改 | 默认不开放 |
| 运维操作 | 同步、清理、重建索引 | 单独规划，不混入业务 MCP |

### 1.3 工具语义面向 AI，而不是机械映射 REST

MCP tool 名称应描述业务意图，而不是直接照搬 REST 路径。

推荐：

```text
get_customer_detail
list_customer_followups
search_knowledge
```

不推荐：

```text
customer_detail_id_get
post_followup_queryByCustomer
knowledge_ai_search_post
```

## 2. 代码组织规范

### 2.1 推荐目录结构

```text
mcp_server/
  package.json
  tsconfig.json
  README.md
  MCP_DEVELOPMENT_STANDARD.md
  src/
    index.ts              # stdio 入口，兼容本地 MCP Client
    server.ts             # createMcpServer，注册 tools/resources/prompts
    aicrmClient.ts        # AI_CRM backend HTTP client
    config.ts             # 环境变量解析与校验
    schemas.ts            # 共用 Zod schema
    tools/
      customerTools.ts
      contactTools.ts
      followupTools.ts
      taskTools.ts
      scheduleTools.ts
      knowledgeTools.ts
      searchTools.ts
    resources/
      customerResources.ts
      knowledgeResources.ts
    prompts/
      customerPrompts.ts
```

当前第一期实现可以保留在 `src/index.ts`，但当工具数量超过 10 个或增加 HTTP transport 时，应按上面的结构拆分。

### 2.2 文件职责

| 文件 | 职责 |
| --- | --- |
| `index.ts` | 本地 stdio 启动，不写业务逻辑 |
| `server.ts` | 创建 MCP server，统一注册 capabilities |
| `aicrmClient.ts` | 统一封装 GET/POST、token 转发、错误处理 |
| `schemas.ts` | 统一管理 ID、分页、枚举、过滤条件等 schema |
| `tools/*.ts` | 按业务模块注册工具 |
| `resources/*.ts` | 注册只读资源 URI |
| `prompts/*.ts` | 注册业务提示词模板 |

### 2.3 禁止事项

- 不在 tool handler 中拼接 SQL
- 不在 tool handler 中保存用户敏感信息
- 不在日志中打印 token、密码、密钥
- 不在 MCP 层复制复杂业务规则
- 不在 MCP 层硬编码生产环境地址
- 不把 `sync_data` 能力混入主业务 MCP

## 3. 工具设计规范

### 3.1 命名规范

统一使用 `snake_case`。

常用动词：

| 动词 | 含义 |
| --- | --- |
| `list` | 列表查询 |
| `get` | 按 ID 获取详情 |
| `search` | 检索 |
| `create` | 创建 |
| `update` | 更新 |
| `delete` | 删除，默认慎用 |
| `analyze` | 分析 |
| `parse` | 从自然语言解析结构化数据 |

示例：

```text
list_customers
get_customer_detail
list_customer_contacts
list_customer_followups
list_my_tasks
list_my_schedules
search_knowledge
get_knowledge_detail
```

### 3.2 工具说明规范

每个 tool 必须包含：

- `title`：简短标题
- `description`：描述业务场景和边界
- `inputSchema`：完整参数 schema
- 明确分页默认值
- 明确枚举值
- 明确 ID 类型

说明应让 AI 明白何时调用该工具，而不是只解释接口路径。

### 3.3 参数规范

#### ID 类型

AI_CRM 后端 Java `Long` 在 JSON 中可能以字符串返回。MCP 入参必须兼容 string 和 number。

推荐 schema：

```ts
const longIdInput = z.union([
  z.string().regex(/^\d+$/, "ID must contain only digits."),
  z.number().int().positive()
]);
```

#### 分页

分页参数统一为：

```text
page: 默认 1
limit: 默认 15，最大 100
```

不要让 AI 一次请求过大数据量。

#### 空值处理

调用后端前应移除：

```text
undefined
null
""
```

避免把空字符串当作过滤条件传给后端。

### 3.4 返回值规范

第一期可以直接返回后端 JSON，但应保持格式稳定：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

后续如果做面向 AI 的摘要化，应保留原始关键 ID，避免 AI 无法继续追问：

```json
{
  "customerId": "2047967282782457857",
  "companyName": "...",
  "stage": "...",
  "ownerName": "..."
}
```

### 3.5 错误处理规范

后端 HTTP 非 2xx：

```text
AI_CRM backend returned HTTP <status>: <body>
```

后端业务错误：

```text
AI_CRM backend returned code <code>: <msg>
```

常见错误：

| 错误 | 含义 | 处理 |
| --- | --- | --- |
| `code=302` | 未登录或 Redis 登录态失效 | 重新获取 token |
| `401` | MCP HTTP 认证失败 | 检查 Authorization |
| `403` | 权限不足 | 检查后端角色权限 |
| `404` | 数据不存在或会话不存在 | 检查 ID 或重新初始化 |

## 4. Resources 设计规范

Resources 用于稳定引用只读对象，不用于执行写操作。

推荐 URI：

```text
crm://customer/{customerId}
crm://customer/{customerId}/contacts
crm://customer/{customerId}/followups
crm://tasks/my?filter=today
crm://schedules/my?filter=thisWeek
crm://knowledge/{knowledgeId}
```

Resources 必须满足：

- 只读
- 可重复读取
- 不产生业务副作用
- 权限仍由后端决定

## 5. Prompts 设计规范

Prompts 用于封装高频业务分析模板，不直接替代 tools。

推荐 prompts：

| Prompt | 用途 |
| --- | --- |
| `summarize_customer_progress` | 总结客户近期进展 |
| `analyze_customer_risk` | 分析客户风险 |
| `suggest_next_sales_actions` | 给出下一步销售动作 |
| `draft_followup_plan` | 生成跟进计划 |
| `summarize_knowledge_document` | 总结知识库文档 |

Prompt 中可以引导 AI 先调用 tools 获取上下文，再生成结果。

## 6. 认证与租户规范

### 6.1 本地 stdio 模式

本地开发使用环境变量：

```text
AICRM_BASE_URL=http://127.0.0.1:8088
AICRM_TOKEN=<AI_CRM login token>
AICRM_TOKEN_HEADER=Manager-Token
```

MCP Server 调后端时转发：

```text
Manager-Token: <AICRM_TOKEN>
```

### 6.2 网络 HTTP 模式

网络版不得使用全局 `AICRM_TOKEN` 作为所有用户共享身份。

推荐请求模型：

```http
Authorization: Bearer <AI_CRM login token>
```

MCP Server 转发给后端：

```http
Manager-Token: <AI_CRM login token>
```

每个请求必须使用当前请求的 token，不允许串用户。

### 6.3 后端权限复用

MCP Server 不自行判断业务权限，只做基础认证检查。最终权限由后端控制：

- Spring Security
- JWT 登录态
- Redis session
- `@RequirePermission`
- 租户上下文
- 数据权限

## 7. Transport 规范

MCP 官方标准 transport 包括：

- `stdio`
- `Streamable HTTP`

参考：Model Context Protocol Transports 规范  
https://modelcontextprotocol.io/specification/2025-06-18/basic/transports

### 7.1 stdio

适用场景：

- 本地 IDE
- 本地 Agent
- 单用户开发环境

要求：

- 只向 `stdout` 输出 MCP JSON-RPC 消息
- 日志只能写 `stderr`
- 不在启动时打印 banner 到 `stdout`

### 7.2 Streamable HTTP

适用场景：

- 多客户端
- 远程部署
- 内网共享
- 企业统一接入

要求：

- 提供单一 MCP endpoint，例如 `/mcp`
- 支持 `POST`
- 按需支持 `GET` SSE
- 校验 `Origin`
- 实现认证
- 生产环境使用 HTTPS

## 8. 安全规范

### 8.1 必须做

- 所有请求必须认证
- 不打印 token
- 不打印完整请求体中的敏感字段
- 配置 CORS 白名单
- 配置 Origin 白名单
- 限制请求体大小
- 增加基础限流
- 后端地址通过环境变量配置
- 生产环境启用 HTTPS

### 8.2 禁止做

- 多用户共享一个管理员 token
- 允许匿名访问业务 tools
- MCP Server 直接连数据库
- 在 MCP Server 中保存明文密码
- 在日志中输出 `Authorization` 或 `Manager-Token`
- 给 AI 开放不受控删除接口

### 8.3 写操作确认

所有写操作必须至少满足：

```text
confirm: true
```

高风险写操作还应要求：

```text
reason: string
```

并记录审计日志：

```text
user
tenant
tool
arguments summary
result
timestamp
request id
```

## 9. 测试规范

### 9.1 编译测试

每次改动必须通过：

```powershell
cd mcp_server
npm run build
```

### 9.2 MCP 协议测试

必须验证：

- MCP Client 能初始化
- `tools/list` 能列出工具
- 每个新增工具能被发现
- 参数 schema 能正确校验

### 9.3 后端真实调用测试

使用有效 token 验证：

| 工具 | 期望 |
| --- | --- |
| `list_customers` | 返回分页客户 |
| `global_search` | 返回搜索结果 |
| `get_customer_detail` | 返回客户详情 |
| `list_customer_contacts` | 返回联系人数组 |
| `list_customer_followups` | 返回跟进数组 |
| `list_my_tasks` | 返回任务数组 |
| `list_my_schedules` | 返回日程数组 |
| `search_knowledge` | 返回知识库搜索结果 |
| `get_knowledge_detail` | 返回知识文档详情 |

### 9.4 认证测试

必须覆盖：

- 无 token
- 失效 token
- 有效 token
- 权限不足用户
- 不同租户用户

### 9.5 网络版测试

如果实现 Streamable HTTP，必须补充：

- `POST /mcp initialize`
- `POST /mcp tools/list`
- `GET /health`
- 无 `Authorization` 返回 401
- 非白名单 `Origin` 被拒绝
- 并发请求不串 token
- 后端不可用时错误清晰

## 10. 发布与部署规范

### 10.1 本地使用

本地 MCP Client 配置示例：

```json
{
  "mcpServers": {
    "ai-crm": {
      "command": "node",
      "args": ["D:/workspace/AI_CRM/mcp_server/dist/index.js"],
      "env": {
        "AICRM_BASE_URL": "http://127.0.0.1:8088",
        "AICRM_TOKEN": "your-ai-crm-token",
        "AICRM_TOKEN_HEADER": "Manager-Token"
      }
    }
  }
}
```

### 10.2 内部包发布

如果多个项目复用，应拆成独立包：

```text
@company/ai-crm-mcp-server
```

发布前需要：

- 移除 `private: true`
- 确认 `bin` 指向 `dist/index.js`
- 提供 README
- 提供 `.env.example`
- 提供版本号和 changelog

### 10.3 网络部署

推荐拓扑：

```text
MCP Client
  -> HTTPS Gateway / Nginx
  -> ai-crm-mcp-server
  -> AI_CRM backend
```

推荐环境变量：

```text
MCP_HTTP_HOST=127.0.0.1
MCP_HTTP_PORT=3100
AICRM_BASE_URL=http://127.0.0.1:8088
MCP_ALLOWED_ORIGINS=https://client.example.com
```

## 11. 新工具接入流程

新增 tool 必须按以下流程：

1. 确认后端已有 API 和权限注解
2. 确认不需要直连数据库
3. 设计 tool 名称和 description
4. 设计 Zod input schema
5. 增加 tool handler
6. 增加 SDK smoke test
7. 使用真实 token 调用一次
8. 更新 README 工具列表
9. 若有风险，补充确认参数和审计

评审问题清单：

- 这个 tool 是否会修改数据？
- 是否可能泄露跨租户数据？
- 是否一次返回过多数据？
- 是否包含敏感字段？
- 参数是否足够明确？
- AI 是否容易误用？
- 后端权限是否已经覆盖？

## 12. 当前第一期标准工具

当前第一期只读工具清单：

| Tool | 后端接口 | 风险 |
| --- | --- | --- |
| `global_search` | `POST /search/global` | 低 |
| `list_customers` | `POST /customer/queryPageList` | 低 |
| `get_customer_detail` | `GET /customer/detail/{id}` | 低 |
| `list_customer_contacts` | `POST /contact/queryByCustomer` | 低 |
| `list_customer_followups` | `POST /followup/queryByCustomer` | 低 |
| `list_my_tasks` | `GET /task/myTasks` | 低 |
| `list_my_schedules` | `GET /schedule/mySchedules` | 低 |
| `search_knowledge` | `POST /knowledge/ai-search` | 中 |
| `get_knowledge_detail` | `GET /knowledge/detail/{id}` | 低 |

`search_knowledge` 标记为中风险，是因为其返回内容可能包含文档摘要或片段，应依赖后端权限并控制返回数量。

## 13. 版本演进建议

### v0.1

- stdio transport
- 第一批只读 tools
- 环境变量 token
- SDK smoke test

### v0.2

- 拆分代码结构
- 增加 resources
- 增加 prompts
- 增加更完整的测试脚本

### v0.3

- 增加低风险写入 tools
- 增加 `confirm: true`
- 增加审计日志

### v0.4

- 增加 Streamable HTTP
- 每请求 token
- Origin/CORS/限流
- Docker 部署

### v1.0

- 多项目复用
- 完整认证方案
- 企业级审计
- 稳定版本发布

## 14. 参考资料

- MCP Transports Specification: https://modelcontextprotocol.io/specification/2025-06-18/basic/transports
- MCP TypeScript SDK: https://github.com/modelcontextprotocol/typescript-sdk
