# CoTask — 多维协作研发看板

> 一款面向团队协作的全栈看板式任务管理平台，支持多工作区、拖拽排序、实时协同、AI 智能助手。

**CoTask** 是一个 Trello / Notion 风格的项目管理工具，由 **Vue 3 前端** 和 **Spring Boot 后端** 组成，支持多人实时协作、基于角色的权限控制，并集成了 AI 智能助手（任务拆解、周报生成、风险预警）。

---

## ✨ 核心功能

### 📋 看板管理
- **多工作区隔离** — 每个用户可创建/加入多个工作区，数据完全隔离
- **多看板支持** — 每个工作区可创建多个看板，每个看板包含可自定义的列表（阶段列）
- **卡片任务** — 在列表中创建卡片，支持标题、描述、截止日期
- **拖拽排序** — 基于 SortableJS 的流畅拖拽，支持卡片跨列表移动和列表排序（后端采用 Double 小数插值算法）
- **实时协同** — 基于 STOMP over WebSocket 的实时数据同步，多人同时操作即时可见

### 👥 工作区协作
- **角色权限** — 三级角色体系：OWNER（所有者）> ADMIN（管理员）> MEMBER（成员）
- **邀请机制** — 生成邀请链接（24小时有效），通过邀请码加入工作区
- **成员管理** — 查看成员列表、移除成员、变更角色
- **在线状态** — 实时显示工作区内其他成员的在线/离线状态

### 🤖 AI 智能助手
- **任务拆解** — 选择一张卡片，AI 自动将其拆解为 3-5 个子任务
- **周报生成** — AI 扫描工作区内所有卡片，自动生成结构化 Markdown 周报
- **风险预警** — 智能识别临近截止日期但状态未更新的卡片，按 HIGH / MEDIUM / LOW 分级预警

### 📊 数据仪表盘
- **统计概览** — 总任务数、已完成数、进行中数
- **进度可视化** — 完成率进度条
- **风险预警** — 高风险卡片列表，截止日期临近提醒

### 📝 审计追踪
- **活动日志** — 记录卡片/列表/看板的创建、更新、删除、移动、评论等所有操作
- **变更对比** — 记录字段变更前后的值，可追溯完整操作历史

---

## 🏗️ 技术架构

```
┌─────────────────────────┐         ┌──────────────────────────────────┐
│   Frontend (Vue 3)      │  HTTP   │   Backend (Spring Boot 3.4)      │
│   Vite + TypeScript     │◄───────►│   Java 17 + PostgreSQL           │
│   Port: 5173 (dev)      │   REST  │   Port: 5000                     │
│                         │         │                                  │
│   STOMP.js              │◄─ WS ──►│   Spring WebSocket + STOMP       │
│   (实时协同)             │         │   (实时推送)                      │
└─────────────────────────┘         └──────────────┬───────────────────┘
                                                   │
                                     ┌─────────────┼─────────────┐
                                     ▼             ▼             ▼
                               ┌──────────┐  ┌──────────┐  ┌──────────┐
                               │PostgreSQL│  │ JWT Auth │  │ 豆包 AI  │
                               │ Database │  │  (24h)   │  │ API      │
                               └──────────┘  └──────────┘  └──────────┘
```

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | Composition API | 渐进式前端框架，`<script setup>` 语法 |
| TypeScript | 5.9 | 类型安全的 JavaScript 超集 |
| Vite | 7 | 极速前端构建工具 |
| Pinia | 3 | 轻量级状态管理 |
| Vue Router | 4 | 客户端路由 |
| Tailwind CSS | 4 | 原子化 CSS 框架 |
| Axios | — | HTTP 请求客户端（拦截器、JWT 注入） |
| STOMP.js + SockJS | — | WebSocket 实时通信 |
| SortableJS + vuedraggable | — | 拖拽排序 |
| Lucide Vue Next | — | 图标库 |

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.4.1 | Java 企业级应用框架 |
| Java | 17 | 运行环境 |
| Spring Security | — | 认证与授权（无状态 JWT） |
| Spring Data JPA + Hibernate | — | ORM 持久层 |
| Spring WebSocket + STOMP | — | 实时消息推送 |
| PostgreSQL | 14+ | 关系型数据库 |
| JJWT | 0.12.6 | JWT 令牌生成与校验 |
| Lombok | — | 样板代码精简 |
| BCrypt | — | 密码加密 |
| Jackson + Hibernate6 | — | JSON 序列化（懒加载支持） |

---

## 📁 项目结构

