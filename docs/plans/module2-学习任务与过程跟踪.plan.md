# 模块2：学习任务与过程跟踪 — 实施计划

> 一句话定位：将 `specs/宋芷萱/模块2-学习任务与过程跟踪-需求分析.md` 的 EARS 需求拆成按依赖排序、可独立验证的实施任务，每阶段有检查点。
> 阅读前置：[`plan-writing-guide.md`](./plan-writing-guide.md)、[`../../specs/宋芷萱/模块2-学习任务与过程跟踪-需求分析.md`](../../specs/宋芷萱/模块2-学习任务与过程跟踪-需求分析.md)、[`../architecture/模块接口与协作规范.md`](../architecture/模块接口与协作规范.md)
> 状态：草拟

---

## Context（背景）

模块2（学习任务与过程跟踪）是5个模块中的教学活动枢纽。当前代码库已有基础版 LearningTask CRUD + TaskSubmission CRUD + Quiz答题系统，但只覆盖了约40%的需求。

本次实施需要在**保留现有代码架构**的前提下，增量升级数据库、完善后端API、重构前端页面，补齐 3.2.1~3.2.9 的完整功能链路。

## Goals（目标）

1. 数据库升级：LearningTask 增加8个字段，TaskSubmission 增加2个字段，新建 learning_behavior_log 表
2. 后端API从4个核心接口扩展到~15个，覆盖筛选、状态管理、行为日志、进度统计
3. 前端从3个任务相关页面扩展到5个，每种任务类型有差异化交互
4. 实现行为日志采集，覆盖视频/PPT/PDF/测验/资源/报告6类行为
5. 教师端全班进度矩阵，一眼识别落后学生

## Non-Goals（不做）

- 不修改现有鉴权机制（Auth.java）
- 不重构现有前端路由架构
- 不实现AI相关功能（属于模块3/4/5+agentic）
- 不修改Course/Lesson/Student的现有CRUD

## 回引规格

| 需求组 | 对应功能 | 规格编号 |
|--------|---------|---------|
| R1 | 任务发布、修改、删除、筛选、状态管理 | R1.1–R1.8 |
| R2 | 逾期设置、提交次数、格式校验、状态判定 | R2.1–R2.4 |
| R3 | 多类型提交（测验/报告/实践/视频/阅读） | R3.1–R3.7 |
| R4 | 完成状态记录、学生/教师查看 | R4.1–R4.3 |
| R5 | 行为日志采集、查询 | R5.1–R5.4 |
| R6 | 完成率/平均时长/逾期次数/资源访问统计 | R6.1–R6.4 |
| R7 | 学生进度概览、教师全班进度矩阵 | R7.1–R7.3 |

## 架构决策

| 决策 | 理由 |
|------|------|
| 不使用 UUID 主键，保持 INT AUTO_INCREMENT | 现有6张表全部用INT自增，保持一致 |
| 任务类型用字符串枚举而非独立子表 | 团队规模小，ENUM字符串+代码校验已够用 |
| 行为日志用单表而非按资源分表 | 查询需求跨资源类型，单表+索引性能足够 |
| 前端不引入新依赖 | 现有 Vue3+Element Plus+ECharts 已覆盖全部UI需求 |
| 文件复用 FileStorageService | 已存在统一文件存储，不重复造轮子 |

## 备选方案

| 方案 | 为何不选 |
|------|---------|
| 任务类型用独立实体表（TaskType + TaskTypeConfig） | 过度设计，5人团队不需要此复杂度 |
| 行为日志用时序数据库（InfluxDB） | 增加运维复杂度，TiDB MySQL足够 |
| 前端用 Pinia 做状态管理 | 当前页面间无需共享复杂状态，localStorage+props足够 |

## 假设

1. 现有6张表结构不变，只在 learning_task 和 task_submission 上 ADD COLUMN
2. TiDB Cloud 连接可用，支持 ALTER TABLE 和 CREATE TABLE
3. 前端 8080 端口代理到后端 8080 端口，无需修改 vue.config.js
4. knowledge_point 字段当前用 TEXT/JSON 字符串存储（模块1的知识点系统尚未构建）
5. 学生端和教师端共用 MainLayout，无需新建布局

---

## 任务清单

### 阶段1：数据库升级（地基）

- [ ] **T1 — LearningTask 实体和表升级**（满足 R1.1, R1.3, R1.6, R2.1, R2.2, R2.3）
  - 验收：① `schema.sql` 新增8个字段的 ALTER TABLE 语句可执行；② `LearningTask.java` 新增字段可编译；③ `data.sql` 测试数据更新
  - 验证：执行 schema.sql → 启动后端 → GET /task/{courseCode} 返回包含新字段
  - 依赖：无
  - 文件：`schema.sql`, `data.sql`, `LearningTask.java`
  - 规模：M

