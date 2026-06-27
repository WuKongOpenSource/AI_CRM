# 一个可二开的 AI CRM 工程长什么样

## 适合标题

- 从代码看悟空 AI CRM：一个可二开的 AI 应用工程
- AI SaaS 不是一个 Prompt：看看完整工程里有什么
- Vue3 + Spring Boot + Docker，搭一个真正能跑的 AI CRM

## 导语

很多 AI 项目看起来很酷，但打开代码后会发现：只有一个聊天页面、几个 Prompt、一个模型调用接口。这样的项目适合 Demo，却很难承载真实业务。

一个可二开的 AI CRM，至少要有前端交互、后端业务、权限、数据库迁移、文件存储、知识库、模型配置、Docker 部署和可扩展模块。

## 正文

悟空 AI CRM 的工程结构比较典型：`backend` 是 Spring Boot 项目，`frontend` 是 Vue 3 + TypeScript 项目，`docker` 目录提供 Docker Compose 部署配置。后端使用 Java 21、Spring Boot、Spring AI、MyBatis-Plus、PostgreSQL、Redis、MinIO；前端使用 Vue 3、Pinia、Element Plus、Tailwind CSS 和 Vite。

从目录上看，它不是一个单点 AI Demo，而是一个完整业务系统。

后端按模块拆分 controller、service、mapper、entity、BO、VO。客户、联系人、任务、日程、知识库、邮件、项目、产品、候选人、招聘岗位都有自己的接口和服务。数据库迁移使用 Flyway，`db/migration` 下可以看到从基础表、角色权限、任务、日程、产品、项目、邮件，到候选人和招聘岗位的持续演进。

前端则围绕工作台式布局展开。`MainLayout.vue` 管理左侧模块导航，`ChatView.vue` 承载聊天主界面，客户、候选人、项目、产品等都有自己的列表和详情视图。候选人模块还复用了客户模块的体验：列表、阶段、详情、对话、右侧信息面板。

AI 相关代码没有散落在各处，而是有清晰的边界：

- `ai/app` 定义应用和工具组。
- `ai/provider` 处理模型服务商。
- `ai/tools` 暴露业务工具。
- `ChatServiceImpl` 编排会话、上下文、附件、RAG 和工具调用。
- `AiContextHolder` 在工具调用时提供当前用户和业务对象上下文。

部署层也不是一句“自己装环境”。Docker Compose 编排了 CRM 后端、前端代理、PostgreSQL、Redis、MinIO、WeKnora app、docreader 等服务。对开源项目来说，这一点非常重要：用户能不能十分钟跑起来，决定了项目能不能被更多人试用和二开。

可二开还体现在动态字段和模块化工具上。客户字段、候选人字段、关系人字段、产品字段都不是完全写死；新增 AI 技能也可以沿着 `ChatApplicationRegistry + Tool` 的模式扩展。比如要增加合同模块，可以新建合同实体、合同工具、合同详情面板，再把工具组注册到一个新的应用里。

这类工程设计的价值，不只是当前功能多，而是后续扩展有路径。

## 适合开发者关注的点

1. 看 `ChatApplicationRegistry`，理解 AI 应用如何按业务场景拆分。
2. 看 `DynamicChatClientProvider`，理解模型配置和工具注册。
3. 看 `CustomerTools`、`CandidateTools`、`KnowledgeTools`，理解 AI 如何安全调用业务动作。
4. 看 `CustomFieldServiceImpl`，理解 CRM 可配置字段体系。
5. 看 `docker/docker-compose.yaml`，理解完整依赖如何本地部署。

## 结尾

AI 应用不是一个 Prompt，也不是一个聊天框。真正能进入企业业务的软件，需要完整工程能力。悟空 AI CRM 的意义在于，它把大模型、CRM 数据、知识库、工作流和可部署工程放在了一起，这正是开源 AI 应用最值得研究的地方。

