# Plan：模块5 — 学情分析与教学决策

> **一句话定位**：将[模块5需求文档](../../../specs/毛瑞琪/模块5-学情分析与教学决策-需求文档.md)的 8 组 EARS 需求（R1~R8）拆分为 11 个按依赖排序的原子任务，分 4 阶段交付——从班级 CRUD 地基到 AI 驱动的共性问题聚类与教学建议，最终集成教师端仪表盘。
> **阅读前置**：[模块5需求文档](../../../specs/毛瑞琪/模块5-学情分析与教学决策-需求文档.md)（需求定义）、[模块接口与协作规范](../../../specs/模块接口与协作规范.md)（上下游接口契约）、[plan-writing-guide.md](../plan-writing-guide.md)（计划写作规范）
> **状态**：草拟

---

## Context（背景）

模块5（学情分析与教学决策）是 AI 智慧课程平台的教师"驾驶舱"。它不产生原始学习数据，而是从模块1~4汇聚数据，通过聚合计算、规则引擎和 agentic LLM 推理，为教师提供：

- **班级管理**：创建班级、管理学生归属
- **成绩分析**：班级成绩总览、趋势图、知识点薄弱排名
- **进度监控**：班级/个人学习进度、任务完成率
- **风险预警**：7 类自动风险检测 + 游戏引擎事件接收
- **共性问题聚类**：错题/反馈语义聚类 → 班级高频问题
- **教学建议**：LLM 驱动的教学调整建议和个别干预方案
- **报表导出**：Excel 成绩报表 + PDF 综合分析报告

模块5 的核心约束：**只读消费**模块1~4数据，**不自动执行**任何教学干预，所有建议的最终决策权在教师。

---

## Goals（做什么）

- 教师能创建/管理班级，通过 ClassID 与其他模块互通
- 教师能查看班级成绩总览、成绩趋势、知识点薄弱排名
- 教师能监控班级和个人的学习进度与任务完成率
- 系统自动检测 7 类学习风险并在教师端高亮预警
- 系统接收模块4游戏事件（hp_critical/stuck/inactive）并转为风险预警
- 教师一键触发共性问题聚类，agentic 返回按主题分组的错题/反馈
- 教师一键获取教学调整建议和个别学生干预方案
- 教师能导出 Excel 成绩报表和 PDF 综合分析报告
- 所有 agentic 调用有 fallback，LLM 不可用时功能降级但不崩溃

---

## Non-Goals（明确不做）

- ❌ 不持有学生/课程/任务/试题的写入权（只读消费）
- ❌ 不自动执行教学干预（不自动发通知、调整任务）
- ❌ 不做独立的消息推送系统（邮件/短信/App Push）
- ❌ 不做教师之间的班级共享/协管
- ❌ 不做学生端学情展示（由模块4负责）
- ❌ 不做跨课程对比分析
- ❌ 不做自定义报表设计器（格式固定）
- ❌ 不做实时聊天/即时通讯

---

## 回引规格

| 来源 | 文档 |
|------|------|
| 需求文档 | `specs/毛瑞琪/模块5-学情分析与教学决策-需求文档.md` |
| 项目总需求 | `specs/项目需求.md`（基础5、创新5-7） |
| 模块接口契约 | `specs/模块接口与协作规范.md` 第八节（模块5） + 第十四节（游戏事件系统） |
| 覆盖需求编号 | R1.1–R1.8, R2.1–R2.5, R3.1–R3.4, R4.1–R4.9, R5.1–R5.6, R6.1–R6.6, R7.1–R7.4, R8.1–R8.5（共 44 条） |

---

## 架构决策

