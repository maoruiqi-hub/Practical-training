# Dify 第一批 Workflow 详细设计与配置手册

## 1. 使用范围

本文只设计第一批可以直接迁移或轻量优化的 AI 能力：

1. 错题问题聚类。
2. 教学建议。
3. 学习风险检测。
4. 爬塔诊断报告。
5. 作业智能评阅。
6. 个性化推荐理由。

本文不设计新的培养能力映射 Workflow，也不迁移题包选择、掌握度计算和爬塔核心规则。那些内容需要后续重新设计。

本文的目标是让第一次使用 Dify 的开发者可以按照章节顺序，先在 Dify 页面中创建出可测试的 Workflow，再让后端逐个接入。

## 2. 先理解这几个词

| 名称 | 含义 |
|---|---|
| Workflow | 有固定输入和输出的工作流，适合聚类、评阅、风险分析等一次性任务 |
| Chatflow | 面向连续对话的工作流，适合课程问答和讲解；本批暂不使用 |
| Start | 定义后端传给 Dify 的输入变量 |
| LLM | 调用 DeepSeek 等模型并执行提示词 |
| End | 定义 Workflow 返回给后端的输出变量 |
| Dataset | Dify 知识库，本批结构化分析暂不依赖它 |
| API Key | 后端调用某个 Dify 应用的密钥，不能放到前端 |

第一批 Workflow 的基本形状全部是：

```text
Start → LLM → End
```

暂时不要添加 Agent、工具调用、循环、知识库检索和复杂分支。先把输入、输出和后端校验跑通。

## 3. 统一配置规则

### 3.1 应用命名

建议在 Dify 中创建以下六个 Workflow 应用：

| 顺序 | Dify 应用名称 | capability |
|---:|---|---|
| 1 | `course-problem-cluster` | `clusterProblems` |
| 2 | `course-teaching-suggestions` | `teachingSuggestions` |
| 3 | `student-risk-detect` | `riskDetect` |
| 4 | `tower-diagnosis-report` | `tower-diagnosis-report` |
| 5 | `submission-assessment` | `assessment` |
| 6 | `learning-recommendation` | `recommend` |

应用名称可以不同，但后端必须保存“capability → Dify 应用”的对应关系。

### 3.2 模型设置

在 Dify 的模型提供商中配置 DeepSeek。六个 Workflow 先使用同一个稳定模型和同一套基础参数，便于比较结果。

建议初始设置：

| 参数 | 建议值 | 说明 |
|---|---:|---|
| Temperature | `0.1～0.3` | 结构化分析需要稳定，不宜太随机 |
| 最大输出长度 | 按任务设置 | 防止报告过长拖慢响应 |
| 输出语言 | 中文 | 在提示词中明确要求 |

不要在每个 Workflow 中随意使用不同模型或不同温度，否则后续很难判断问题来自提示词还是模型参数。

### 3.3 统一输入方式

每个 Workflow 只设置一个 Start 输入：

```text
变量名：request_json
类型：Paragraph / Long Text
```

后端把结构化业务数据序列化成 JSON 后传入。这样比在 Dify 中为每个数据库字段创建大量变量更简单，也不会把数据库结构暴露给教师。

每个 Workflow 的提示词都使用：

```text
{{request_json}}
```

### 3.4 统一安全要求

- 不把数据库账号、密码、API Key 放入 Workflow 输入。
- 不把完整学生姓名、手机号、登录账号传给 AI；优先传匿名编号或统计结果。
- 后端提供的 ID 才是唯一合法 ID，AI 不允许自行生成或修改 ID。
- Dify 只返回建议，不能直接连接项目数据库。
- 后端必须校验 AI 返回的 JSON、ID、数值范围和数组数量。

## 4. 在 Dify 中创建第一个 Workflow

以下步骤六个应用都相同。

1. 登录 Dify 工作区。
2. 进入 `Studio` 或应用工作区。
3. 点击创建应用，选择 `Workflow`。
4. 输入本文指定的应用名称。
5. 在模型设置中选择已经配置好的 DeepSeek 模型。
6. 保留 `Start` 节点，新增一个 `LLM` 节点。
7. 将 `Start` 的 `request_json` 连接到 LLM 节点的提示词变量。
8. 在 LLM 节点开启结构化输出，填写本文对应的 JSON Schema。
9. 新增 `End` 节点，把 LLM 的结构化输出连接到 End。
10. 使用本文的测试数据运行 Workflow。
11. 测试通过后发布应用，再生成 API Key。