- [ ] **T2 — TaskSubmission 实体升级 + LearningBehaviorLog 新建**（满足 R3.5, R5.1, R5.2）
  - 验收：① TaskSubmission 新增 `attemptNumber`(INT)、`isOverdue`(TINYINT)；② `learning_behavior_log` 表建表成功；③ 两个新 Entity 可编译
  - 验证：执行建表SQL → `SHOW TABLES` 包含 learning_behavior_log → `DESC learning_behavior_log` 10个字段齐全
  - 依赖：无（与T1并行）
  - 文件：`schema.sql`, `TaskSubmission.java`, `LearningBehaviorLog.java`(新), `BehaviorLogMapper.java`(新)
  - 规模：M

- [ ] **T3 — BehaviorLog Service + Mapper XML**（满足 R5.2, R5.3）
  - 验收：① `BehaviorLogService` 含 `record(log)` 和 `query(filters)` 方法；② Mapper XML 含 insert + select 联合查询
  - 验证：单元测试插入一条日志 → 按 user_id 查询返回该日志
  - 依赖：T2
  - 文件：`BehaviorLogService.java`(新), `BehaviorLogServiceImpl.java`(新), `BehaviorLogMapper.java`(新), `BehaviorLogMapper.xml`(新)
  - 规模：M

#### 🔍 检查点：阶段1完成
- [ ] schema.sql 全部变更可执行，无语法错误
- [ ] 后端启动成功，3个Entity + Mapper + Service 就绪
- [ ] 人审通过再继续

---

### 阶段2：后端API增强（核心）

- [ ] **T4 — TaskController 增强**（满足 R1.1, R1.4, R1.5, R1.6, R1.7）
  - 验收：① POST /task 接受全部新字段（taskName、lessonNo、knowledgePoints、gradingRule、status、allowLate、maxAttempts、attachmentFormats）；② PUT 支持修改全部字段；③ DELETE 检查是否有提交记录并提示确认；④ GET 接口支持 status/type/lessonNo 筛选参数
  - 验证：Postman 测试：创建带所有新字段的任务 → 按类型筛选 → 修改状态为closed → 删除
  - 依赖：T1
  - 文件：`TaskController.java`, `LearningTaskService.java`, `LearningTaskServiceImpl.java`
  - 规模：M

- [ ] **T5 — TaskSubmissionController 增强**（满足 R2.1, R2.2, R2.3, R3.5, R3.6, R3.7）
  - 验收：① 提交时校验 allowLate，逾期且不允许则拒绝；② 校验 maxAttempts，超过则拒绝；③ 校验文件扩展名是否符合 attachmentFormats；④ 提交时自动填入 attemptNumber 和 isOverdue
  - 验证：创建不允许逾期任务 → 截止时间过后学生提交 → 返回"已截止"错误
  - 依赖：T1, T2
  - 文件：`TaskSubmissionController.java`, `TaskSubmissionService.java`, `TaskSubmissionServiceImpl.java`
  - 规模：M

- [ ] **T6 — BehaviorLogController 新建**（满足 R5.1, R5.3）
  - 验收：① POST /learning-logs 记录一条行为日志；② GET 支持按 studentNo/courseCode/actionType/startTime/endTime 筛选
  - 验证：前端上报一条 video_play 日志 → GET 查询返回该条日志
  - 依赖：T3
  - 文件：`BehaviorLogController.java`(新)
  - 规模：S

- [ ] **T7 — StatsController 增强**（满足 R6.1, R6.2, R6.3）
  - 验收：① 学生统计增加：completionRate（完成率）、totalStudyDuration（总学习时长）、overdueCount（逾期次数）；② 课程统计增加：perTaskCompletionRate（各任务完成率）、avgCompletionMinutes（平均完成时长）、overdueStudentCount（逾期人数）
  - 验证：GET /stats/student/{no} 返回 completionRate、totalStudyDuration、overdueCount 三个新字段
  - 依赖：T1, T2, T3
  - 文件：`StatsController.java`, `StatsService.java`, `StatsServiceImpl.java`
  - 规模：M