| 决策 | 理由 |
|------|------|
| 风险检测用**确定性规则引擎**，不用 LLM | 7 类风险全部可量化判定，规则可审计可调试；LLM 仅做复杂场景兜底（agentic `/risk-detect`） |
| 聚类/建议结果**持久化存储**（AnalyticsReport / TeachingSuggestion 表） | 教师可回查历史报告，避免重复调用 agentic |
| 报表导出使用 **Apache POI + iText** 后端生成 | 纯 Java 库，与 Spring Boot 生态一致；前端只需下载文件流 |
| 风险检测**定时 + 手动双触发** | 定时（每日凌晨）保证不遗漏；手动允许教师即时刷新 |
| 游戏事件接收用**模块4 → 模块5 的同步 REST 调用** | 同进程内模块间 Service 调用更快，但游戏事件来自模块4内部引擎，用 REST 解耦更好（避免循环依赖）；模块5暴露 `POST /api/risk-alerts` |
| 所有外部模块数据调用走 **Service 接口注入**，Phase 1-2 用 Mock | 遵循 §12.2 的 Mock 先行原则，不等其他模块开发完 |

---

## 备选方案

| 方案 | 优点 | 为何不选 |
|------|------|---------|
| 风险检测全部用 LLM | 语义理解更强 | LLM 不稳定、延迟高、结果不可复现；7 类风险有明确的量化规则 |
| 报表用前端生成（jsPDF + SheetJS） | 减轻后端负担 | 数据量可能大，前端处理性能差；后端生成可复用同一份聚合逻辑 |
| 聚类结果不存储，每次实时计算 | 节省存储 | agentic 调用昂贵，历史数据丢失不利于教学复盘 |
| 模块4通过进程内 Service 调用模块5 | 更快、无网络开销 | 可能导致模块4→模块5的循环依赖；REST 解耦更清晰 |

---

## 假设

以下假设如不正确，请在实施前纠正：

1. **金仓数据库 6/24 培训后可正常使用**；Phase 1 期间开发环境用 MySQL 过渡，MyBatis-Plus 兼容两者。
2. **模块2/3/4 在 Phase 1 结束时提供可用的 Service 接口**（至少 Mock 实现），模块5 在 Phase 2 可依赖它们。
3. **agentic 服务在 Phase 3 启动前就绪**（至少 `/cluster-problems` 和 `/teaching-suggestions` 两个端点可用）。
4. **教师端前端由专人负责**，模块5 仅提供 REST API + JSON 数据，不涉及前端 UI 开发。
5. **项目脚手架已就绪**（Spring Boot 3.5.15 + MyBatis-Plus + JWT 鉴权），模块5 在现有工程中新增 package 即可。
6. **班级规模 ≤ 100 学生/班**，初期无需考虑大数据量分页/缓存优化。
7. **开发期 game_mode_enabled = false**，游戏事件相关接口可测试但不影响正常流程。

→ 不纠正则按上述假设推进。

---

## 任务清单

### 阶段 1：地基（Week 1-2）

> **目标**：模块5 可启动，Class 表可 CRUD，RiskAlert 表可接收游戏事件。ClassID 对其他模块可用。

- [ ] **T1 — 搭建模块5骨架 + Class 表 + 班级 CRUD**（满足 R1.1–R1.8）
  - 验收：
    1. `analytics_class` 表创建成功（金仓/MySQL），4 个字段完整
    2. `POST /api/classes` 创建班级返回 201 + ClassID（UUID）
    3. `GET /api/classes?teacher_id=` 返回教师班级列表
    4. `POST /api/classes/{id}/enroll` 添加学生成功，`DELETE /api/classes/{id}/students/{sid}` 移除成功
    5. 重复班级名拒绝创建（409），有学生时删除班级拒绝（409）
  - 验证：
    - `mvn test -pl . -Dtest=ClassControllerTest`（创建/查询/注册/删除/边界条件各一条）
    - Postman：`POST /practical-training/api/classes` → 201 → `GET /practical-training/api/classes` → 200 含新班级
  - 依赖：无（模块5 自身实体，不依赖其他模块）
  - 文件（≤6）：
    - `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/entity/Class.java`
    - `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/mapper/ClassMapper.java`
    - `backend/src/main/resources/mapper/module5_analytics/ClassMapper.xml`
    - `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/ClassService.java` + `impl/`
    - `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/controller/ClassController.java`
  - 规模：**M**

