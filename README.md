# AI智慧课程平台

深度融合人工智能技术的教育管理及学习辅助平台，以"智能化生产、结构化管理、个性化学习"为核心目标。

## 架构

```text
┌──────────────────────────────────────────────┐
│  Frontend（前端）                              │
│  教师端 · 学生端 · 管理后台                     │
├──────────────────────────────────────────────┤
│  Backend（后端）                               │
│  RESTful API · 业务逻辑 · 数据持久化            │
├──────────────────────────────────────────────┤
│  Agentic（AI Agent 服务）                      │
│  知识图谱构建 · 智能批改 · 个性化学习推荐         │
├──────────────────────────────────────────────┤
│  Data：关系型数据库 + 向量数据库                  │
└──────────────────────────────────────────────┘
```

## 项目结构

规格驱动（SDD）：`specs/`（应该建什么）→ `docs/`（怎么建）→ 代码目录（实际建什么）。

```
.
├── specs/                  ★ 规格文档 — 待做事项与需求（项目唯一事实源）
│   ├── 毛瑞琪/             个人需求分析 / 创新点 / 技术调研文档
│   └── README.md           规格目录说明与写作规范
├── docs/                   ★ 设计文档 — 确定后的技术方案与设计说明（怎么写）
├── frontend/               前端应用（教师端 + 学生端）
├── backend/                后端 RESTful API 服务
├── agentic/                AI Agent 服务（LLM 驱动的知识图谱、智能评分、推荐）
├── resource/               静态资源与教学资源模板
├── references/             参考文档与迭代日志
│   └── ITERATION_LOG.md    开发迭代日志（每次提交均在此记录）
└── README.md               本文件
```

### 目录职责

| 目录 | 职责 | 内容示例 |
|------|------|---------|
| `specs/` | **待做与需求**（开发前先写清楚"应该建什么"） | 需求分析、创新点、技术调研、任务拆解 |
| `docs/` | **怎么建**（设计方案，写代码前先确定技术方案） | 架构设计、API 定义、数据模型、部署方案 |
| `references/` | 开发过程参考 | 迭代日志、外部参考资料 |

> **门控规则**：新功能或行为变更，先把需求写进 `specs/`，再在 `docs/` 确定技术方案，最后写代码。直接写代码不碰文档 = 不合规。

## 开发流程

```text
  需求分析          规格编写           方案设计           代码实现
  ───────→  specs/  ───────→  docs/   ───────→  frontend/
                （应该建什么）    （怎么建）      backend/
                                              agentic/
```

1. **需求阶段** — 在 `specs/` 中编写需求分析、创新点、功能拆解
2. **设计阶段** — 在 `docs/` 中确定架构设计、API 定义、数据模型等技术方案
3. **实现阶段** — 在对应代码目录（`frontend/`、`backend/`、`agentic/`）中按设计方案开发
4. **迭代日志** — 每次提交后更新 `references/ITERATION_LOG.md`

> 新人上手前先读：[specs/README.md](specs/README.md)（规格说明）、[references/ITERATION_LOG.md](references/ITERATION_LOG.md)（最新进展）

## 技术栈

| 层 | 技术方向 |
|---|---------|
| 前端 | 教师端 + 学生端（框架待定） |
| 后端 | RESTful API（语言 / 框架待定） |
| Agentic | LLM 驱动的 AI Agent 服务 |
| 数据库 | 关系型数据库 + 向量数据库（产品待定） |

## 快速启动

> 各模块独立开发，详见各自目录下的 README。

### 前端

```bash
cd frontend
# 待补充：安装依赖与启动命令
```

### 后端

```bash
cd backend
# 待补充：安装依赖与启动命令
```

### Agentic

```bash
cd agentic
# 待补充：安装依赖与启动命令
```

## 核心功能

- **课程管理** — 课程创建、编排、发布与选修管理
- **知识图谱** — LLM 驱动的知识点抽取与关联，构建学科知识网络
- **智能批改** — AI 辅助作业批改与反馈
- **个性化推荐** — 基于学习行为与知识图谱的学习路径推荐
- **鸿蒙端应用** — HarmonyOS PC / 移动端适配
- **国产数据库适配** — 金仓数据库支持