如果 Dify 当前版本将结构化输出显示为 `Structured Output` 或 `Output Variables`，选择等价功能即可。核心要求是 End 节点返回稳定的 JSON，而不是一段带 Markdown 的文本。

## 5. Workflow 一：错题问题聚类

### 5.1 用途

对应后端 `AgenticClient.clusterProblems(...)`，根据班级错题统计发现共性问题。它属于后台分析任务，不应该阻塞教师页面的其他操作。

### 5.2 输入样例

Start 输入变量：`request_json`

```json
{
  "courseCode": "python",
  "knowledgePoints": [
    {"id": "kp-1", "name": "函数", "wrongCount": 18, "studentCount": 30},
    {"id": "kp-2", "name": "异常处理", "wrongCount": 11, "studentCount": 30}
  ],
  "questionStats": [
    {
      "questionId": "q-101",
      "knowledgePointId": "kp-2",
      "wrongCount": 9,
      "wrongRate": 0.30,
      "commonWrongPatterns": ["把异常类型判断错误", "没有处理异常分支"]
    }
  ]
}
```

生产环境应传统计摘要，不要把全班每个学生的完整答案全部塞进一次请求。

### 5.3 LLM 提示词

```text
你是高校课程学情分析助手。

请根据输入的班级错题统计，识别最多 5 个有证据支撑的共性问题。

要求：
1. 只能使用输入中的 knowledge point id 和 question id。
2. 只能根据输入数据判断，不要补充输入中没有的事实。
3. 一个问题必须有明显的错误数量或错误模式支撑。
4. 问题名称要具体，例如“异常类型判断错误”，不要只写“基础较差”。
5. 如果证据不足，返回空的 clusters 数组。
6. 输出使用中文。
7. 只返回 JSON，不要返回 Markdown、解释文字或代码围栏。

输入数据：
{{request_json}}
```

### 5.4 结构化输出

```json
{
  "clusters": [
    {
      "topic": "string",
      "student_count": "number",
      "knowledge_point_ids": ["string"],
      "question_ids": ["string"],
      "description": "string",
      "suggested_action": "string",
      "confidence": "number"
    }
  ]
}
```

后端校验：

- `student_count` 不得小于 `0`。
- `confidence` 必须在 `0～1` 之间。
- 所有知识点和题目 ID 必须来自输入。
- 最多保存 5 个聚类。

## 6. Workflow 二：教学建议

### 6.1 用途

对应后端 `AgenticClient.teachingSuggestions(...)`，根据班级学情给教师生成干预建议。

### 6.2 输入样例

```json
{
  "courseCode": "python",
  "classSummary": {
    "studentCount": 30,
    "averageMastery": 68,
    "completionRate": 0.82,
    "atRiskStudentCount": 4
  },
  "weakPoints": [
    {"knowledgePointId": "kp-2", "name": "异常处理", "mastery": 42, "wrongRate": 0.58},
    {"knowledgePointId": "kp-5", "name": "文件读写", "mastery": 51, "wrongRate": 0.44}
  ],
  "recentClusters": [
    {"topic": "异常类型判断错误", "student_count": 9}
  ]
}
```

### 6.3 LLM 提示词

```text
你是高校教师的教学决策助手。

请根据输入的班级学习数据，生成最多 5 条可执行的教学建议。

要求：
1. 每条建议必须引用输入中的数据依据。
2. 建议要具体到复习、练习、分组辅导或教学节奏调整。
3. 不要输出学生姓名，不要虚构班级情况。
4. 如果数据不足，降低建议置信度并说明原因。
5. 输出使用中文。
6. 只返回 JSON，不要返回 Markdown。

输入数据：
{{request_json}}
```

### 6.4 结构化输出

```json
{
  "suggestions": [
    {
      "suggestion_type": "reteach|practice|individual|pace",
      "content": "string",
      "target": "whole_class|group|individual",
      "urgency": "high|medium|low",
      "based_on": ["string"],
      "confidence": "number"
    }
  ]
}
```