- [ ] **T2 — RiskAlert 表 + 游戏事件接收端点**（满足 R4.8, R4.9, R8.1–R8.5）
  - 验收：
    1. `analytics_risk_alert` 表创建成功，含 `risk_type` / `risk_level` / `status` 枚举字段
    2. `POST /api/risk-alerts` 接收模块4事件，写入 RiskAlert 记录并返回 201
    3. `GET /api/students/{id}/risk-status` 返回该生所有未解除预警的类型列表和最高风险等级
    4. 同类型重复预警不重复创建（唯一约束：student_id + risk_type + status='active'）
  - 验证：
    - `mvn test -Dtest=RiskAlertControllerTest`（事件接收/去重/状态查询各一条）
    - Postman：`POST /practical-training/api/risk-alerts` body `{student_id, course_id, risk_type:"hp_critical", detail_json:{...}}` → 201
  - 依赖：无（模块5 自身实体；`student_id` 和 `course_id` 仅做格式校验，不查其他模块）
  - 文件（≤5）：
    - `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/entity/RiskAlert.java`
    - `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/mapper/RiskAlertMapper.java` + XML
    - `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/RiskAlertService.java` + `impl/`
    - `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/controller/RiskAlertController.java`
  - 规模：**M**

### 🔍 检查点：阶段 1 完成

- [ ] T1 + T2 全部验收通过
- [ ] 单元测试全部通过（`mvn test -Dtest="ClassControllerTest,RiskAlertControllerTest"`）
- [ ] 应用可启动（`mvn spring-boot:run`），无报错
- [ ] ClassID（UUID）可被其他模块引用
- [ ] 人审通过再进入阶段 2

---

### 阶段 2：核心业务（Week 3-5）

> **目标**：无 AI 加持的核心分析功能跑通——成绩分析、进度监控、风险检测、报表导出。依赖模块2/3/4 提供数据。

- [ ] **T3 — 班级成绩总览 + 分数分布 + 薄弱知识点排名**（满足 R2.1, R2.4, R2.5）
  - 验收：
    1. `GET /api/classes/{id}/scores` 返回聚合数据：平均分/最高分/最低分/标准差/各分数段人数/学生排名（调用模块3 `getStudentScores` + 模块4 `getStudentsByClass`）
    2. `GET /api/classes/{id}/score-distribution` 返回 5 段分布 `[{range:"0-59", count:N}, ...]`
    3. `GET /api/classes/{id}/weak-points` 返回各知识点得分率，从低到高排序（调用模块3 `getClassMistakeStats` + 模块1 `getKnowledgePointsByCourse`）
    4. 所有查询在 2 秒内返回（Postman 响应时间断言）
  - 验证：
    - `mvn test -Dtest=ScoreAnalysisServiceTest`（Mock 模块3/4 数据，验证聚合计算正确）
    - Postman：`GET /practical-training/api/classes/{id}/scores` → 200 + JSON 含 `avgScore`, `distribution`, `rankings[]`
  - 依赖：模块3 `getStudentScores()` / `getClassMistakeStats()`，模块4 `getStudentsByClass()`，模块1 `getKnowledgePointsByCourse()` → Phase 1 期间用 Mock
  - 文件（≤5）：
    - `module5_analytics/service/ScoreAnalysisService.java` + `impl/`
    - `module5_analytics/controller/ScoreController.java`
    - `module5_analytics/dto/ScoreOverviewDTO.java`
    - `module5_analytics/dto/ScoreDistributionDTO.java`
    - `module5_analytics/dto/WeakPointDTO.java`
  - 规模：**M**

- [ ] **T4 — 成绩趋势图 + 学生个人成绩对比**（满足 R2.2, R2.3）
  - 验收：
    1. `GET /api/classes/{id}/score-trends?granularity=week` 返回按周聚合的班级均分序列 `[{label:"W1", avgScore:72}, ...]`
    2. `GET /api/classes/{id}/score-trends?student_id={id}` 返回学生个人得分序列 + 班级均分对比线数据
    3. 支持 `granularity=exam` 按考试节点聚合
  - 验证：
    - `mvn test -Dtest=ScoreTrendServiceTest`（Mock 时间序列数据，验证聚合粒度正确）
  - 依赖：T3（共用 ScoreAnalysisService 的模块3数据获取层）
  - 文件（≤4）：
    - `module5_analytics/service/ScoreTrendService.java` + `impl/`
    - `module5_analytics/controller/ScoreController.java`（追加端点）
    - `module5_analytics/dto/ScoreTrendDTO.java`
  - 规模：**S**

