# 星期六 Codex 检测问题

> 检测依据：`specs/模块接口与协作规范.md`、`references/ITERATION_LOG.md`、`specs/项目需求.md`。
> 检测分支：`fix/update-interfaces`。
> 说明：本文件先记录本次审查发现的问题，便于后续逐项修复。

## 一、阻塞级问题

### 1. 后端无法编译

执行 `mvn test` 时，后端在编译阶段失败，测试无法运行。

典型错误包括：

- `Question.getKnowledgePointId()`、`LearningTask.getTaskType()` 等 getter 找不到。
- `RiskAlert.setStudentId()` 等 setter 找不到。
- `AgenticResponse(boolean, Map, String)` 构造器找不到。

初步判断：实体和 DTO 大量依赖 Lombok `@Data`、`@AllArgsConstructor` 生成方法，但当前 Maven 运行在较新 JDK 上，未显式启用 Lombok 注解处理，导致 Lombok 生成代码没有参与编译。

影响：

- 后端无法启动。
- 所有单元测试、接口测试都无法执行。
- 其余功能合规性无法通过自动化测试验证。

## 二、接口契约与功能问题

### 2. 任务完成率统计是假数据（已修复）

位置：`backend/src/main/java/com/neu/CoursePlatform/controller/TaskController.java`

`/api/tasks/{taskNo}/stats` 中的 `completionRate` 当前只要任务存在就返回 `100`，没有按学生数、提交数、已完成数计算。

影响：

- 不符合模块2“任务完成统计”的接口契约。
- 会误导模块5学情分析和教师端任务完成率展示。

修复记录：

- `TaskController` 已改为按系统学生总数、有效提交去重学生数计算完成率。
- 过滤 `superseded` 旧提交，避免重复提交污染统计。
- 返回 `totalStudents`、`submittedStudents`、`totalSubmissions` 等统计字段。

### 3. 学生提交查询参数与契约不一致（已修复）

位置：`backend/src/main/java/com/neu/CoursePlatform/controller/TaskSubmissionController.java`

规范要求：

```text
GET /api/students/{id}/submissions?course_id={id}
```

当前代码只支持 `courseCode` 参数，没有兼容 `course_id`。

影响：

- 其他模块或前端按契约调用 `course_id` 时，课程过滤不会生效。

修复记录：

- `TaskSubmissionController` 已兼容 `course_id` 和旧参数 `courseCode`。
- 优先使用旧参数 `courseCode`，为空时使用契约参数 `course_id`。

### 4. 学习日志 course_id 过滤无效（已修复）

位置：

- `backend/src/main/java/com/neu/CoursePlatform/controller/BehaviorLogController.java`
- `backend/src/main/resources/mapper/BehaviorLogMapper.xml`

Controller 接收了 `course_id`，但 Service/Mapper 没有把该条件传入 SQL。

影响：

- 模块4计算画像、模块5做学情分析时，按课程查询日志会拿到跨课程数据。

修复记录：

- `BehaviorLogServiceImpl` 已把 `course_id` 过滤条件传给 Mapper。
- `BehaviorLogMapper.xml` 已在提供 `course_id` 时关联 `learning_task`，按 `learning_task.course_code` 过滤日志。
- 当前口径：有 `task_no` 的日志可以准确按课程过滤；无任务号日志因表结构没有课程字段，无法归属到课程。

### 5. 前后端组卷接口不一致（已修复）

位置：

- `frontend/src/api/index.js`
- `backend/src/main/java/com/neu/CoursePlatform/controller/ExamController.java`

前端调用：

```text
POST /api/questions/course/{courseCode}/generate
```

后端实际提供：

```text
POST /api/exams/generate?courseCode={courseCode}
```

影响：

- 前端组卷功能会 404。
- 不符合模块3“智能组卷”接口契约。

修复记录：

- 前端 `generatePaper` 已改为调用后端实际接口 `POST /api/exams/generate?courseCode=...`。
- 后端契约测试已覆盖 `POST /api/exams/generate`。

### 6. 学生列表接口不完全符合模块4契约（已修复）

规范要求：

```text
GET /api/students?class_id={id}
```

当前代码主要是：

```text
GET /api/students/list
```

且没有按 `class_id` 过滤。

影响：

- 模块5按班级分析学生时无法按契约直接消费学生列表接口。

修复记录：

- `StudentController` 已增加 `GET /api/students?class_id=...`。
- `StudentService` / `StudentMapper` 已增加按班级查询方法。
- 当前口径：项目学生表字段为 `class_name`，因此 `class_id` 参数兼容映射到 `student.class_name` 查询；后续若统一使用模块5的 `analytics_class_student`，可再接入模块5对外 Service。

## 三、跨层/跨模块边界问题

### 7. 部分 Controller 直接编排 Agentic 调用（已修复）

位置：