```
co-task/                              # 仓库根目录
├── README.md                         # 本文件
│
├── co-task-frontend/                 # 前端项目
│   ├── src/
│   │   ├── api/
│   │   │   ├── axios.ts              # HTTP 客户端（拦截器、Token 注入）
│   │   │   └── websocket.ts          # STOMP WebSocket 实时协作客户端
│   │   ├── components/
│   │   │   ├── AuthPage.vue          # 登录 / 注册页面
│   │   │   ├── KanbanBoard.vue       # 看板核心视图（拖拽排序）
│   │   │   ├── CardDetailModal.vue   # 卡片详情弹窗（评论、活动日志）
│   │   │   ├── WorkspaceSelector.vue # 侧边栏（工作区切换、创建、邀请）
│   │   │   ├── DashboardPanel.vue    # 数据仪表盘
│   │   │   ├── AiAssistant.vue       # AI 助手面板
│   │   │   ├── JoinWorkspace.vue     # 通过邀请链接加入工作区
│   │   │   └── ToastItem.vue         # 通知提示组件
│   │   ├── stores/
│   │   │   ├── authStore.ts          # 认证 / 用户 / 工作区状态
│   │   │   └── boardStore.ts         # 看板 / 列表 / 卡片状态
│   │   ├── router/index.ts           # 路由配置
│   │   ├── types/index.ts            # TypeScript 类型定义
│   │   ├── App.vue                   # 根组件
│   │   └── main.ts                   # 应用入口
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── co-task-backend/                  # 后端项目
│   ├── src/main/java/com/cotask/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java          # Spring Security + CORS
│   │   │   └── JacksonConfig.java           # JSON 序列化配置
│   │   ├── controller/
│   │   │   ├── AuthController.java          # /api/auth
│   │   │   ├── WorkspaceController.java     # /api/workspaces
│   │   │   ├── BoardController.java         # /api/boards
│   │   │   ├── TaskListController.java      # /api/lists
│   │   │   ├── CardController.java          # /api/cards
│   │   │   ├── CommentController.java       # /api/comments
│   │   │   ├── ActivityLogController.java   # /api/activities
│   │   │   └── AiAssistantController.java   # /api/ai
│   │   ├── entity/                          # JPA 实体类（User, Workspace, Board, Card …）
│   │   ├── repository/                      # Spring Data 仓库接口
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java        # JWT 生成与校验
│   │   │   ├── JwtAuthenticationFilter.java # JWT 认证过滤器
│   │   │   ├── WorkspacePermissionChecker.java # 工作区权限检查
│   │   │   └── AuthChannelInterceptor.java  # WebSocket JWT 拦截器
│   │   ├── service/                         # 业务逻辑层（接口）
│   │   │   └── impl/                        # 业务逻辑层（实现）
│   │   └── websocket/
│   │       ├── WebSocketConfig.java         # STOMP 配置
│   │       ├── BoardWebSocketController.java # 看板实时消息
│   │       ├── PresenceWebSocketController.java # 在线状态
│   │       └── OnlineUserTracker.java       # 在线用户追踪
│   ├── src/main/resources/
│   │   └── application.yml                  # 应用配置
│   ├── src/test/                            # 单元测试
│   ├── pom.xml
│   └── mvnw                                 # Maven Wrapper
│
└── docs/                              # 文档（可选）
    └── screenshots/                   # 截图
```

---

## 🗄️ 数据模型

```
Workspace (工作区)
  ├── Board (看板) × N
  │     └── TaskList (列表) × N
  │           └── Card (卡片) × N
  │                 └── Comment (评论) × N
  ├── WorkspaceMember (成员) × N
  │     └── User (用户)
  ├── ActivityLog (活动日志) × N
  └── AiTask (AI 异步任务) × N
```

### 核心实体

| 实体 | 关键字段 | 说明 |
|------|----------|------|
| **User** | id, email, name, passwordHash, createdAt | 用户账户 |
| **Workspace** | id, name, description, inviteCode | 工作区（团队空间） |
| **WorkspaceMember** | workspace, user, role (OWNER/ADMIN/MEMBER) | 成员-角色关联 |
| **Board** | id, title, workspace | 看板 |
| **TaskList** | id, title, position (Double), board | 列表（阶段列） |
| **Card** | id, title, description, position, dueDate, taskList | 任务卡片 |
| **Comment** | id, content, card, user | 卡片评论 |
| **ActivityLog** | id, actionType, entityType, oldValues, newValues | 操作审计日志 |
| **AiTask** | id, taskType, prompt, result, status | AI 异步任务记录 |

---

## 🔌 API 概览

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册（自动创建默认工作区） |
| POST | `/api/auth/login` | 用户登录（返回 JWT，24h 有效） |