- [ ] **T8 — ProgressController 新建**（满足 R7.1, R7.2, R7.3）
  - 验收：① GET /progress/student/{studentNo}?courseCode=X 返回：任务完成数/总数、完成率、每个任务的状态列表；② GET /progress/course/{courseCode} 返回：每个学生的完成数、完成率、最后活跃时间、逾期次数；③ 自动标注落后学生（完成率<50%）和逾期学生（逾期≥2次）
  - 验证：教师账号调用 /progress/course/1 → 返回全班学生进度矩阵
  - 依赖：T1, T2, T3, T7
  - 文件：`ProgressController.java`(新), `ProgressService.java`(新), `ProgressServiceImpl.java`(新)
  - 规模：L

#### 🔍 检查点：阶段2完成
- [ ] 全部10个新API端点可调用且返回正确数据
- [ ] 任务筛选支持4种条件组合
- [ ] 逾期检测、提交次数限制、文件格式校验均生效
- [ ] 行为日志写入+查询跑通
- [ ] 进度接口返回准确统计数据
- [ ] 人审通过再继续

---

### 阶段3：前端页面重构（界面）

- [ ] **T9 — TaskList.vue 重构**（满足 R1.7, R1.8, R1.4, R1.5）
  - 验收：① 顶部增加筛选栏：课程下拉、类型下拉、状态下拉、时间范围；② 教师端每行增加"编辑"和"删除"按钮；③ 学生端每行显示任务状态标签（待完成/已提交/已逾期/已完成）；④ 任务列表支持分页
  - 验证：教师登录 → 筛选"测验"类型 → 点击编辑 → 修改截止时间 → 保存成功 → 列表刷新
  - 依赖：T4（后端API就绪）
  - 文件：`TaskList.vue`, `api/index.js`（新增 updateTask/deleteTask 封装）
  - 规模：L

- [ ] **T10 — TaskSubmit.vue 重构**（满足 R3.1, R3.2, R3.3, R3.4, R2.4）
  - 验收：① 测验类自动跳转到 QuizTake.vue（已有，保持）；② 报告/文档类显示文件上传区+格式提示；③ 实践类显示文件上传+文字说明；④ 视频/阅读类不显示提交表单，显示"系统将自动记录学习进度"；⑤ 顶部显示任务状态和剩余时间
  - 验证：学生登录 → 打开报告类任务 → 上传非 .pdf 文件 → 被拒绝 → 上传 .pdf → 提交成功
  - 依赖：T5
  - 文件：`TaskSubmit.vue`
  - 规模：M

- [ ] **T11 — ProgressView.vue 新建**（满足 R7.1, R7.2, R7.3）
  - 验收：① 学生端：课程进度环图（完成数/总数）+ 各任务状态列表 + 最近学习时间线；② 教师端：课程选择器 + 学生×任务完成矩阵表 + 落后学生高亮（红色）+ 逾期学生高亮（橙色）
  - 验证：教师登录 → 选择课程 → 看到全班矩阵表，红色标注的学生一眼可见
  - 依赖：T8
  - 文件：`ProgressView.vue`(新), `api/index.js`（新增 getStudentProgress/getCourseProgress）
  - 规模：L

- [ ] **T12 — StatsView.vue 增强**（满足 R6.1, R6.2, R6.3, R6.4）
  - 验收：① 学生端增加：完成率进度条、总学习时长、逾期次数卡片；② 教师端增加：各任务完成率柱状图、资源访问频次排名；③ 所有图表使用 ECharts
  - 验证：学生登录 → 统计页 → 看到完成率60%、学习时长2.5h、逾期1次
  - 依赖：T7
  - 文件：`StatsView.vue`
  - 规模：M

- [ ] **T13 — 路由和导航更新**（满足整体导航）
  - 验收：① 路由新增 `/progress` → ProgressView；② 学生侧边栏新增"学习进度"菜单项；③ 教师侧边栏新增"学习进度"菜单项
  - 验证：学生登录 → 侧边栏看到"学习进度" → 点击跳转到 ProgressView
  - 依赖：T11
  - 文件：`router/index.js`, `MainLayout.vue`, `api/index.js`
  - 规模：XS

#### 🔍 检查点：阶段3完成
- [ ] 所有前端页面加载无白屏/报错
- [ ] 筛选器、编辑删除、状态标签交互正常
- [ ] 不同任务类型展示不同提交界面
- [ ] 教师端全班进度矩阵数据正确、异常学生高亮
- [ ] 统计数据图表展示正确
- [ ] 人审通过再继续

---

### 阶段4：行为日志集成 + 联调