- `backend/src/main/java/com/neu/CoursePlatform/controller/LectureController.java`
- `backend/src/main/java/com/neu/CoursePlatform/controller/CourseQaController.java`
- `backend/src/main/java/com/neu/CoursePlatform/controller/AbilityMapGenerationController.java`

这些 Controller 直接注入并调用 `AgenticClient`，把 AI 请求组装、业务校验、外部服务调用都放在 Controller 中。

影响：

- Controller 层职责过重。
- 不利于测试和复用。
- 更合理的结构是 Controller 调 Service，由 Service 统一编排 agentic 调用。

修复记录：

- 新增 `CourseAiService` / `CourseAiServiceImpl`，承接知识点讲解、知识点问答、能力图谱生成的 AI 请求组装与 agentic 调用。
- `LectureController`、`CourseQaController`、`AbilityMapGenerationController` 已改为只做鉴权和入口转发，不再直接注入 `AgenticClient`。

### 8. 模块4推荐仍使用 MockAgenticClient（已修复）

位置：`backend/src/main/java/com/neu/CoursePlatform/profile/service/impl/RecommendationServiceImpl.java`

当前个性化推荐使用 `profile.mock.MockAgenticClient`，没有走统一的 `agentic.AgenticClient`。

影响：

- 与“所有 LLM 调用统一走 agentic 服务”的约束不完全一致。
- 后续真实 agentic 接入时还需要二次替换。

修复记录：

- `RecommendationServiceImpl` 已移除 `MockAgenticClient` 依赖，改为通过统一 `agentic.AgenticClient` 调用 `recommend` 能力。
- 当 agentic mock/http 返回中没有推荐理由时，Service 会使用本地兜底理由，保证推荐记录仍可生成。

### 9. 模块5部分数据源仍未闭环（已修复）

位置：`backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/external/Module13ExternalDataProvider.java`

以下方法仍为空实现或返回默认值：

- `getClassProgressList()` 返回空列表。
- `getStudentIdsByClass()` 返回空列表。
- `getLastActiveTime()` 返回 `null`。

影响：

- 风险检测无法覆盖真实班级学生。
- 长期未登录、进度落后等预警无法准确生成。
- 班级维度分析无法完全满足项目需求。

修复记录：

- `getStudentIdsByClass()` 已改为通过 `ClassInfoService.getStudentIds(classId)` 获取真实班级学生 ID。
- `getLastActiveTime()` 已改为通过模块2 `BehaviorLogService.listByUserId(studentId)` 取最近行为时间。
- `getClassProgressList(courseId)` 已改为基于真实学生列表和 `getStudentProgress()` 计算课程维度学生进度。
- `getStudentProgress()` 已过滤 `superseded` 旧提交，并按任务去重，避免重复提交污染完成率。

当前口径：

- `ExternalDataProvider.getClassProgressList` 的接口参数只有 `courseId`，没有 `classId`，因此当前返回课程维度全部学生进度；班级级别风险检测仍通过 `getStudentIdsByClass(classId)` 限定检测对象。

## 四、总体结论

当前代码在模块间调用方向上基本没有发现 Controller 直接注入 Mapper 这种严重跨层问题；模块5真实适配器也基本通过 Service 消费其他模块数据，方向是正确的。

但当前最大问题是后端无法编译。应优先修复 Lombok 注解处理配置，再继续修复接口参数、统计逻辑、AI 调用分层和模块5真实数据闭环。

## 五、二次复查新增问题

### 10. 模块5班级任务完成率仍为模拟数据（已修复）

位置：`backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/ProgressService.java`

复查发现 `getTaskCompletion()` 曾固定返回 15 人、12 提交、3 未提交等模拟数据，无法反映真实班级任务完成情况。

修复记录：

- `ExternalDataProvider` 新增 `getTaskCompletion(classId, taskId)`。
- `ProgressService.getTaskCompletion()` 已改为调用真实数据提供者，不再生成固定模拟值。
- `Module13ExternalDataProvider` 已按班级学生名单统计任务有效提交、未提交、逾期提交和提交率。
- `MockExternalDataProvider` 同步实现新接口，避免默认开发环境断裂。

### 11. 模块5班级进度按课程全体学生计算（已修复）

位置：

- `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/ProgressService.java`
- `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/external/Module13ExternalDataProvider.java`

复查发现 `ProgressService.getClassProgress(classId, courseId)` 虽然接收 `classId`，但底层只按 `courseId` 获取进度；真实适配器曾使用 `studentService.list()`，会把全体学生纳入班级进度统计。

修复记录：

- `ExternalDataProvider.getClassProgressList` 已改为接收 `classId, courseId`。
- `ProgressService`、`RiskDetectionService`、`TeachingSuggestionService` 已同步传入 `classId`。
- `Module13ExternalDataProvider` 已通过 `ClassInfoService.getStudentIds(classId)` 获取班级学生，只统计指定班级。