## 规格文档（spec-driven）

本项目以规格文档驱动开发，规格位于 [`specs/`](specs/)：
入口见 [`specs/README.md`](specs/README.md)。

## 数据模型

### Student（学生）

| 字段 | 类型 | 说明 |
|------|------|------|
| `studentNo` | INT (PK) | 学号，自增主键 |
| `name` | VARCHAR | 学生姓名 |
| `college` | VARCHAR | 所属学院 |
| `className` | VARCHAR | 班级，如"计科202班" |
| `courseGrades` | TEXT | 课程成绩（预留，JSON或文本格式） |
| `username` | VARCHAR | 登录用户名，唯一 |
| `password` | VARCHAR | 登录密码 |

### Teacher（教师）

| 字段 | 类型 | 说明 |
|------|------|------|
| `teacherNo` | INT (PK) | 教职工码，自增主键 |
| `name` | VARCHAR | 教师姓名 |
| `college` | VARCHAR | 所属学院 |
| `major` | VARCHAR | 专业方向（系） |
| `phone` | VARCHAR | 联系电话 |
| `role` | VARCHAR | 角色：`teacher` 教师 / `admin` 管理员 |
| `username` | VARCHAR | 登录用户名，唯一 |
| `password` | VARCHAR | 登录密码 |

### Course（课程）

| 字段 | 类型 | 说明 |
|------|------|------|
| `courseCode` | INT (PK) | 课程编号，自增主键 |
| `courseName` | VARCHAR | 课程名称 |
| `teacher` | VARCHAR | 授课教师姓名（关联 Teacher.name） |
| `credits` | INT | 学分 |
| `hours` | INT | 总学时 |
| `coverUrl` | VARCHAR | 封面图片路径，如 `resource/CourseResource/xxx.png` |
| `lessons` | (虚拟) | 课时列表，仅内存映射，不持久化 |

### Lesson（课时）

| 字段 | 类型 | 说明 |
|------|------|------|
| `lessonNo` | INT (PK) | 课时编号，自增主键 |
| `courseCode` | INT | 所属课程编号（外键 → Course） |
| `lessonTitle` | VARCHAR | 课时标题 |
| `resourceType` | VARCHAR | 资源类型：`video` / `ppt` / `doc` / `img` |
| `resourceUrl` | VARCHAR | 资源文件路径，上传后自动记录 |
| `description` | TEXT | 内容简介 |

### LearningTask（学习任务）

| 字段 | 类型 | 说明 |
|------|------|------|
| `taskNo` | INT (PK) | 任务编号，自增主键 |
| `courseCode` | INT | 所属课程编号（外键 → Course） |
| `taskType` | VARCHAR | 任务类型，如"编程作业"、`quiz` 在线测验等 |
| `description` | TEXT | 任务说明 |
| `deadline` | DATETIME | 截止时间 |
| `submitMethod` | VARCHAR | 提交方式，如"在线提交""文档上传" |
| `score` | INT | 任务分值 |
| `resourceUrl` | VARCHAR | 附件资源路径，如 `resource/TaskResource/xxx.pdf` |

### TaskSubmission（任务提交记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| `submissionId` | INT (PK) | 提交编号，自增主键 |
| `taskNo` | INT | 关联任务编号（外键 → LearningTask） |
| `studentNo` | INT | 提交学生学号（外键 → Student） |
| `content` | TEXT | 文字提交内容 |
| `filePath` | VARCHAR | 上传附件路径，如 `resource/HomeworkUpload/xxx.pdf` |
| `submitTime` | DATETIME | 提交时间 |
| `score` | INT | 得分，可由系统自动评阅或教师复核后写入 |
| `status` | VARCHAR | 状态：`submitted` 待教师复核 / `graded` 已完成评阅 |
| `feedback` | TEXT | 系统评阅说明或教师反馈 |

### Question（题库）