建议默认自动生成，但教师可以在插件内修改或忽略；不要让 AI 自动修改课程内容或教学计划。

## 7. Workflow 三：学习风险检测

### 7.1 用途

对应后端 `AgenticClient.riskDetect(...)`，分析学生学习进度、成绩和活跃情况。

### 7.2 输入样例

```json
{
  "courseCode": "python",
  "student": {
    "anonymousId": "student-42",
    "completionRate": 0.25,
    "averageScore": 48,
    "daysSinceLastActivity": 9,
    "wrongRate": 0.61,
    "deadlineDaysRemaining": 3
  },
  "recentEvents": [
    {"type": "quiz", "score": 42, "occurredAt": "2026-08-01"},
    {"type": "resource_view", "durationSeconds": 35, "occurredAt": "2026-07-30"}
  ]
}
```

### 7.3 LLM 提示词

```text
你是学习风险分析助手。

请根据输入的学习统计识别可能的学习风险。

要求：
1. 只根据输入的数据判断。
2. 风险类型只能使用 procrastination、low_score、inactive、progress_lag。
3. 没有足够证据时返回空 risks 数组。
4. 不要把一次低分直接判断为严重风险。
5. detail 必须说明触发判断的具体数据。
6. 输出使用中文，只返回 JSON。

输入数据：
{{request_json}}
```

### 7.4 结构化输出

```json
{
  "risks": [
    {
      "type": "procrastination|low_score|inactive|progress_lag",
      "level": "high|medium|low",
      "detail": "string",
      "evidence": ["string"],
      "confidence": "number"
    }
  ]
}
```

风险检测只能产生提醒，不能自动改变学生状态、限制账号或修改成绩。

## 8. Workflow 四：爬塔诊断报告

### 8.1 用途

对应 `tower-diagnosis-report`。本阶段只做“基础诊断层”：根据本次节点的知识点、具体错误和后端判题结果，生成复习重点与下一步练习建议。

本阶段明确不做以下事情：

- 不评价学生的培养能力或能力图谱。
- 不生成“某项能力不足”“能力提升”“能力迁移”等结论。
- 不读取或推断新的“现有能力点 → 培养能力”关系。
- 不修改掌握度、能力分、通关结果或游戏属性。

等能力点重构和培养能力映射稳定后，再单独扩展诊断输出。该功能当前已经有异步服务，迁移到 Dify 后继续保持异步，不允许答题结束或页面跳转等待 Dify。

### 8.2 输入样例

```json
{
  "courseCode": "python",
  "knowledgePointId": "kp-2",
  "roomType": "diagnosis",
  "correctRate": 0.67,
  "cleared": true,
  "questionCount": 3,
  "answers": [
    {
      "questionId": "q-101",
      "knowledgePointId": "kp-2",
      "correct": false,
      "difficulty": 2,
      "errorSummary": "异常类型判断错误"
    }
  ]
}
```

不要把 `studentNo` 传入 Dify。诊断只需要答题和课程上下文；学生身份由后端在结果落库时关联。

### 8.3 LLM 提示词

```text
你是课程学习诊断助手。

请根据学生本次节点的后端判题结果，生成简洁、具体的学习诊断。

要求：
1. 只能使用输入中的答题结果和知识点。
2. 不要重新判题，不要修改 correct、correctRate 或 cleared。
3. weaknesses 最多 3 条，每条必须对应具体错误或薄弱知识点。
4. recommendedAction 必须是下一步可执行的复习或练习建议。
5. 如果学生全部答对，也要给出简短的巩固建议，不要虚构错误。
6. 诊断范围仅限本次答题涉及的知识点和具体错误。
7. 禁止评价培养能力、能力图谱、能力迁移或学生总体学习能力。
8. 不要根据正确率单独推断长期掌握情况。
9. 输出使用中文，只返回 JSON。

输入数据：
{{request_json}}
```

### 8.4 结构化输出

```json
{
  "summary": "string",
  "weaknesses": ["string"],
  "recommendedAction": "string",
  "reviewFocus": ["string"],
  "confidence": "number"
}
```

后端返回给学生端时继续使用已有状态：

```text
pending → success
        → failed
```