- [ ] **T5 — 班级进度监控 + 任务完成率**（满足 R3.1–R3.4）
  - 验收：
    1. `GET /api/classes/{id}/progress` 返回班级平均完成率 + 进度落后学生列表（低于平均 20% 以上）
    2. `GET /api/classes/{id}/task-completion?task_id={id}` 返回已提交/未提交/延迟提交人数和名单
    3. 数据可手动刷新（`?refresh=true`），默认返回 5 分钟内缓存
  - 验证：
    - `mvn test -Dtest=ProgressServiceTest`（Mock 模块2 数据，验证落后判定阈值和缓存逻辑）
  - 依赖：模块2 `getStudentProgress()` / `getTaskCompletionStats()`，模块4 `getStudentsByClass()`
  - 文件（≤5）：
    - `module5_analytics/service/ProgressService.java` + `impl/`
    - `module5_analytics/controller/ProgressController.java`
    - `module5_analytics/dto/ClassProgressDTO.java`
    - `module5_analytics/dto/TaskCompletionDTO.java`
  - 规模：**M**

- [ ] **T6 — 风险检测引擎 + 预警展示 + 标记已处理**（满足 R4.1–R4.7）
  - 验收：
    1. 7 类风险规则全部实现并通过单元测试（每类至少 2 条测试：命中 + 不命中）
    2. `POST /api/classes/{id}/risk-detect` 手动触发：遍历全班学生 → 拉取模块2/3/4数据 → 逐规则判定 → 生成 RiskAlert（去重）
    3. `GET /api/classes/{id}/risk-alerts?status=active&level=high` 返回高→低排序的预警列表，高风险红色标记
    4. `PUT /api/risk-alerts/{id}/resolve` 标记已处理，处理后不出现在默认列表
    5. 同学生多规则命中时合并为一条综合预警（`detail` 含全部命中类型）
  - 验证：
    - `mvn test -Dtest=RiskDetectionServiceTest`（每类规则独立测试，同生多规则合并测试，去重测试）
    - Postman：`POST /api/classes/{id}/risk-detect` → 200 + `[RiskAlert...]`
  - 依赖：模块2 `getStudentLogs()` / `getStudentProgress()`，模块3 `getStudentScores()`，模块4 `getStudentProfile()` / `getStudentsByClass()`，T2（RiskAlert 表已就绪）
  - 文件（≤6）：
    - `module5_analytics/service/RiskDetectionService.java` + `impl/`
    - `module5_analytics/service/risk_rules/RiskRule.java`（接口）
    - `module5_analytics/service/risk_rules/ProcrastinationRule.java`
    - `module5_analytics/service/risk_rules/LowScoreRule.java`
    - `module5_analytics/service/risk_rules/InactiveRule.java`
    - `module5_analytics/service/risk_rules/ProgressLagRule.java`
    - `module5_analytics/service/risk_rules/ScoreDeclineRule.java`
    - `module5_analytics/controller/RiskAlertController.java`（追加 detect / resolve 端点）
  - 规模：**L**（风险最高，7 条独立规则 + 合并逻辑 + 去重；建议每个规则用策略模式独立文件，可单独测试）

  > ⚠️ **高风险任务（fail fast）**：T6 是整个模块5 最复杂的业务逻辑。如果规则判定结果与预期偏差大，需要尽早发现调整。**建议 T6 在阶段 2 最优先启动**，不等 T3-T5 全部完成。

