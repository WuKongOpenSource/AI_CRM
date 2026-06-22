# Wukong AICRM


[![License](https://img.shields.io/badge/License-Noncommercial%20Use-blue.svg)](../../LICENSE)
[![Commercial Use](https://img.shields.io/badge/Commercial%20Use-Authorization%20Required-orange.svg)](../../LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/WuKongOpenSource/AI_CRM)](https://github.com/WuKongOpenSource/AI_CRM/stargazers)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/WuKongOpenSource/AI_CRM/pulls)

**Language:** [简体中文](../../README.md) | [English](README.en-US.md)

---

<a id="english-version"></a>
## 🇺🇸 English Version

> The Open Source Salesforce + ChatGPT
>
> Manage Customers.  
> Query Knowledge.  
> Execute Tasks.  
> All Through Conversation.

### 🚀 Try It Now
We strongly recommend you first experience the power of Wukong AICRM through the following methods.

| Experience | Address/Account | Notes |
| :--- | :--- | :--- |
| **🌐 Live Demo** | [https://www.72crm.ai/](https://www.72crm.ai/) | Register a cloud account to try it |
| **🔑 Trial Account** | Please register a new user on the cloud platform | For testing the online demo |
| **💬 Help & Discussion** | [Community Forum](https://bbs.72crm.com#/forum/detail/2020712408698912768) | Report issues and share ideas |

> **Tip**: The live demo comes pre-loaded with sample data and customer information. You can log in directly and experience all core features.

### ✨ What Can It Do?
Wukong AICRM is more than a traditional CRM; it‘s an AI partner that understands your business.

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

### 🧩 Overall Architecture

Wukong AICRM uses AI Assistant as the unified interaction layer, connecting customer management, knowledge retrieval, and task execution in one workflow.

```text
┌───────────────┐
│    User       │
└──────┬────────┘
       │
       ▼
┌───────────────┐
│ AI Assistant  │
└──────┬────────┘
       │
 ┌─────┼─────┐
 │     │     │
 CRM  RAG  Workflow
 │     │     │
 └─────┼─────┘
       │
       ▼
 PostgreSQL
 Redis
 MinIO
```
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
### ⚡️ Quick Start

We recommend using Docker one-click installation first. Use manual source installation only when you need local development or secondary development.

#### Option 1: Docker One-Click Installation (Recommended)

Prerequisites:

- Docker
- Docker Compose

```bash
git clone https://github.com/WuKongOpenSource/AI_CRM.git
cd AI_CRM/docker
docker-compose up -d
# Visit http://localhost
```

#### Option 2: Manual Source Installation

Prerequisites:

- JDK 21+
- Node.js 18+
- Maven 3.8+
- PostgreSQL 17
- Redis 6+

1. Clone the Repository

```bash
git clone https://github.com/WuKongOpenSource/AI_CRM.git
cd AI_CRM
```

2. Start the Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
# API service will run at http://localhost:8088
# API documentation (Knife4j): http://localhost:8088/doc.html
```

3. Start the Frontend

```bash
cd frontend
npm install
npm run dev
# Frontend will run at http://localhost:5173
```

Configuration: Before first run, configure database, AI API keys (such as OpenAI or DeepSeek), and other required settings according to comments in `backend/src/main/resources/application.yml`.

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

Please register a new user on the cloud platform for testing. Change the initialized administrator password immediately before production use and disable demo data if it is not needed.

## Model Configuration

After installation, you must go to "System Settings" -> "API/AI" to configure the AI large model by entering the corresponding API key. Otherwise, the conversation feature will fail.

---

### 🤝 Welcome Contributions
Wukong AICRM is in a rapid growth phase, and we warmly welcome contributions of all forms!
- 🐛 **Report Issues**: Use [GitHub Issues](https://github.com/WuKongOpenSource/AI_CRM/issues) to submit bugs or feature suggestions.
- 🔧 **Submit Code**: Pull Requests are welcome.
- 📖 **Improve Documentation**: Help with docs or translations.
- 💡 **Share Ideas**: Discuss in our [Community Forum](https://bbs.72crm.com).

### 📄 License
Wukong AICRM source code is available for learning, research, evaluation, and other noncommercial purposes. Commercial use, production deployment, hosted services, commercial derivative products, plugin / Agent marketplace cooperation, and brand use require separate commercial authorization. Please read [LICENSE](../../LICENSE), [LICENSE.en.md](../../LICENSE.en.md), [NOTICE](../../NOTICE), and [TRADEMARKS.md](../../TRADEMARKS.md).

### ❓ FAQ
**Q: Which AI models are supported?**
A: By default, it supports any model providing an OpenAI-compatible API (e.g., OpenAI GPT series, DeepSeek, Ollama local models). Configure the corresponding API Key in the backend “System Settings” -> “API/AI” section.

**Q: Is data safe for commercial use?**
A: The project can be fully self-hosted. All data (customers, documents, AI interactions) is stored on your own servers, ensuring data security.

**Q: How to get more help?**
A: You can visit the project’s [Community Forum](https://bbs.72crm.com) to ask questions or search for existing answers.