## 六、剩余待处理问题与难度评估

### 12. 空班级成绩统计存在 NaN 风险（已修复，难度：低）

位置：`backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/impl/ScoreAnalysisServiceImpl.java`

问题：

- `getClassScoreOverview()` 在统计通过率时使用 `passCount / allScores.size()`。
- 当班级没有学生，或 `getStudentIdsByClass(classId)` 返回空列表时，`allScores.size()` 为 0，会产生 `NaN`。

影响：

- 教师端班级成绩总览可能出现异常数值。
- 后续图表渲染可能因为 `NaN` 出现显示异常。

建议修复：

- 当 `allScores.isEmpty()` 时，返回空排名、空分布、均分/最高/最低/标准差/通过率均为 0。
- 增加单元测试覆盖空班级场景。

处理难度：

- 低。只需要补边界判断和测试，不涉及跨模块接口变更。

修复记录：

- `ScoreAnalysisServiceImpl.getClassScoreOverview()` 已增加空班级边界判断，空列表时返回均分、最高分、最低分、标准差、通过率均为 0，排名和分布为空列表。
- `buildDistribution()` 已增加空分数列表保护，避免分布百分比出现 `NaN`。
- 新增 `ScoreAnalysisServiceImplTest.classOverviewForEmptyClassReturnsZeroStats()` 覆盖空班级场景。

### 13. 模块4画像初始化仍依赖 MockKnowledgePointService（已修复，难度：中）

位置：`backend/src/main/java/com/neu/CoursePlatform/profile/service/impl/ProfileServiceImpl.java`

问题：

- 学生画像初始化能力评分时仍通过 `MockKnowledgePointService.getAbilityMap(courseCode)` 获取能力点。
- 当前模块1已经有能力图谱相关 Service，继续使用 Mock 会导致模块4能力评分与真实课程能力图谱不一致。

影响：

- 个性化推荐、能力评分、学生画像可能基于模拟能力点生成。
- 不完全符合“模块间通过 Service 查询真实数据”的协作规范。

建议修复：

- 将 `MockKnowledgePointService` 替换为模块1真实 `AbilityPointService` 或 `AbilityMapService`。
- 初始化能力评分时使用真实 `AbilityPoint` 数据。
- 若课程尚未生成能力图谱，可保留空列表或明确兜底策略，而不是写入固定 Mock 能力点。

处理难度：

- 中。需要确认模块1能力点实体字段与模块4 `CompetencyScore` 字段映射，并补画像初始化测试。

修复记录：

- `ProfileServiceImpl` 已移除 `MockKnowledgePointService` 注入，改为注入模块1真实 `AbilityPointService`。
- 学生画像初始化能力评分时，已通过 `AbilityPointService.listByCourseCode(String.valueOf(courseCode))` 获取真实能力点，并映射 `abilityPointId -> CompetencyScore.abilityPointId`、`name -> CompetencyScore.abilityPointName`。
- 画像摘要和知识塔能力图谱也改为基于真实 `AbilityPoint` 构造；课程尚未生成能力图谱时返回空能力图谱，不再写入固定 Mock 能力点。
- `DEF` 重算已改为基于当前课程真实能力点评分平均值；当前模块1 `AbilityPoint` 暂无 level 字段，后续如补充层级字段可再恢复“基础层级”筛选。
- 新增 `ProfileServiceImplTest.profileInitializationUsesRealAbilityPoints()` 覆盖画像初始化使用真实能力点的映射。

### 14. 教学建议/共性问题聚类仍缺历史持久化与结构化解析（难度：中高）

位置：

- `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/TeachingSuggestionService.java`
- `backend/src/main/java/com/neu/CoursePlatform/module5_analytics/service/ProblemClusterService.java`

问题：

- `TeachingSuggestionService.getHistory()` 当前返回空列表。
- agentic 返回解析仍是占位式包装，内容包含“返回格式待适配”。
- `ProblemClusterService.getLatestCluster()` 也未从分析报告表读取历史结果。

影响：

- 教师端无法查看历史教学建议或最近一次共性问题聚类结果。
- “学习风险预警与干预建议”“班级共性问题聚类”“教学建议”功能还不是完整闭环。

建议修复：

- 定义聚类结果和教学建议的持久化结构，优先复用规范中的 `AnalyticsReport` 或新增对应实体/Mapper。
- 调用 agentic 后解析为结构化列表并保存。
- `getHistory()` / `getLatestCluster()` 改为从持久化表读取。

处理难度：

- 中高。需要补实体、表结构、Mapper、Service、Controller 返回格式和测试，改动面比前两个更大。

## 七、建议处理顺序

1. 先修问题 12：低风险、容易验证，可以快速消除边界异常。
2. 再修问题 13：能让模块4画像真正接入模块1能力图谱，收益高。
3. 最后修问题 14：功能价值大，但涉及持久化设计，建议单独作为一轮改动。