- [ ] **T7 — 报表导出（Excel + PDF）**（满足 R7.1–R7.4）
  - 验收：
    1. `POST /api/classes/{id}/reports/export` body `{format:"excel", report_type:"scores"}` 返回 Excel 文件流（3 Sheet：成绩总表/分布统计/知识点得分率）
    2. `POST /api/classes/{id}/reports/export` body `{format:"pdf", report_type:"full_analysis"}` 返回 PDF 文件流（含成绩+进度+预警+薄弱知识点+建议）
    3. `GET /api/reports/{id}/download` 可重新下载 30 天内的历史报表
  - 验证：
    - `mvn test -Dtest=ReportExportServiceTest`（验证 Excel POI 生成 + PDF iText 生成 + 历史查询）
  - 依赖：T3（成绩数据就绪）、T5（进度数据就绪）、Apache POI + iText 依赖已加入 `pom.xml`
  - 文件（≤5）：
    - `module5_analytics/service/ReportExportService.java` + `impl/`
    - `module5_analytics/controller/ReportController.java`
    - `module5_analytics/entity/AnalyticsReport.java`（复用该表存储导出记录）
  - 规模：**M**

### 🔍 检查点：阶段 2 完成

- [ ] T3–T7 全部验收通过
- [ ] 全部单元测试通过（`mvn test -pl .` 模块5 包下所有测试）
- [ ] 核心流程端到端可走通：创建班级 → 添加学生 → 查询成绩总览 → 查看进度 → 检测风险 → 导出报表
- [ ] Mock Service 全部替换为真实模块调用（如对应模块已就绪）或确认 Mock 行为与契约一致
- [ ] 人审通过再进入阶段 3

---

### 阶段 3：AI 智能（Week 6-8）

> **目标**：接入 agentic 服务，共性问题聚类和教学建议上线。本阶段真正的阻塞项是 agentic 服务可用时间。

- [ ] **T8 — 共性问题聚类**（满足 R5.1–R5.6）
  - 验收：
    1. `POST /api/classes/{id}/problem-cluster` 触发后：汇聚模块3 错题数据 + 模块2 反馈文本 → 调用 agentic `/api/agent/cluster-problems` → 结果存入 `analytics_report` 表 → 返回聚类列表
    2. `GET /api/classes/{id}/problem-cluster` 返回最新聚类报告（含 `topic`, `count`, `knowledge_point_ids`, `typical_cases`, `severity`）
    3. agentic 不可用时返回 `{error:"AI_SERVICE_UNAVAILABLE", message:"AI 服务暂不可用，请稍后重试"}`，HTTP 503，**系统不崩溃**
    4. 聚类结果可在 `analytics_report` 表中按时间回查历史
  - 验证：
    - `mvn test -Dtest=ProblemClusterServiceTest`（Mock agentic 成功/失败/超时 3 种情况）
    - Postman：`POST /practical-training/api/classes/{id}/problem-cluster` → 200 + clusters[]
  - 依赖：agentic `/api/agent/cluster-problems` 可用，模块3 `getClassMistakeStats()` / `getStudentMistakes()`，模块2 `getStudentSubmissions()`
  - 文件（≤4）：
    - `module5_analytics/service/ProblemClusterService.java` + `impl/`
    - `module5_analytics/controller/ClusterController.java`
    - `module5_analytics/dto/ClusterResultDTO.java`
  - 规模：**M**

- [ ] **T9 — 教学建议 + 个别干预建议**（满足 R6.1–R6.6）
  - 验收：
    1. `POST /api/classes/{id}/teaching-suggestions` 触发后：汇聚薄弱知识点 + 最新聚类结果 + 进度数据 + 预警数量 → 调用 agentic `/api/agent/teaching-suggestions` → 结果存入 `analytics_teaching_suggestion` 表 → 返回建议列表（含 `suggestion_type`, `content`, `target`, `urgency`, `based_on`）
    2. `POST /api/students/{id}/intervention` 触发后：汇聚该生画像 + 成绩序列 + 学习日志 + 风险状态 → agentic 返回针对性辅导建议
    3. `GET /api/classes/{id}/teaching-suggestions` 返回历史建议列表
    4. agentic 不可用时同 T8 返回 503 + 友好错误提示
    5. 系统不自动执行任何干预操作
  - 验证：
    - `mvn test -Dtest=TeachingSuggestionServiceTest`（Mock agentic 成功/失败，验证字段完整性）
  - 依赖：agentic `/api/agent/teaching-suggestions` 可用，T6（风险检测数据），T8（聚类结果），模块1 `getWeakPoints()`
  - 文件（≤5）：
    - `module5_analytics/service/TeachingSuggestionService.java` + `impl/`
    - `module5_analytics/controller/SuggestionController.java`
    - `module5_analytics/entity/TeachingSuggestion.java`（如 T2 未创建）
    - `module5_analytics/dto/TeachingSuggestionDTO.java`
  - 规模：**M**