AI 失败时保留判题结果、掌握度和通关结果，只将诊断报告标记为失败或使用规则说明。

### 8.5 当前阶段验收标准

- 输入只包含本次节点的答题事实和知识点上下文即可完成诊断。
- 正常结果能够指出具体错误，并给出可执行的复习动作。
- 全部答对时不生成虚假薄弱点，只返回巩固建议。
- 输出中不得出现培养能力、能力图谱或能力迁移结论。
- Dify 超时或失败不影响判题、掌握度、通关和页面跳转。
- 后续能力点重构时，可以在不改变基础诊断输入的前提下增加独立的能力解释层。

## 9. Workflow 五：作业智能评阅

### 9.1 用途

对应 `assessment`，用于填空题、简答题、编程题等主观或半主观材料的辅助评阅。

AI 结果只能作为教师复核参考，不能直接替代教师最终确认。

### 9.2 输入样例

```json
{
  "courseCode": "python",
  "task": {
    "taskNo": "task-101",
    "taskType": "programming",
    "description": "实现一个读取文件并统计单词频率的程序",
    "rubric": [
      {"name": "文件读取", "points": 30},
      {"name": "数据统计", "points": 40},
      {"name": "异常处理", "points": 20},
      {"name": "代码规范", "points": 10}
    ]
  },
  "submission": {
    "text": "学生提交的代码或文字答案",
    "hasAttachment": false
  }
}
```

### 9.3 LLM 提示词

```text
你是高校课程作业辅助评阅助手。

请根据任务要求、评分标准和学生提交内容生成教师复核草稿。

要求：
1. 评分必须以输入的 rubric 为依据。
2. 不要因为代码看起来完整就默认全部正确。
3. 对无法确认的部分降低 confidence，并在 summary 中说明。
4. 这是辅助评阅，不要声称已经完成教师最终评分。
5. score 和各维度分数必须是 0 到 100 的整数。
6. 输出使用中文，只返回 JSON。

输入数据：
{{request_json}}
```

### 9.4 结构化输出

```json
{
  "score": "number",
  "dimensions": {
    "内容完整性": "number",
    "知识点覆盖度": "number",
    "逻辑结构": "number",
    "表达规范": "number",
    "任务要求符合度": "number"
  },
  "summary": "string",
  "suggestions": ["string"],
  "riskLevel": "low|medium|high",
  "confidence": "number",
  "needsTeacherReview": true
}
```

后端必须保留原始提交、AI 评阅草稿、教师最终评分三份信息，不能让 AI 结果覆盖原始作答。

## 10. Workflow 六：个性化推荐理由

### 10.1 当前问题

当前 `RecommendationServiceImpl` 会遍历每个能力点，逐个调用 `recommend`。如果课程有 10 个能力点，就可能产生 10 次 AI 调用，这会直接放大延迟。

迁移到 Dify 时应顺手改成批量生成：一次请求传入所有推荐对象，一次返回所有理由。

### 10.2 输入样例

```json
{
  "courseCode": "python",
  "student": {"anonymousId": "student-42"},
  "recommendations": [
    {
      "targetId": "ap-1",
      "targetName": "Python 编程基础能力",
      "score": 35,
      "type": "review_material",
      "priority": 1
    },
    {
      "targetId": "ap-2",
      "targetName": "函数与异常处理能力",
      "score": 54,
      "type": "practice",
      "priority": 2
    }
  ]
}
```

### 10.3 LLM 提示词

```text
你是个性化学习推荐助手。

请为输入中的每个推荐对象生成一句不超过 40 字的中文推荐理由。

要求：
1. 必须保留输入中的 targetId。
2. 理由必须结合 targetName、score 和 type。
3. 不要虚构学生没有提供的学习行为。
4. 不要给出医学、心理或升学结论。
5. 每个对象只能返回一条理由。
6. 输出使用中文，只返回 JSON。

输入数据：
{{request_json}}
```

### 10.4 结构化输出

```json
{
  "reasons": [
    {
      "targetId": "string",
      "reason": "string",
      "confidence": "number"
    }
  ]
}
```

后端校验返回的 `targetId` 与输入完全匹配。Dify 失败时，直接使用现有的本地模板理由，不阻塞推荐列表生成。

## 11. Dify 测试方法