### 工作区

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/workspaces` | 创建工作区 | 登录用户 |
| GET | `/api/workspaces/my` | 获取我的工作区列表 | 登录用户 |
| GET | `/api/workspaces/{id}` | 获取工作区详情 | MEMBER+ |
| PUT | `/api/workspaces/{id}` | 更新工作区 | ADMIN+ |
| DELETE | `/api/workspaces/{id}` | 删除工作区 | OWNER |
| GET | `/api/workspaces/{id}/invite-link` | 生成邀请链接 | ADMIN+ |
| POST | `/api/workspaces/join` | 通过邀请码加入 | 登录用户 |
| GET | `/api/workspaces/{id}/members` | 查看成员列表 | MEMBER+ |
| PUT | `/api/workspaces/{id}/members/{userId}/role` | 变更成员角色 | OWNER |
| DELETE | `/api/workspaces/{id}/members/{userId}` | 移除成员 | ADMIN+ |

### 看板 / 列表 / 卡片 / 评论

完整 CRUD + 拖拽移动接口，详见后端 Controller 源码。

### WebSocket 实时事件

| STOMP 端点 | 方向 | 说明 |
|------------|------|------|
| `/topic/workspace/{id}/card/moved` | 服务端→客户端 | 卡片移动广播 |
| `/topic/workspace/{id}/card/updated` | 服务端→客户端 | 卡片更新广播 |
| `/topic/workspace/{id}/list/updated` | 服务端→客户端 | 列表更新广播 |
| `/topic/workspace/{id}/presence` | 服务端→客户端 | 在线状态广播 |
| `/app/board/card/move` | 客户端→服务端 | 卡片拖拽移动 |
| `/app/board/card/update` | 客户端→服务端 | 卡片内容更新 |

### AI 助手

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/decompose` | 任务拆解（异步，返回 taskId 轮询） |
| POST | `/api/ai/summary/weekly-report` | 周报生成（异步，返回 taskId 轮询） |
| GET | `/api/ai/risks/workspace/{id}` | 风险分析（同步返回） |
| GET | `/api/ai/task/{taskId}` | 查询异步任务执行结果 |

---

## 🚀 快速开始

### 环境要求

| 组件 | 要求 |
|------|------|
| **前端** | Node.js ≥ 18，npm 或 pnpm |
| **后端** | JDK 17，Maven 3.8+ |
| **数据库** | PostgreSQL 14+ |
| **AI（可选）** | 豆包 API Key 或 OpenAI 兼容 API Key |

### 1. 克隆仓库

```bash
git clone https://github.com/你的用户名/co-task.git
cd co-task
```

### 2. 配置并启动后端

```bash
cd co-task-backend

# 1) 确保 PostgreSQL 已启动，创建数据库
# psql -U postgres -c "CREATE DATABASE cotask_db;"

# 2) 编辑 src/main/resources/application.yml，修改数据库用户名和密码

# 3) 设置 AI API Key（可选，不设置则 AI 功能不可用）
# Linux / macOS:
export DOUBAO_API_KEY=your_api_key_here
# Windows:
set DOUBAO_API_KEY=your_api_key_here

# 4) 编译并启动
./mvnw spring-boot:run       # Linux / macOS
mvnw.cmd spring-boot:run     # Windows

# 后端运行在 http://localhost:5000
# WebSocket 端点: ws://localhost:5000/ws
```

### 3. 配置并启动前端

```bash
cd co-task-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 前端运行在 http://localhost:5173
```

### 4. 访问应用

浏览器打开 `http://localhost:5173`，注册账号后即可使用。

新用户注册时会**自动创建**一个默认工作区，包含一个演示看板和四个列表：**待办事项**、**进行中**、**审核中**、**已完成**。

---

## 🔐 安全设计

| 机制 | 说明 |
|------|------|
| **JWT 认证** | 登录后获取 24 小时有效期的 JWT Token，存储于 localStorage |
| **BCrypt 加密** | 用户密码使用 BCrypt 哈希存储，不可逆 |
| **三级角色权限** | OWNER > ADMIN > MEMBER，每个 API 端点均有角色校验 |
| **WebSocket 鉴权** | 连接时通过 STOMP CONNECT 帧携带 Token，AuthChannelInterceptor 校验 |
| **无状态会话** | REST API 完全无状态，每次请求通过 `Authorization: Bearer <token>` 携带 JWT |
| **CORS 配置** | 后端配置允许前端跨域访问 |

---

## 🧩 角色权限矩阵

| 操作 | OWNER | ADMIN | MEMBER |
|------|:-----:|:-----:|:------:|
| 查看看板 / 卡片 | ✅ | ✅ | ✅ |
| 创建 / 编辑 / 移动卡片 | ✅ | ✅ | ✅ |
| 添加评论 | ✅ | ✅ | ✅ |
| 创建 / 编辑列表 | ✅ | ✅ | ❌ |
| 创建 / 编辑看板 | ✅ | ✅ | ❌ |
| 管理工作区设置 | ✅ | ✅ | ❌ |
| 邀请成员 | ✅ | ✅ | ❌ |
| 移除成员 | ✅ | ✅ | ❌ |
| 变更成员角色 | ✅ | ❌ | ❌ |
| 删除工作区 | ✅ | ❌ | ❌ |

---