### 🔍 检查点：阶段 3 完成

- [ ] T8 + T9 全部验收通过
- [ ] agentic 成功/失败/超时三种情况均有测试覆盖
- [ ] agentic 不可用时模块5 不崩溃（所有功能降级返回错误提示而非 500）
- [ ] 聚类结果和建议结果可持久回查
- [ ] 人审通过再进入阶段 4

---

### 阶段 4：联调与收尾（Week 9-10）

> **目标**：教师端仪表盘 API 对齐、全链路集成测试、Bug 修复、文档补齐。

- [ ] **T10 — 教师端仪表盘 API 对齐 + 反馈汇总**（满足 R5.6）
  - 验收：
    1. 与前端负责人逐接口对齐：请求参数名/返回字段名/错误码格式全部确认
    2. `GET /api/classes/{id}/feedback-summary` 可用（调用模块2 获取学生提交中的反馈/提问文本，按时间排序）
    3. 所有 API 的 Swagger/OpenAPI 注解完整（或提供 Postman Collection）
  - 验证：
    - 前端对接人可以独立通过 Postman Collection 调通所有 22 个接口
  - 依赖：前序所有任务完成
  - 文件（≤4）：
    - `module5_analytics/controller/FeedbackController.java`（追加 feedback-summary）
    - Postman Collection 文件 `docs/api/module5-api.postman_collection.json`
    - 各 Controller 追加 Swagger `@Operation` 注解
  - 规模：**M**

- [ ] **T11 — 全链路集成测试 + Bug 修复**（满足全部 R1–R8）
  - 验收：
    1. 端到端场景全部走通（见下方验证步骤）
    2. 无阻断性 Bug（P0 = 0）
    3. 全部 44 条需求（R1.1–R8.5）逐条验收通过
  - 验证：
    - 场景1：教师创建班级 → 添加学生 → 学生在模块2提交任务 → 模块3评分 → 教师查看班级成绩总览 + 趋势图 → 导出 Excel 报表
    - 场景2：模拟学生低分/拖延/不活跃 → `POST /risk-detect` → 教师查看预警列表（高中低排序）→ 标记已处理
    - 场景3：教师触发共性问题聚类 → 查看聚类结果 → 生成教学建议 → 查看历史建议
    - 场景4：模拟模块4发送 `hp_critical` 事件 → 模块5 创建预警 → `GET /risk-status` 返回预警
    - 场景5：agentic 服务不可用 → 聚类/建议接口返回 503 → 成绩/进度/风险功能正常
    - `mvn test` 全量测试通过
  - 依赖：前序所有任务 + 模块2/3/4/agentic 全部就绪
  - 文件：`module5_analytics/` 下所有文件（≤~20）
  - 规模：**L**

### 🔍 检查点：阶段 4 完成 → 整体完成

- [ ] 全部 11 个任务勾选完成
- [ ] `mvn test` 全量通过，覆盖率 ≥ 70%（模块5 包）
- [ ] 与依赖模块（模块1/2/3/4）的接口联调通过
- [ ] 前端对接人确认 API 可用
- [ ] 全部 44 条需求逐条验收通过
- [ ] 人审通过 → 标记本 plan 为"已完成"

---