| 字段 | 类型 | 说明 |
|------|------|------|
| `questionId` | INT (PK) | 题目编号，自增主键 |
| `courseCode` | INT | 所属课程编号 |
| `lessonNo` | VARCHAR | 关联课时编号 |
| `type` | VARCHAR | 题型：`single` 单选 / `multi` 多选 / `fill` 填空 / `essay` 简答 / `program` 编程 |
| `stem` | TEXT | 题干 |
| `options` | TEXT | 选项 JSON，选择题使用 |
| `answer` | VARCHAR/TEXT | 正确答案、参考答案或评分要点 |
| `difficulty` | INT | 难度等级，1~5 |
| `knowledgePoint` | VARCHAR | 关联知识点，目前以文本方式记录 |
| `score` | INT | 默认分值 |

### TaskQuestion（测验题目绑定）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT (PK) | 绑定编号，自增主键 |
| `taskNo` | INT | 测验任务编号 |
| `questionId` | INT | 题目编号 |

## 已实现功能

### 课程与资源管理

- 学生、教师、管理员登录注册。
- 课程、课时、学习任务的增删改查。
- 课时资源、任务附件、学生提交附件上传。
- 教师/管理员按课程管理课时、任务和提交记录。

### 题库与组卷

- 支持题库管理：单选、多选、填空、简答、编程题。
- 题目可维护课程、课时、知识点、难度、分值、答案或评分要点。
- 发布在线测验时支持手动选题。
- 支持三种组卷方式：
  - 随机组卷：从候选题中随机抽取指定数量。
  - 按知识点组卷：按所选知识点分组轮询抽题，尽量覆盖多个知识点。
  - 难度平衡：按难度等级分组轮询抽题，尽量避免试卷难度单一。
- 组卷可按题型、知识点、难度范围、题目数量筛选。
- 生成试卷后自动勾选题目，保存测验时绑定到 `task_question`。

### 在线测验与评阅

- 学生可进入在线测验答题。
- 客观题包括单选、多选、填空，提交后系统自动评阅。
- 简答题、编程题进入教师复核，不自动判分。
- 教师端可查看学生答案、正确答案/参考答案、系统评阅结果，并进行最终复核。
- 首页“我的任务”支持待复核任务 loading 状态，并发加载课程任务和提交记录。

### 成绩统计

- 学生端展示成绩总览、成绩趋势和成绩明细。
- 教师端展示课程任务统计、提交人数、已评阅数量和平均分。
- 数据检测/统计页面展示任务名称，不再只显示任务类型。

## 技术栈

### 后端

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.5.15 | Web 框架 |
| MyBatis-Plus | 3.5.15 | ORM（含逻辑删除、驼峰映射） |
| MySQL | runtime | 数据库（`mysql-connector-j`） |
| Lombok | provided | 简化 Java 代码 |
| JUnit 5 | (Spring Boot 内置) | 测试框架 |
| DevTools | optional | 开发热重载 |

### 前端

| 组件 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| Element Plus | 2.14+ | UI 组件库 |
| Vue Router | 4.x | 前端路由 |
| Axios | 1.x | HTTP 请求 |
| ECharts | 6.x | 成绩可视化图表 |
| Pinia | 3.x | 状态管理（预留） |

### 构建工具

| 组件 | 版本 |
|------|------|
| Maven | 3.9+ |
| Vue CLI | 5.x |

## API 接口

> 鉴权基于 HttpSession，角色分三类：`student` / `teacher` / `admin`

### 权限矩阵

