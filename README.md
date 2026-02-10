# 🤖 AI CRM | AI-Powered Conversational CRM
> 智能对话式客户关系管理系统 / Intelligent Conversational Customer Relationship Management System

[![License](https://img.shields.io/github/license/WuKongOpenSource/AI_CRM)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/WuKongOpenSource/AI_CRM)](https://github.com/WuKongOpenSource/AI_CRM/stargazers)
[![GitHub Release](https://img.shields.io/github/v/release/WuKongOpenSource/AI_CRM)](https://github.com/WuKongOpenSource/AI_CRM/releases)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/WuKongOpenSource/AI_CRM/pulls)

**快速导航** | **Quick Nav:** [🇨🇳 中文](#-中文版--chinese) | [🇺🇸 English](#-english-version--英文版)

---
<a id="-中文版--chinese"></a>
## 🇨🇳 中文版 | Chinese

### 🚀 立即体验
| 体验方式 | 地址/账号 | 说明 |
| :--- | :--- | :--- |
| **🌐 在线演示站** | [https://aicrm.5kcrm.cn](https://aicrm.5kcrm.cn) | **一键访问，推荐首选** |
| **🔑 演示站账号** | 用户：`admin` <br> 密码：`123456a` | 用于登录在线演示站 |
| **💬 帮助与讨论** | [前往社区论坛](https://bbs.72crm.com#/forum/detail/2020712408698912768) | 反馈问题、交流想法 |

> **提示**：在线演示站已预置示例数据和客户信息，您可以直接登录并体验所有核心功能。

### ✨ 它能做什么？
| 功能模块 | 核心价值 |
| :--- | :--- |
| **💬 AI对话助手** | **像同事一样询问业务**：“上一季度华东区的销售冠军是谁？”，系统可结合结构化数据与知识库文档，生成智能回答。 |
| **🧠 知识库RAG增强** | **赋予AI“记忆”**：上传公司产品手册、合同、会议纪要，AI助手能基于这些文档内容进行精准问答和总结。 |
| **👥 智能客户管理** | **一体化客户视图**：集中管理客户信息、联系人、跟进记录，并通过AI自动分析客户阶段与需求。 |
| **✅ AI任务生成** | **自动创建工作项**：在对话或分析客户后，可指令AI创建待办任务，并自动设置优先级与提醒。 |
| **🔗 无缝团队协作** | **信息实时同步**：客户动态、任务分配、知识更新均在团队内即时同步，促进高效协作。 |

### 🛠️ 技术栈

- **后端**: Java 21 + Spring Boot 3.x + Spring AI + PostgreSQL + Redis + MinIO
- **前端**: Vue 3 + TypeScript + Element Plus + Tailwind CSS
- **部署**: 支持 Docker Compose 一键部署，提供完整生产环境配置。

#### 后端技术栈明细
| 技术 | 版本 | 说明 |
| :--- | :--- | :--- |
| Java | 21 | 编程语言 |
| Spring Boot | 3.3.12 | 应用框架 |
| Spring AI | 1.0.0 | AI/LLM 集成 (支持 OpenAI 兼容 API) |
| PostgreSQL | 17 | 主数据库 |
| MyBatis-Plus | 3.5.7 | 数据持久层框架 |
| Redis | - | 缓存与会话管理 |
| MinIO | - | 对象存储（用于文档、文件） |

#### 前端技术栈明细
| 技术 | 版本 | 说明 |
| :--- | :--- | :--- |
| Vue | 3.4 | 前端框架 |
| TypeScript | 5.5 | 类型安全 |
| Element Plus | 2.8 | UI 组件库 |
| Pinia | 2.2 | 状态管理 |
| Tailwind CSS | 3.4 | 实用CSS框架 |
| Vite | 5.4 | 构建工具 |

### 📁 项目结构
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
└── LICENSE.md               # 协议文件
└── README.md                # 本文档

```
### ⚡️ 快速开始（本地开发）
如果你想在本地运行或进行二次开发，请遵循以下步骤。

#### 先决条件
- JDK 21+, Node.js 18+, Maven 3.8+
- PostgreSQL 17, Redis 6+
- (可选) Docker & Docker Compose

#### 1. 克隆项目
```bash
git clone https://github.com/WuKongOpenSource/AI_CRM.git
cd AI_CRM
```
#### 2. 后端启动
```bash
cd backend
mvn clean install
mvn spring-boot:run
# API服务将在 http://localhost:8088 运行
# API文档 (Knife4j): http://localhost:8088/doc.html
```

#### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
# 前端将在 http://localhost:5173 运行
```

#### 4. 使用Docker一键部署（推荐）

```bash
cd docker
docker-compose up -d
# 访问 http://localhost 即可 (Nginx反向代理了前后端)
```

配置文件：首次运行前，请根据 backend/src/main/resources/application.yml 中的注释，配置数据库、AI API密钥（如OpenAI、DeepSeek等）等必要信息。

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
- 密码：`123456a`

## 模型配置

-安装完成需要到“系统设置”的“API/AI”中进行AI大模型配置，输入对应的key，否则对话会出错。

---

### 🤝 欢迎贡献
AI CRM 正处于快速成长阶段，我们热烈欢迎任何形式的贡献！
- 🐛 **报告问题**：使用 [GitHub Issues](https://github.com/WuKongOpenSource/AI_CRM/issues) 提交Bug或新功能建议。
- 🔧 **提交代码**：请阅读我们的贡献指南（待创建），了解开发流程和代码规范。
- 📖 **完善文档**：帮助改进文档、翻译，让项目更易懂。
- 💡 **分享想法**：在[社区论坛](https://bbs.72crm.com)分享你的使用场景或优化建议。

### 📄 许可证
本项目基于 [MIT License](LICENSE) 开源。这意味着你可以自由地使用、修改和分发代码，但需保留原作者的版权声明。

### ❓ 常见问题
**Q：AI模型支持哪些？**
A：默认支持任何提供 OpenAI 兼容 API 的模型（如 OpenAI GPT系列、DeepSeek、Ollama本地模型等）。在后台“系统设置”的“API/AI”配置中填入对应API Key即可。

**Q：商业使用时数据安全吗？**
A：项目可完全私有化部署，所有数据（客户、文档、AI交互）均保存在您自己的服务器中，确保数据安全。

**Q：如何获取更多帮助？**
A：您可以访问项目的 [社区论坛](https://bbs.72crm.com) 提问或搜索现有答案。

---
<a id="-english-version--英文版"></a>
## 🇺🇸 English Version | 英文版

### 🚀 Try It Now
We strongly recommend you first experience the power of AI CRM through the following methods.

| Experience | Address/Account | Notes |
| :--- | :--- | :--- |
| **🌐 Live Demo** | [https://aicrm.5kcrm.cn](https://aicrm.5kcrm.cn) | **One-click access, recommended** |
| **🔑 Demo Account** | User: `admin` <br> Password: `123456a` | For logging into the live demo site |
| **💬 Help & Discussion** | [Community Forum](https://bbs.72crm.com#/forum/detail/2020712408698912768) | Report issues and share ideas |

> **Tip**: The live demo comes pre-loaded with sample data and customer information. You can log in directly and experience all core features.

### ✨ What Can It Do?
AI CRM is more than a traditional CRM; it‘s an AI partner that understands your business.

| Feature | Core Value |
| :--- | :--- |
| **💬 AI Conversational Assistant** | **Ask about business like talking to a colleague**: “Who was the sales champion in East China last quarter?” The system can generate intelligent answers by combining structured data and knowledge base documents. |
| **🧠 Knowledge Base RAG Enhancement** | **Give AI “memory”**: Upload company product manuals, contracts, meeting minutes. The AI assistant can provide precise Q&A and summaries based on these documents. |
| **👥 Intelligent Customer Management** | **Unified customer view**: Centrally manage customer information, contacts, follow-up records, with AI automatically analyzing customer stages and needs. |
| **✅ AI Task Generation** | **Automatically create work items**: After conversations or customer analysis, instruct AI to create to-do tasks with automatic priority and reminders. |
| **🔗 Seamless Team Collaboration** | **Real-time information sync**: Customer updates, task assignments, and knowledge updates are instantly synchronized within the team for efficient collaboration. |

### 🛠️ Technology Stack
This is a full-stack open-source project with a modern and stable technology stack.
- **Backend**: Java 21 + Spring Boot 3.x + Spring AI + PostgreSQL + Redis + MinIO
- **Frontend**: Vue 3 + TypeScript + Element Plus + Tailwind CSS
- **Deployment**: Supports one-click deployment via Docker Compose with complete production environment configuration.

#### Backend Tech Stack Details
| Technology | Version | Purpose |
| :--- | :--- | :--- |
| Java | 21 | Programming Language |
| Spring Boot | 3.3.12 | Application Framework |
| Spring AI | 1.0.0 | AI/LLM Integration (OpenAI-compatible API) |
| PostgreSQL | 17 | Primary Database |
| MyBatis-Plus | 3.5.7 | ORM Framework |
| Redis | - | Cache & Session Management |
| MinIO | - | Object Storage (for docs, files) |

#### Frontend Tech Stack Details
| Technology | Version | Purpose |
| :--- | :--- | :--- |
| Vue | 3.4 | Frontend Framework |
| TypeScript | 5.5 | Type Safety |
| Element Plus | 2.8 | UI Component Library |
| Pinia | 2.2 | State Management |
| Tailwind CSS | 3.4 | Utility-first CSS Framework |
| Vite | 5.4 | Build Tool |

### 📁 Project Structure
```
wk_ai_crm/
├── backend/                 # Backend Spring Boot Project
│   ├── src/main/java/       # Java Source Code
│   ├── src/main/resources/  # Configuration Files
│   └── pom.xml              # Maven Configuration
├── frontend/                # Frontend Vue Project
│   ├── src/                 # Frontend Source Code
│   └── package.json         # npm Configuration
├── docker/                  # Docker Deployment Configuration
│   ├── docker-compose.yaml  # Orchestration File
│   └── nginx/               # Nginx Configuration
├── LICENSE.md               # License File
└── README.md                # This Document

```
### ⚡️ Quick Start (Local Development)
If you want to run it locally or contribute to development, please follow these steps.

#### Prerequisites
- JDK 21+, Node.js 18+, Maven 3.8+
- PostgreSQL 17, Redis 6+
- (Optional) Docker & Docker Compose

#### 1. Clone the Repository
```bash
git clone https://github.com/WuKongOpenSource/AI_CRM.git
cd AI_CRM
```
#### 2. Start the Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
# API server will run at http://localhost:8088
# API Docs (Knife4j): http://localhost:8088/doc.html
```

#### 3. Start the Frontend

```bash
cd frontend
npm install
npm run dev
# Frontend will run at http://localhost:5173
```

#### 4. One-Click Deployment with Docker (Recommended)

```bash
cd docker
docker-compose up -d
# Visit http://localhost (Nginx reverse proxies frontend and backend)
```

Configuration: Before first run, configure necessary information like database and AI API keys (e.g., OpenAI, DeepSeek) according to comments in backend/src/main/resources/application.yml.

## Configuration Guide

Main configuration file: `backend/src/main/resources/application.yml`

### Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wk_ai_crm
    username: postgres
    password: your_password
```

## Redis Configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
      database: 7
```

## AI Service Configuration

```yaml
spring:
  ai:
    openai:
      api-key: your_api_key
      base-url: https://api.openai.com/v1/  # Or other compatible API
      chat:
        options:
          model: gpt-4
```

## MinIO Object Storage Configuration

```yaml
minio:
  enabled: true
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: ai-crm
```

## WeKnora Knowledge Base Service Configuration

```yaml
weknora:
  enabled: true
  base-url: http://localhost:8080/api/v1
  api-key: your_api_key
  knowledge-base-id: your_kb_id
```

## API Documentation

After starting the backend service, access the Knife4j API documentation at:

```
http://localhost:8088/doc.html
```

## Default Account

· Username: admin
· Password: 123456a

## Model Configuration

After installation, you must go to "System Settings" -> "API/AI" to configure the AI large model by entering the corresponding API key. Otherwise, the conversation feature will fail.

---

### 🤝 Welcome Contributions
AI CRM is in a rapid growth phase, and we warmly welcome contributions of all forms!
- 🐛 **Report Issues**: Use [GitHub Issues](https://github.com/WuKongOpenSource/AI_CRM/issues) to submit bugs or feature suggestions.
- 🔧 **Submit Code**: Pull Requests are welcome.
- 📖 **Improve Documentation**: Help with docs or translations.
- 💡 **Share Ideas**: Discuss in our [Community Forum](https://bbs.72crm.com).

### 📄 License
This project is open source under the [MIT License](LICENSE). This means you are free to use, modify, and distribute the code, provided that the original copyright notice is retained.

### ❓ FAQ
**Q: Which AI models are supported?**
A: By default, it supports any model providing an OpenAI-compatible API (e.g., OpenAI GPT series, DeepSeek, Ollama local models). Configure the corresponding API Key in the backend “System Settings” -> “API/AI” section.

**Q: Is data safe for commercial use?**
A: The project can be fully self-hosted. All data (customers, documents, AI interactions) is stored on your own servers, ensuring data security.

**Q: How to get more help?**
A: You can visit the project’s [Community Forum](https://bbs.72crm.com) to ask questions or search for existing answers.

---
<div align="center">

## 🌟 项目动态 / Project Updates
**最新 / Latest**: 项目预览版 v0.1.0 正式开源！/ Preview v0.1.0 officially open-sourced!

**如果 AI CRM 对你有帮助，请给我们一个 ⭐️ Star！这是对我们开源工作的最大鼓励。**<br>
**If AI CRM helps you, please give us a ⭐️ Star! It's the greatest encouragement for our open-source work.**

</div>