## 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| 模块2/3/4 Phase 1 产出延迟，模块5 Phase 2 无法拿到真实数据 | 高 | Phase 1-2 期间使用 Mock Service（已按契约定义接口）；Mock 行为与契约一致，Phase 2 功能逻辑可完整开发测试，待真实模块就绪后切换 |
| agentic 服务 Phase 3 前不可用 | 中 | T8/T9 的核心逻辑（数据汇聚→调用→存储→回查）全部可 Mock agentic 开发；LLM 不可用时返回友好错误，且 T3-T7（占 70% 功能）不依赖 agentic |
| 金仓数据库兼容性问题（MySQL → 金仓迁移） | 中 | MyBatis-Plus 兼容两者；Phase 1 用 MySQL，金仓培训后统一迁移；避免使用 MySQL 特有语法（如 `ON DUPLICATE KEY`），用 MyBatis-Plus 标准 API |
| T6 风险检测规则过多导致任务膨胀 | 中 | 7 类规则用策略模式独立文件，每个规则可单独测试和交付；先完成 3 个高风险规则（procrastination/low_score/inactive），其余 4 个逐步追加 |
| 报表导出依赖 Apache POI / iText 版本冲突 | 低 | 在 `pom.xml` 中显式声明版本，通过 `mvn dependency:tree` 检查冲突 |

---

## Open Questions（已决议）

1. **风险检测定时任务频率** → **每日凌晨 2:00 执行一次**。Phase 2 以手动触发为主，定时任务用 `@Scheduled(cron="0 0 2 * * ?")` 注解，配置开关可控启停。
2. **PDF 综合分析报告是否需要图表** → **Phase 2 不做图表**，仅表格 + 文字。Phase 4 视剩余时间决定是否追加 ECharts 截图嵌入。
3. **聚类结果是否需要教师手动修正** → **不做手动修正入口**。聚类结果仅供教师参考，不准确时教师可自行判断忽略。如后续反馈强烈再追加。
4. **报表历史保留 30 天是否足够** → **保留 30 天**。超过 30 天的文件由定时清理任务删除，教师如需长期保存应自行下载到本地。
5. **预警"已处理"后是否可被重新触发** → **可以，设 7 天冷却期**。同学生同类型预警在 `resolved_at` 后 7 天内不重复生成；超过 7 天且再次满足条件则生成新预警。

---

## 越权红线

- **不臆造接口字段**：所有 API 的请求/响应格式以 `specs/模块接口与协作规范.md` 第八章和本 plan 为准；新增字段必须先更新 spec 再改代码
- **不删除失败测试**：测试失败意味着实现有误或 spec 变了；先定位原因，不跳过测试
- **不绕过模块边界直接查其他模块的数据库表**：即使技术上可行（同进程），也必须通过对方模块的 Service 接口获取数据
- **不自动执行教学干预**：所有建议仅供教师参考，不在系统层面自动发送通知/调整任务/联系学生
- **不在 Phase 1-2 打开 game_mode_enabled**：游戏事件相关功能在 Phase 3 才激活
- **不持有学生/课程/任务/试题的写入权**：模块5 只做只读聚合，不对这些实体做增删改

---

## 完成定义（DoD）

- [ ] 全部 11 个任务验收通过，覆盖的 44 条需求（R1.1–R8.5）全部满足
- [ ] `mvn test` 全量通过，模块5 包测试覆盖率 ≥ 70%
- [ ] 端到端验证 5 个场景全部通过（见 T11 验证步骤）
- [ ] 代码已合入 `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/`
- [ ] 所有 API 有 Swagger 注解或 Postman Collection 可供前端对接
- [ ] agentic 不可用时所有受影响接口返回 503 + 友好错误提示，不崩溃
- [ ] 标记本 plan 状态为"已完成"，更新 `docs/plans/README.md` 索引

---

> 📁 本文档属于 `docs/plans/毛瑞琪/` — 模块5「学情分析与教学决策」的实施计划。上承 `specs/毛瑞琪/模块5-学情分析与教学决策-需求文档.md`（44条EARS需求），下接 `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/`（代码实现）。