| 接口 | student | teacher | admin | 备注 |
|------|:---:|:---:|:---:|------|
| **登录注册** | | | | |
| `POST /student/login` `POST /student/register` | ✅ | ✅ | ✅ | 公开 |
| `POST /teacher/login` `POST /teacher/register` | ✅ | ✅ | ✅ | 公开 |
| **模糊查询** | | | | |
| `GET /teacher/search?keyword=` | ✅ | ✅ | ✅ | 按姓名/学院 |
| `GET /course/search?keyword=` | ✅ | ✅ | ✅ | 按名称/编号 |
| `GET /lesson/search?keyword=` | ✅ | ✅ | ✅ | 按标题/简介 |
| `GET /task/search?keyword=` | ✅ | ✅ | ✅ | 按类型/说明 |
| **关联查询** | | | | |
| `GET /course/{courseCode}/lessons` | ✅ | ✅ | ✅ | 课程下级课时 |
| `GET /lesson/{courseCode}` | ✅ | ✅ | ✅ | 某课程下所有课时 |
| `GET /lesson/detail/{lessonNo}` | ✅ | ✅ | ✅ | 课时详情（含课程名、教师名） |
| `GET /task/{courseCode}` | ✅ | ✅ | ✅ | 课程下级任务 |
| `GET /question/{questionId}` | ✅ | ✅ | ✅ | 查看题目详情 |
| `GET /question/task/{taskNo}` | ✅ | ✅ | ✅ | 查看测验绑定题目 |
| **管理员专用** | | | | |
| `GET/PUT/DELETE /student/{studentNo}` | ❌ | ❌ | ✅ | |
| `GET/PUT/DELETE /teacher/{teacherNo}` | ❌ | ❌ | ✅ | |
| `GET /student/list` `GET /teacher/list` | ❌ | ❌ | ✅ | |
| `GET /course/list` `GET /course/{courseCode}` | ❌ | ❌ | ✅ | |
| `POST /course` `DELETE /course/{courseCode}` | ❌ | ❌ | ✅ | |
| **授课教师可操作** | | | | |
| `PUT /course/{courseCode}` | ❌ | ✅ ① | ✅ | ① 仅限该课授课教师 |
| `POST /lesson` `PUT/DELETE /lesson/{code}/{no}` | ❌ | ✅ ① | ✅ | |
| `POST /task`（multipart）`PUT/DELETE /task/{code}/{no}` | ❌ | ✅ ① | ✅ | 发布任务支持上传附件 |
| `POST /question` `PUT/DELETE /question/{id}` | ❌ | ✅ ① | ✅ | 题库管理 |
| `GET /question/course/{courseCode}` | ❌ | ✅ ① | ✅ | 查询课程题库 |
| `GET /question/lesson/{lessonNo}` | ❌ | ✅ ① | ✅ | 查询课时题目 |
| `GET /question/search?keyword=` | ❌ | ✅ | ✅ | 按题干/知识点搜索 |
| `POST /question/course/{courseCode}/generate` | ❌ | ✅ ① | ✅ | 按策略组卷 |
| `POST /question/task/{taskNo}` `DELETE /question/task/{taskNo}/{questionId}` | ❌ | ✅ ① | ✅ | 绑定或移除测验题目 |
| **学生搜索** | | | | |
| `GET /student/search?keyword=` | ❌ | ❌ | ✅ | 按姓名模糊查 |
| **任务提交** | | | | |
| `POST /submission` | ✅ | ❌ | ❌ | 提交文字+附件 |
| `GET /submission/my` | ✅ | ❌ | ❌ | 查看自己的提交 |
| `GET /submission/task/{taskNo}` | ❌ | ✅ ① | ✅ | 查看某任务所有提交 |
| `GET /submission/grade/{submissionId}` | ❌ | ✅ ① | ✅ | 查看系统评阅/教师复核详情 |
| `PUT /submission/{submissionId}` | ❌ | ✅ ① | ✅ | 打分+反馈 |
| **成绩统计** | | | | |
| `GET /stats/student/{studentNo}` | ✅ ② | ❌ | ✅ | 个人成绩总览+趋势 |
| `GET /stats/course/{courseCode}` | ❌ | ✅ ① | ✅ | 课程各任务统计 |

> ① 仅限该课授课教师 &nbsp; ② 仅限学生本人

### 组卷请求参数

`POST /question/course/{courseCode}/generate`

```json
{
  "strategy": "random",
  "count": 10,
  "types": ["single", "multi", "fill"],
  "knowledgePoints": ["函数定义", "列表"],
  "difficultyMin": 1,
  "difficultyMax": 4
}
```

| 字段 | 说明 |
|------|------|
| `strategy` | 组卷策略：`random` 随机组卷 / `knowledge` 按知识点组卷 / `difficulty` 难度平衡 |
| `count` | 目标题目数量 |
| `types` | 题型过滤，可选 `single`、`multi`、`fill`、`essay`、`program` |
| `knowledgePoints` | 知识点过滤，不传则不限制知识点 |
| `difficultyMin` / `difficultyMax` | 难度范围，1~5 |

### 实体关系