每个 Workflow 建议准备三组测试数据：

### 11.1 正常测试

字段完整，数量适中，包含真实业务中的典型内容。检查输出是否符合 Schema。

### 11.2 空数据测试

例如没有错题、没有风险、没有诊断错误。期望结果应该是空数组或明确的低风险结果，而不是 AI 自己编造数据。

### 11.3 错误数据测试

测试缺少字段、错误 ID、过长文本、无效数字和不支持的题型。Dify 可以返回错误，但后端不能因此写入非法数据。

每个 Workflow 至少记录：

- 测试输入摘要。
- Workflow 版本。
- 模型名称。
- 是否成功。
- 输出是否通过后端校验。
- 大致耗时。

## 12. 后端接入要求

当前项目的 `DifyClient` 已有：

```java
runWorkflow(Map<String, Object> inputs, String userId)
```

但它目前只配置一个 `DIFY_WORKFLOW_API_KEY`。由于每个 Dify Workflow 应用通常有独立 API Key，正式接入六个应用时需要改成 capability 到应用凭据的映射，例如：

```text
DIFY_WORKFLOW_API_KEY_CLUSTER
DIFY_WORKFLOW_API_KEY_SUGGESTIONS
DIFY_WORKFLOW_API_KEY_RISK
DIFY_WORKFLOW_API_KEY_DIAGNOSIS
DIFY_WORKFLOW_API_KEY_ASSESSMENT
DIFY_WORKFLOW_API_KEY_RECOMMEND
```

也可以使用后端配置表保存映射，但 API Key 仍然必须只由后端读取。

后端接入步骤：

1. 组装本文规定的 `request_json`。
2. 根据 capability 选择 Dify Workflow Key。
3. 调用 Workflow。
4. 读取 End 节点输出。
5. 进行 JSON 和业务校验。
6. 保存结果或返回任务状态。
7. 记录 Workflow 版本和失败原因。

后端不应该把 Dify 的原始输出直接返回给前端；前端只接收稳定的项目 DTO。

## 13. 超时与异步处理

以下任务应该异步调用：

- 错题聚类。
- 教学建议。
- 风险检测。
- 爬塔诊断报告。
- 作业评阅。

页面先拿到业务结果，再查询 AI 任务状态：

```text
pending → running → success
                  → failed
                  → timeout
```

推荐理由如果只生成短文本，可以同步尝试，但必须有本地模板兜底。课程问答和讲解是后续 Chatflow 任务，不能套用这里的同步规则。

Dify 慢或不可用时：

- 不影响学生提交答案。
- 不影响掌握度更新。
- 不影响节点结算和跳转。
- 不影响教师查看原始学情数据。
- 只将 AI 说明标记为“生成中”或“暂不可用”。

## 14. 发布前检查清单

### Dify 页面

- [ ] 应用类型是 Workflow。
- [ ] 应用名称与 capability 对应。
- [ ] Start 只有需要的输入变量。
- [ ] LLM 提示词要求只返回 JSON。
- [ ] 已配置结构化输出。
- [ ] End 节点输出变量正确。
- [ ] 正常、空数据、错误数据测试通过。
- [ ] 已发布当前版本。
- [ ] 已生成 API Key。

### 后端

- [ ] API Key 只在后端环境变量或安全配置中。
- [ ] 输入 JSON 不包含不必要的隐私数据。
- [ ] 输出 ID、数字范围和数组数量经过校验。
- [ ] AI 结果不直接覆盖原始业务数据。
- [ ] 异步任务有超时、失败和重试状态。
- [ ] 有本地规则或模板兜底。
- [ ] 重复任务不会重复落库。
- [ ] 保存 Workflow 版本和结果来源。

## 15. 本批完成后的下一步

第一批 Workflow 稳定后，再开始设计 `course-competency-mapping`：

```text
现有能力点 + 知识点 + 代表题目 + 教师培养目标
→ Dify 生成培养能力和关系矩阵草稿
→ 后端校验
→ 自动发布新矩阵版本
→ 教师可选查看、修改或回滚
```

这个 Workflow 不能沿用旧 `ability-map` 的输出格式，因为旧格式是“知识点生成旧能力点”，与新的“经过旧能力点生成培养能力”不是同一件事。
