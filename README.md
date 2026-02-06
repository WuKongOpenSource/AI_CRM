# AI CRM

智能客户关系管理系统 - AI 驱动的 CRM(预览版)，支持自然语言对话、知识库 RAG 检索增强、任务自动生成。

## 功能特性

- **AI 对话助手** - 集成大语言模型，支持 RAG 检索增强生成，智能回答业务问题
- **客户管理** - 客户信息、联系人、标签分类、团队协作、销售阶段跟踪
- **知识库管理** - 文档上传存储、向量检索、会议记录、邮件、合同等文档管理
- **任务管理** - AI 自动生成待办任务、优先级设置、截止日期提醒
- **系统配置** - AI 模型配置、对象存储配置、知识库服务配置

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.3.12 | 应用框架 |
| Spring AI | 1.0.0 | AI/LLM 集成 (OpenAI 兼容 API) |
| MyBatis Plus | 3.5.7 | ORM 框架 |
| PostgreSQL | 17 | 关系型数据库 |
| Redis | - | 缓存与会话管理 |
| MinIO | - | S3 兼容对象存储 |
| JWT | - | 身份认证 |
| Knife4j | - | API 文档 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4 | 前端框架 |
| TypeScript | 5.5 | 类型安全 |
| Pinia | 2.2 | 状态管理 |
| Element Plus | 2.8 | UI 组件库 |
| Tailwind CSS | 3.4 | CSS 框架 |
| Vite | 5.4 | 构建工具 |
| Axios | 1.7 | HTTP 客户端 |

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- PostgreSQL 17
- Redis 6+
- Maven 3.8+

### 后端启动

```bash
cd backend

# 安装依赖并编译
mvn clean install

# 启动服务 (端口 8088)
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器 (端口 5173)
npm run dev

# 生产构建
npm run build
```

### Docker 部署

```bash
cd docker

# 启动所有服务
docker-compose up -d
```

服务端口：
- Nginx: 80
- 后端 API: 8088
- PostgreSQL: 5432
- Redis: 6379
- MinIO: 9000 (API) / 9001 (Console)

## 项目结构

```
wk_ai_crm/
├── backend/                 # 后端 Spring Boot 项目
│   ├── src/main/java/       # Java 源码
│   ├── src/main/resources/  # 配置文件
│   └── pom.xml              # Maven 配置
├── frontend/                # 前端 Vue 项目
│   ├── src/                 # 前端源码
│   └── package.json         # npm 配置
└── docker/                  # Docker 部署配置
    ├── docker-compose.yaml  # 编排文件
    └── nginx/               # Nginx 配置
```

## 配置说明

主要配置文件：`backend/src/main/resources/application.yml`

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wk_ai_crm
    username: postgres
    password: your_password
```

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
      database: 7
```

### AI 服务配置

```yaml
spring:
  ai:
    openai:
      api-key: your_api_key
      base-url: https://api.openai.com/v1/  # 或其他兼容 API
      chat:
        options:
          model: gpt-4
```

### MinIO 对象存储配置

```yaml
minio:
  enabled: true
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: ai-crm
```

### WeKnora 知识库服务配置

```yaml
weknora:
  enabled: true
  base-url: http://localhost:8080/api/v1
  api-key: your_api_key
  knowledge-base-id: your_kb_id
```

## API 文档

启动后端服务后，访问 Knife4j API 文档：

```
http://localhost:8088/doc.html
```

## 默认账号

- 用户名：`admin`
- 密码：`123456`

## 模型配置

-安装完成需要到“系统设置”的“API/AI”中进行AI大模型配置，输入对应的key,否则对话会出错。

## License

MIT
