# AI 智慧课程平台 · 课程实训开源版

[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0--only-blue.svg)](LICENSE)
[![Docs: CC BY-NC-SA 4.0](https://img.shields.io/badge/Docs-CC%20BY--NC--SA%204.0-lightgrey.svg)](LICENSE-DOCS)

> **AI Smart Course Platform** — 深度融合人工智能技术的教育管理与学习辅助平台。  
> 以"智能化生产、结构化管理、个性化学习"为核心目标，引入爬塔游戏化学习模式。

本仓库为东北大学软件系统开发实训课程成果的**合规开源版本**。  
后续论文研究、课堂在线对接、实验数据等将在私有研究仓库中继续演进。

---

## 架构概览

```text
┌──────────────────────────────────────────────────┐
│  Frontend（前端）                                  │
│  Vue 3 + Element Plus · 教师端 · 学生端            │
├──────────────────────────────────────────────────┤
│  HarmonyOS 端                                     │
│  ArkTS · 爬塔游戏化学习 · 能力图谱                  │
├──────────────────────────────────────────────────┤
│  Backend（后端）                                   │
│  Spring Boot 3.5 · RESTful API · MyBatis-Plus     │
├──────────────────────────────────────────────────┤
│  Agentic（AI Agent 服务）                          │
│  DeepSeek / Dify · 知识图谱构建 · 智能评阅          │
├──────────────────────────────────────────────────┤
│  Data：PostgreSQL / KingbaseES                     │
└──────────────────────────────────────────────────┘
```

## 项目结构

规格驱动（SDD）：`specs/`（应该建什么）→ `docs/`（怎么建）→ 代码目录（实际建什么）。

```
.
├── specs/                  规格文档 — 需求分析、实施计划（项目事实源）
│   ├── common/             编码规范与写作约定
│   ├── 党圣航/             前端游戏化重构各阶段实施计划
│   ├── 宋芷萱/             后端测试记录与模块2需求分析
│   ├── 张文慧/             模块4学生画像需求规格
│   ├── 黄榆航/             教师端精进与测评模块分析
│   └── README.md           规格目录说明
├── docs/                   设计文档 — 架构设计、接口定义、部署方案
│   ├── architecture/       模块接口契约
│   ├── plans/              各模块实施计划
│   └── README.md
├── frontend/               Vue 3 前端（教师端 + 学生端）
├── backend/                Spring Boot RESTful API 服务
├── harmonyOS/              HarmonyOS 爬塔游戏化学习 App
├── agentic/                AI Agent 服务目录（预留）
├── scripts/                数据种子脚本与工具
├── resource/               教学资源与示例课程内容
├── references/             参考文档与迭代日志
│   └── ITERATION_LOG.md    开发迭代日志
└── README.md               本文件
```

### 目录职责

| 目录 | 职责 | 内容示例 |
|------|------|---------|
| `specs/` | **待做与需求** — 开发前先写清楚"应该建什么" | 需求分析、实施计划、功能拆解 |
| `docs/` | **怎么建** — 设计方案，写代码前先确定技术方案 | 架构设计、API 定义、数据模型、部署方案 |
| `references/` | 开发过程参考 | 迭代日志、外部参考资料 |

> **门控规则**：新功能或行为变更，先把需求写进 `specs/`，再在 `docs/` 确定技术方案，最后写代码。

## 快速开始

### 环境要求

- **Java** 17+
- **Node.js** 18+
- **Maven** 3.9+
- **PostgreSQL** 15+（或 KingbaseES V9）
- **DevEco Studio**（仅 HarmonyOS 端需要）

### 后端启动

```bash
cd backend

# 1. 配置环境变量（复制模板并填入你的数据库信息）
cp .env.example .env
# 编辑 .env 文件，填入 DB_URL / DB_USERNAME / DB_PASSWORD

# 2. 编译运行
mvn spring-boot:run

# 后端默认运行在 http://localhost:8081/practical-training
```

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run serve

# 前端默认运行在 http://localhost:8080
```

### HarmonyOS 端

使用 DevEco Studio 打开 `harmonyOS/` 目录，连接设备或模拟器后运行。

### 种子数据

```bash
# Python 题库种子（需要先启动后端）
node scripts/seed-python-question-bank.js

# 学生账户种子
node scripts/seed-software-students.js

# 学习记录种子
node scripts/seed-python-learning-records.js
```

> 种子脚本使用环境变量配置连接信息，详见各脚本顶部注释。默认管理员账户：`admin` / `admin123`（仅开发测试用途）。

## 技术栈

### 后端

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.5.15 | Web 框架 |
| MyBatis-Plus | 3.5.15 | ORM |
| PostgreSQL | 15+ | 主数据库 |
| KingbaseES | V9 | 国产数据库适配 |
| JUnit 5 | — | 测试框架 |
| Lombok | — | 代码简化 |

### 前端

| 组件 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| Element Plus | 2.14+ | UI 组件库 |
| Vue Router | 4.x | 前端路由 |
| Axios | 1.x | HTTP 请求 |
| ECharts | 6.x | 数据可视化 |
| Pinia | 3.x | 状态管理 |

### HarmonyOS

| 组件 | 说明 |
|------|------|
| ArkTS | 声明式 UI 开发语言 |
| ArkUI | HarmonyOS UI 框架 |
| Navigation | 路由导航 |

### AI 服务

| 组件 | 说明 |
|------|------|
| DeepSeek | LLM API（Anthropic 兼容接口） |
| Dify | AI 应用平台（RAG、Workflow、Agent） |

## 核心功能

### 课程与资源管理
- 管理员/教师登录注册，学生登录注册
- 课程、课时、学习任务的增删改查
- 课时资源（视频/文档）、任务附件、学生作业上传
- Python 课程示例（17课时视频 + VTT字幕）

### 知识图谱
- 知识点管理（名称、章节、重要性等级）
- 知识点前置关系（prerequisite 边）
- LLM 驱动的知识点抽取（Dify RAG）

### 题库与组卷
- 五种题型：单选、多选、填空、简答、编程
- 三种组卷策略：随机组卷、按知识点组卷、难度平衡
- 在线测验发布与题目绑定

### 在线测验与智能评阅
- 客观题自动评阅（单选/多选/填空）
- 主观题 AI 辅助评阅（简答/编程）
- 教师复核与反馈

### 学习任务与过程跟踪
- 任务发布（在线测验/文档上传/在线答题）
- 提交管理与截止时间
- 重复提交控制（maxAttempts）
- 成绩统计：学生成绩总览、趋势、明细

### 学生画像与个性化学习（模块4）
- 能力雷达图（多维度能力点评估）
- 知识点掌握度热力图
- 学习进度跟踪与滞后检测

### 学情分析与教学决策（模块5）
- 班级管理与风险预警
- 成绩分析与进度监控
- AI 共性问题聚类
- 教学建议生成

### 爬塔游戏化学习（HarmonyOS 端）
- 知识地图塔层（TowerMap）— 知识点驱动的楼层路径
- 战斗房间（BattleRoom）— 答题闯关
- 商店房间（ShopRoom）— 错题卡兑换
- 宝藏房间（TreasureRoom）— 学习资源解锁
- 休息房间（RestRoom）— 体力恢复
- AI 导师（AiTutor）— 智能学习辅导
- 能力图谱（AbilityMap）— 学习成果可视化

## API 接口

> 鉴权基于 HttpSession，角色分三类：`student` / `teacher` / `admin`。  
> 接口统一使用 `/api/` 前缀。

### 权限矩阵

| 接口 | student | teacher | admin | 备注 |
|------|:---:|:---:|:---:|------|
| **登录注册** | | | | |
| `POST /api/students/login` `POST /api/students/register` | ✅ | ✅ | ✅ | 公开 |
| `POST /api/teachers/login` `POST /api/teachers/register` | ✅ | ✅ | ✅ | 公开 |
| **模糊查询** | | | | |
| `GET /api/teachers/search` `GET /api/courses/search` | ✅ | ✅ | ✅ | 按关键词 |
| **关联查询** | | | | |
| `GET /api/courses/{courseCode}/lessons` | ✅ | ✅ | ✅ | 课程下级课时 |
| `GET /api/tasks?course_id={code}` | ✅ | ✅ | ✅ | 课程下级任务 |
| **管理员专用** | | | | |
| `GET/PUT/DELETE /api/students/{studentNo}` | ❌ | ❌ | ✅ | |
| `GET/PUT/DELETE /api/teachers/{teacherNo}` | ❌ | ❌ | ✅ | |
| `GET /api/students/list` `GET /api/teachers/list` | ❌ | ❌ | ✅ | |
| **教师可操作** | | | | |
| `POST/PUT/DELETE /api/courses/...` | ❌ | ✅¹ | ✅ | ¹仅限授课教师 |
| `POST/PUT/DELETE /api/lessons/...` | ❌ | ✅¹ | ✅ | |
| `POST/PUT/DELETE /api/tasks/...` | ❌ | ✅¹ | ✅ | |
| `POST/PUT/DELETE /api/questions/...` | ❌ | ✅¹ | ✅ | 题库管理 |
| `POST /api/questions/course/{code}/generate` | ❌ | ✅¹ | ✅ | 组卷 |
| **任务提交** | | | | |
| `POST /api/tasks/{taskNo}/submit` | ✅ | ❌ | ❌ | 提交答案 |
| `GET /api/tasks/{taskNo}/submissions` | ❌ | ✅¹ | ✅ | 查看提交 |
| `PUT /api/submissions/{id}` | ❌ | ✅¹ | ✅ | 打分反馈 |
| **成绩统计** | | | | |
| `GET /api/stats/student/{studentNo}` | ✅² | ❌ | ✅ | 个人成绩 |
| `GET /api/stats/course/{courseCode}` | ❌ | ✅¹ | ✅ | 课程统计 |
| **学情分析（模块5）** | | | | |
| `GET /api/analytics/...` | ❌ | ✅¹ | ✅ | 成绩分析/进度/预警 |
| **学生画像（模块4）** | | | | |
| `GET /api/profiles/...` | ✅² | ✅¹ | ✅ | 能力图谱/掌握度 |
| **游戏化（爬塔）** | | | | |
| `GET/POST /api/tower/...` | ✅ | ❌ | ❌ | 爬塔数据/事件 |

> ¹ 仅限该课授课教师   ² 仅限学生本人

### 实体关系

```
Course ──1:N──▶ Lesson
Course ──1:N──▶ LearningTask ──1:N──▶ TaskSubmission
Course ──1:N──▶ Question
LearningTask ──N:M──▶ Question（通过 TaskQuestion）
Student ──────────────────────1:N──▶ TaskSubmission
Teacher ──(授课)──▶ Course
```

## 数据模型

详见 [docs/](docs/) 目录下的架构文档和 [specs/](specs/) 目录下的需求规格。

核心实体：Student（学生）、Teacher（教师）、Course（课程）、Lesson（课时）、LearningTask（学习任务）、TaskSubmission（提交记录）、Question（题库）、KnowledgePoint（知识点）、RiskAlert（风险预警）。

## 已知限制

1. **学生与课程绑定**：当前学生和课程无直接关联，需引入选课/课程分配机制。
2. **班级管理**：需完善班级概念，使师生只看到自己范围内的任务。
3. **知识图谱可视化**：知识点关系目前以文本维护，图谱可视化待完善。
4. **批量导入导出**：学生、教师、题库缺少 Excel/CSV 批量导入和成绩报表导出。
5. **AI 评阅覆盖**：当前 AI 评阅已接入（DeepSeek/Dify），但覆盖率和准确性仍在迭代。
6. **密码存储**：当前为明文存储（开发阶段简化），生产部署前需改为 BCrypt 等哈希方案。

## 贡献者

详见 [CONTRIBUTORS.md](CONTRIBUTORS.md)。

## 许可证

- **源代码**：[GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0-only）
- **文档**：[Creative Commons BY-NC-SA 4.0](LICENSE-DOCS)

> ⚠️ **远程网络交互声明**：本平台为 Web 应用程序。根据 AGPL-3.0 第 13 条，若您修改本软件并通过计算机网络远程提供访问，您必须同时向用户提供获取修改后完整源代码的途径。

---

*东北大学 软件学院 · 软件系统开发实训 · 2026*