```
Course ──1:N──▶ Lesson
Course ──1:N──▶ LearningTask ──1:N──▶ TaskSubmission
Course ──1:N──▶ Question
LearningTask ──N:M──▶ Question（通过 TaskQuestion）
Student ──────────────────────1:N──▶ TaskSubmission
Teacher ──(授课)──▶ Course
```

## 待解决问题

1. **学生与课程绑定**：目前学生和课程没有直接关联，无法控制学生能看到哪些课程和任务。需要引入选课/课程分配机制。

2. **班级管理**：尚未引入班级概念。理想方式是老师认领班级及班级下学生，学生和老师分别只看自己班级/授课范围内的任务，避免不同班级学生互相看到彼此的任务提交。

3. **知识图谱实体化**：当前题目只用 `knowledgePoint` 文本关联知识点，尚未实现独立的知识点表、知识图谱边关系、图谱可视化和图谱节点详情页。

4. **批量导入导出**：学生、老师、题库目前主要是逐条录入，缺少 Excel/CSV 批量导入功能。成绩报表、学生名单、题库等也需要导出功能。

5. **AI 能力接入**：目前“系统评阅”只覆盖客观题规则判分，简答题、编程题、报告类作业仍需要教师复核。后续可接入 LLM 或代码判题服务完成智能评分、反馈生成和学习推荐。

6. **前端 UI 美化**：当前前端功能可用但样式仍偏管理后台原型，需整体润色排版、配色、间距等视觉效果。

7. **测试环境解耦远程数据库**：`mvn test` 当前依赖远程 TiDB Cloud，网络不可用时会失败。建议增加 `test` profile，使用 H2 或本地测试库。

## 注意事项

1. **数据库延迟**：数据库部署在 TiDB Cloud（新加坡节点），网络延迟较高。首次加载或重启后查询可能需等待 10~30 秒，页面出现 loading 属正常现象，不是数据丢失或接口损坏。前端 Axios 超时时间已调为 30 秒。

2. **先登录再操作**：所有功能页面需要先登录获取 session 后才能正常访问。如果直接通过 URL 跳转到 `/courses`、`/stats` 等内页而未登录，接口会返回 `请先登录` 或显示空白/报错，属于预期行为。

3. **课程与教师的关系**：指导老师对"教师对课程是一对一还是一对多"未明确说明。当前后端 `Course.teacher` 存教师姓名，天然支持多对多，若后续要求改为一对一，基本只改前端即可，后端无需大动。

4. **在线测验类型**：发布测验时任务类型应使用 `quiz`，前端展示为“在线测验”。只有 `quiz` 类型会进入在线答题、题目绑定和系统评阅流程。

5. **组卷不是强约束考试系统**：当前组卷是可用版策略抽题。如果符合条件题目不足，可能返回少于目标数量的题。暂未实现固定总分、按题型精确配比、题目去重策略配置和试卷版本管理。

6. **底层代码不是死的**：现有的 Controller、Service、Mapper 只是搭了个骨架，不是不能动。如果觉得结构不合理、方法多余、或者有更好的写法，直接改，不用问我。这些代码是参考，不是约束。

## 代码规范

1. **别跨层**：Controller → Service → Mapper，各调各的。Controller 别直接调 Mapper，也别注别人的 Service。比如 CourseController 要用 Lesson 的数据，应该走 CourseService 去委托 LessonService，而不是 Controller 自己注一个 LessonService 进来。

2. **Service 能不写就不写**：IService 自带增删改查了（`getById` `save` `list` 这些），别写重复的包装方法。只有自定义 SQL（模糊查 LIKE、按外键查）和业务逻辑（登录、注册）才需要声明。

3. **我自己也有漏的**：上面两条我也没全部遵守，有些地方可能偷懒直接注了别的 Service。翻到直接改。

## 资源目录说明

| 目录 | 内容 |
|------|------|
| `backend/src/main/resources/` | mapper XML、`application.yml`、`schema.sql`、`data.sql`，放项目配置和 SQL |
| `resource/`（项目根） | 上传的课件、作业附件、课程封面、测试图片。`WebConfig` 映射到 `/resource/**` 对外访问。里面 `red1.png` `white1.png` `python.png` 是占位玩的，以后换了就行 |