- [ ] **T14 — 前端行为日志采集**（满足 R5.1, R5.4）
  - 验收：① LessonDetail.vue 中视频播放时每30秒调用 POST /learning-logs 上报播放进度；② 暂停/跳转时上报日志；③ PPT/PDF浏览时上报打开和关闭日志；④ TaskDetail.vue 页面上报查看日志
  - 验证：学生播放视频 → 等待30秒 → 查询日志接口 → 看到 video_play 日志
  - 依赖：T6（后端日志接口）, T11
  - 文件：`LessonDetail.vue`, `TaskDetail.vue`（小幅修改）, `api/index.js`（新增 reportBehaviorLog）
  - 规模：M

- [ ] **T15 — 端到端集成测试**（满足全部验收标准 AC1~AC10）
  - 验收：按验收标准逐项测试通过
  - 验证：教师发布任务 → 学生提交 → 行为日志产生 → 进度/统计更新 → 教师查看全班进度
  - 依赖：T1~T14 全部完成
  - 文件：无需改动代码，手工测试+记录
  - 规模：M

- [ ] **T16 — 边界情况修复 + 文档更新**（满足 AC10）
  - 验收：① 空数据状态（无任务、无提交）不报错；② 网络异常有友好提示；③ 更新 `references/ITERATION_LOG.md`；④ 更新 `specs/README.md` 路线表
  - 验证：清空任务数据 → 前端无白屏 → 显示"暂无任务"
  - 依赖：T15
  - 文件：`ITERATION_LOG.md`, `specs/README.md`
  - 规模：S

#### 🔍 检查点：阶段4完成（最终检查点）
- [ ] 全流程12个核心场景全部走通
- [ ] 行为日志正确记录并能查询
- [ ] 视频播放进度追踪准确
- [ ] 所有空数据/异常网络场景有兜底
- [ ] `specs/README.md` 路线表更新
- [ ] 人审通过 → 完成 ✅

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| TiDB Cloud ALTER TABLE 锁表 | 数据库不可用 | 在本地/测试环境先验证SQL，选择低峰期执行 |
| 前端重构导致已有页面不可用 | 开发期其他成员无法演示 | 新建页面不删除旧页面，路由渐进切换 |
| 行为日志量过大影响查询性能 | 统计接口响应慢 | 日志表加 (user_id, created_at) 联合索引；统计走聚合查询 |
| 模块1知识点系统未就绪 | 任务无法关联知识点 | knowledgePoints 用 TEXT JSON 字符串存储，模块1就绪后迁移 |

## Open Questions

1. 知识点关联字段当前用 TEXT 存 JSON 数组（如 `["kp_001","kp_002"]`），模块1就绪后是否迁移为关联表？→ [待定]
2. 视频播放30秒上报间隔是否合适？是否需要可配置？→ 先写死30秒，后续可变
3. 任务类型 `video`/`reading` 类型不设手动提交，系统如何判断"完成"？→ 初版：视频播放≥90%标记完成，PPT/PDF打开即标记已完成

## 越权红线

- ❌ 不改动 Auth.java 鉴权逻辑
- ❌ 不改动其他模块的表结构（course/lesson/student/teacher/question）
- ❌ 不物理删除已提交的记录
- ❌ 不修改前端路由守卫逻辑
- ❌ 不引入新的前端npm依赖

## 完成定义（DoD）

- [ ] AC1~AC10 全部通过验收
- [ ] 教师能创建含全部新字段的任务并发布
- [ ] 学生能按任务类型看到差异化的提交界面
- [ ] 行为日志正确记录视频、PPT、PDF、测验、资源5类行为
- [ ] 教师端全班进度矩阵显示正确
- [ ] 统计数据（完成率、平均时长、逾期率）准确无误
- [ ] 所有页面在空数据状态下不报错

## 端到端验证

```bash
# 1. 启动后端
cd backend && mvn spring-boot:run

# 2. 启动前端
cd frontend && npm run serve

# 3. 核心场景测试
# 场景A：教师发布任务全流程
#  - 教师登录 → 进入课程 → 发布新任务（填写全部字段）→ 筛选任务 → 编辑任务 → 切换状态为"已关闭"
#
# 场景B：学生提交多类型任务
#  - 学生登录 → 查看任务列表（看到不同状态标签）→ 测验类答题 → 报告类上传PDF → 实践类上传ZIP
#
# 场景C：逾期检测
#  - 创建不允许逾期任务、截止时间设为过去 → 学生提交 → 返回"已截止"
#
# 场景D：行为日志
#  - 学生播放视频30秒 → 查询日志API → 看到video_play日志
#
# 场景E：学习进度
#  - 教师登录 → 进入学习进度 → 看到全班学生×任务矩阵 → 落后学生被红色标注
#
# 场景F：数据统计
#  - 学生登录 → 进入统计 → 看到完成率、平均分、学习时长、逾期次数
```
