# Dify 实操教程：从零创建第一个错题聚类 Workflow

## 1. 这份教程要做什么

这不是一份抽象设计，而是一份可以边看边点击的练习。

本教程只完成一个功能：

```text
输入一段班级错题统计 JSON
→ Dify 调用 DeepSeek
→ 输出最多 5 个共性问题的 JSON
```

完成后，你会掌握后面四个 Dify Workflow 都会用到的基本操作：

- 创建 Workflow 应用。
- 配置输入变量。
- 在 LLM 节点中编写系统提示词。
- 让模型返回结构化 JSON。
- 使用 Test Run 测试输入和输出。
- 查看节点日志定位错误。
- 发布 Workflow 并获取后端 API Key。

本文假设你使用的是 Dify Cloud 或已经可以打开的自托管 Dify 网页。不同版本的按钮文字可能显示为 `Start` 或 `User Input`，它们在本教程中指的是同一个输入节点。

## 2. 先不要连接我们的后端

第一次练习时，不要先改 Java、不要先配置 API Key，也不要先上传课程资料。

先在 Dify 页面中用固定测试数据跑通 Workflow。只有 Dify 页面中的测试结果稳定以后，才进入后端接入。

本次需要准备：

- Dify 账号。
- 一个已经配置 DeepSeek 的模型提供商。
- 浏览器。
- 本文后面的测试 JSON。

如果还没有配置模型，先在 Dify 的模型提供商设置中添加 DeepSeek。模型名称可以使用你们当前可用的 DeepSeek 模型，第一次练习不必追求最复杂的模型。

## 3. 创建应用

### 3.1 进入创建页面

1. 在浏览器打开 Dify 控制台。
2. 登录后进入 `Studio`。
3. 点击 `Create from blank`、`从空白创建` 或类似按钮。
4. 选择 `Workflow`，不要选择 Chatbot、Agent 或 Chatflow。
5. 在应用名称中输入：

```text
course-problem-cluster
```

6. 描述可以填写：

```text
根据班级错题统计识别共性问题，供教师查看和教学干预使用。
```

7. 点击创建。

创建成功后，你应该看到一个工作流画布。画布上一般已经有：

```text
Start（或 User Input） → End（或 Output）
```

如果画布上的节点名称不同，不要紧，关键是有一个输入节点和一个结束节点。

### 3.2 先保存一次

创建后先点击右上角的保存按钮。如果页面没有单独的保存按钮，Dify 可能会自动保存。

此时先不要发布。`保存`代表编辑草稿，`发布`代表让 API 调用使用这个版本，这两个动作不是一回事。

## 4. 配置输入节点

### 4.1 打开输入节点

1. 点击画布上的 `Start` 或 `User Input` 节点。
2. 右侧会出现配置面板。
3. 找到 `Input Fields`、`输入变量` 或 `添加变量`。
4. 添加一个文本变量。

填写如下：

| 配置项 | 填写内容 |
|---|---|
| Label / 显示名称 | `Request JSON` |
| Variable Name / 变量名 | `request_json` |
| Type / 类型 | `Paragraph` 或 `Long Text` |
| Required / 必填 | 开启 |
| Max Length / 最大长度 | 先填 `20000`；如果版本限制较小，填允许的最大值 |
| Description / 说明 | `后端传入的班级错题统计 JSON` |

变量名必须是英文的 `request_json`。提示词中后面要引用这个名字，不能写成中文名称。

### 4.2 你现在应该看到什么

输入节点配置完成后，画布上的 Start 节点应该能看到类似：

```text
request_json: Paragraph
```

如果没有看到变量名，说明还没有点击保存变量。先保存输入节点，再继续下一步。

## 5. 添加 LLM 节点

### 5.1 添加节点

1. 点击 Start 节点右侧的加号，或者点击画布上的 `+`。
2. 在节点列表中选择 `LLM`。
3. 将 Start 节点连到 LLM 节点。
4. 点击新建的 LLM 节点。
5. 将节点名称改为：

```text
Analyze Problem Clusters
```

### 5.2 选择模型

在 LLM 节点的模型选择处，选择你已经配置好的 DeepSeek 模型。

如果模型列表为空：

- 说明模型提供商还没有配置成功。
- 先离开 Workflow，完成模型提供商配置。
- 回到 Workflow 后刷新页面。

第一次练习先不要调整其他高级参数。只设置：

| 参数 | 建议值 |
|---|---:|
| Temperature | `0.2` 左右 |
| Max Tokens | `1500～2500` |

## 6. 配置 LLM 提示词

### 6.1 系统提示词位置

在 LLM 节点配置面板中找到 `System Prompt`、`系统提示词` 或角色提示词输入框，把下面整段内容粘贴进去：

```text
你是高校课程学情分析助手，负责根据班级错题数据识别有证据支撑的共性问题。

你的任务：
从输入数据中识别最多 5 个共性问题，并给出与问题对应的知识点、题目和教学建议。

必须遵守：
1. 只能引用输入中的 knowledge point id 和 question id。
2. 不要创造输入中不存在的学生、题目、知识点或统计数字。
3. 一个共性问题必须有错误数量、错误率或重复错误模式支撑。
4. 问题名称要具体，例如“异常类型判断错误”，不要只写“基础薄弱”。
5. student_count 必须来自输入数据或根据输入中的人数统计计算。
6. confidence 必须是 0 到 1 之间的小数。
7. 如果输入证据不足，返回空的 clusters 数组。
8. 输出使用简体中文。
9. 只能输出 JSON，不要输出 Markdown、解释文字或 ```json 代码围栏。
```

### 6.2 添加用户提示词

在系统提示词下面找到 `User Prompt`、`用户提示词` 或消息输入区域。

不要手敲变量名。按照下面操作：

1. 点击用户提示词输入框。
2. 输入一句：

```text
请分析下面的班级错题数据：
```

3. 在同一个输入框中输入 `{` 或点击变量插入按钮。
4. 从变量列表中选择 `Start/request_json` 或 `User Input/request_json`。

最终用户提示词应该类似：

```text
请分析下面的班级错题数据：

{{request_json}}
```

在 Dify 画布中，它可能显示为一个带颜色的变量标签，而不是普通文字。这是正常的。不要把变量名拼错成 `requestJson`、`request-json` 或 `Request JSON`。

## 7. 配置结构化输出

### 7.1 找到结构化输出开关

在 LLM 节点配置面板中寻找以下任意一种名称：

- `Structured Output`。
- `结构化输出`。
- `Output Variables`。
- `输出变量`。
- `JSON Schema`。

不同 Dify 版本的显示名称可能不同。目标是让 LLM 节点输出一个结构化对象，而不是纯文本。

### 7.2 导入 JSON Schema

如果页面有 `Import from JSON`、`从 JSON 导入` 或 `导入 Schema` 按钮，粘贴下面内容：

```json
{
  "type": "object",
  "properties": {
    "clusters": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "topic": {"type": "string"},
          "student_count": {"type": "number"},
          "knowledge_point_ids": {
            "type": "array",
            "items": {"type": "string"}
          },
          "question_ids": {
            "type": "array",
            "items": {"type": "string"}
          },
          "description": {"type": "string"},
          "suggested_action": {"type": "string"},
          "confidence": {"type": "number"}
        },
        "required": [
          "topic",
          "student_count",
          "knowledge_point_ids",
          "question_ids",
          "description",
          "suggested_action",
          "confidence"
        ]
      }
    }
  },
  "required": ["clusters"]
}
```

如果你的 Dify 版本不是导入 Schema，而是逐个添加输出字段，则按下面方式添加：

```text
字段名：clusters
类型：Array / Object Array
```

如果它要求配置数组内部字段，再添加：

```text
topic                  String
student_count          Number
knowledge_point_ids    Array[String]
question_ids           Array[String]
description            String
suggested_action       String
confidence             Number
```

### 7.3 为什么一定要结构化输出

如果只在提示词里说“请返回 JSON”，模型可能返回：

```text
当然可以，以下是分析结果：
```json
...
```
```

这种结果虽然人能看懂，但后端处理起来不稳定。结构化输出可以让 Dify 和后端更明确地知道每个字段是什么。

不过结构化输出不是安全边界。后端仍然必须检查 ID 是否真实、数字是否越界。

## 8. 配置 End 节点

### 8.1 连接节点

1. 从 LLM 节点右侧的输出连接到 End 节点。
2. 点击 End 节点。
3. 添加一个输出变量。

填写：

| 配置项 | 填写内容 |
|---|---|
| Output Name | `result` |
| Type | LLM 结构化输出对应的 Object |
| Value | 选择 `Analyze Problem Clusters/structured_output` |

如果结构化输出在你的版本中叫 `text` 或 `json`, 就选择 LLM 节点实际生成的结构化输出变量，不要选择普通文本变量。

### 8.2 检查画布

现在画布应该是：

```text
Start(request_json)
        ↓
Analyze Problem Clusters(LLM)
        ↓
End(result)
```

如果出现一个节点没有连接，Test Run 时可能不会执行它。

## 9. 第一次 Test Run

### 9.1 打开测试

1. 点击右上角 `Test Run`、`运行` 或类似按钮。
2. 找到 `request_json` 输入框。
3. 粘贴下面的完整 JSON。

```json
{
  "courseCode": "python-demo",
  "knowledgePoints": [
    {
      "id": "kp-1",
      "name": "函数",
      "wrongCount": 18,
      "studentCount": 30
    },
    {
      "id": "kp-2",
      "name": "异常处理",
      "wrongCount": 11,
      "studentCount": 30
    }
  ],
  "questionStats": [
    {
      "questionId": "q-101",
      "knowledgePointId": "kp-2",
      "wrongCount": 9,
      "wrongRate": 0.3,
      "commonWrongPatterns": [
        "把异常类型判断错误",
        "没有处理异常分支"
      ]
    },
    {
      "questionId": "q-102",
      "knowledgePointId": "kp-1",
      "wrongCount": 8,
      "wrongRate": 0.27,
      "commonWrongPatterns": [
        "函数参数和返回值理解错误"
      ]
    }
  ]
}
```

4. 点击 `Start Run`、`开始运行` 或类似按钮。

### 9.2 正常结果应该是什么

结果不要求文字完全一致，但结构应该接近：

```json
{
  "clusters": [
    {
      "topic": "异常类型判断错误",
      "student_count": 9,
      "knowledge_point_ids": ["kp-2"],
      "question_ids": ["q-101"],
      "description": "多个学生在异常处理题中错误判断异常类型",
      "suggested_action": "安排异常类型和异常分支处理的专项练习",
      "confidence": 0.86
    }
  ]
}
```

你需要检查的不是措辞，而是：

- 最外层是否为 `clusters`。
- `clusters` 是否为数组。
- `knowledge_point_ids` 中是否只有 `kp-1` 或 `kp-2`。
- `question_ids` 中是否只有 `q-101` 或 `q-102`。
- `confidence` 是否为 0 到 1 之间的数字。
- 是否没有出现输入中不存在的 ID。

### 9.3 如果运行失败，先看哪里

不要立刻修改提示词。按下面顺序检查：

1. Start 节点的变量名是不是 `request_json`。
2. User Prompt 中是否真的插入了 Start 节点变量。
3. LLM 节点是否选中了可用模型。
4. JSON Schema 是否是合法 JSON。
5. End 节点是否引用了 LLM 的输出变量。
6. 查看对应节点的 `Last Run`、`运行日志` 或 `Trace`。

Dify 官方快速入门建议在节点级别查看最近一次运行日志，必要时可以只运行某个节点，不必每次从头执行整个 Workflow。[Dify Workflow 快速入门](https://docs.dify.ai/en/guides/application-orchestrate/creating-an-application)

## 10. 做三个故意失败的测试

### 10.1 空数据测试

把输入替换成：

```json
{
  "courseCode": "python-demo",
  "knowledgePoints": [],
  "questionStats": []
}
```

期望：

```json
{"clusters": []}
```

如果模型开始编造“学生普遍存在某问题”，说明提示词约束不够，需要补充“无证据返回空数组”。

### 10.2 低证据测试

```json
{
  "courseCode": "python-demo",
  "knowledgePoints": [
    {"id": "kp-1", "name": "函数", "wrongCount": 1, "studentCount": 30}
  ],
  "questionStats": [
    {
      "questionId": "q-101",
      "knowledgePointId": "kp-1",
      "wrongCount": 1,
      "wrongRate": 0.03,
      "commonWrongPatterns": []
    }
  ]
}
```

期望：返回空数组，或者返回低置信度的问题，并且不能夸大成“班级共性问题”。

### 10.3 非法 ID 测试

在 `questionStats` 中加入：

```json
{"questionId": "q-not-exist", "knowledgePointId": "kp-1", "wrongCount": 10}
```

模型可能会照抄这个 ID，这不代表 Workflow 失败。真正的防线是后端校验：后端发现 ID 不属于本次输入或数据库，就丢弃这条关系，不落库。

## 11. 发布 Workflow

测试通过后：

1. 点击右上角 `Publish`、`发布` 或 `Publish Update`。
2. 确认发布的是当前画布版本。
3. 打开应用的 `API Access`、`API 访问` 或 `发布/API` 页面。
4. 创建一个只供后端使用的 API Key。
5. 复制 API Key 到安全位置，不要发到聊天、前端代码或 Git。

Dify 的 Workflow API 使用服务端 API Key；官方接口返回 Workflow 的状态、输入、输出和错误信息，适合由后端异步任务读取。[Dify Workflow API 文档](https://docs.dify.ai/api-reference/%E3%83%AF%E3%83%BC%E3%82%AF%E3%83%AD%E3%83%BC/%E3%83%AF%E3%83%BC%E3%82%AF%E3%83%AD%E3%83%BC%E5%AE%9F%E8%A1%8C%E8%A9%B3%E7%B4%B0%E3%82%92%E5%8F%96%E5%BE%97)

## 12. 它和我们的后端如何对应

这个 Workflow 对应现有后端的：

```text
AgenticClient.clusterProblems(request)
```

当前后端做的事情是：

1. 统计课程错题。
2. 组装一个 `Map<String, Object>`。
3. 调用 AI。
4. 解析返回的 JSON。
5. 保存问题聚类报告。

迁移后只替换第 3 步的执行器：

```text
原来：后端提示词 → DeepSeek
现在：后端 request_json → Dify Workflow → DeepSeek
```

第 1、4、5 步仍由后端负责。

Dify 返回的 End 输出可能在 API 响应中位于 `outputs.result`，具体以你当前 Dify 版本的实际响应为准。后端接入时需要先打印一次脱敏后的响应结构，再写正式解析代码；不能凭猜测写死字段。

## 13. 先不做后端改动的验收标准

只有全部满足以下条件，才算第一个 Workflow 做成功：

- [ ] 能在 Dify 画布中看到 Start → LLM → End。
- [ ] Start 中有必填的 `request_json`。
- [ ] LLM 能使用 DeepSeek 模型运行。
- [ ] 正常测试返回合法 JSON。
- [ ] 空数据测试返回空数组或明确无证据结果。
- [ ] 非法 ID 不会被当成真实数据库记录。
- [ ] 能看到每个节点的运行日志。
- [ ] 已发布当前版本。
- [ ] 已生成 API Key，但还没有放到前端。

做到这里后，再进行后端接入，不要跳过测试直接改 Java。

## 14. 其他四个 Workflow 怎么复制

第一个 Workflow 跑通后，可以复制应用或创建新 Workflow。保留：

- Start 的 `request_json`。
- LLM 节点。
- End 节点。
- 测试和发布流程。

只替换三部分：

| 功能 | 替换内容 |
|---|---|
| 教学建议 | 系统提示词、输出 `suggestions` Schema |
| 爬塔诊断 | 系统提示词、输出 `summary/weaknesses/recommendedAction` Schema |
| 作业评阅 | 系统提示词、输出评分 Schema |
| 推荐理由 | 系统提示词、输出 `reasons` Schema；输入改为批量推荐对象 |

每复制一个 Workflow，都重新做正常、空数据和错误数据三组测试。不要只复制成功的测试结果。

## 15. 下一步

你完成本教程后，下一步才是：

1. 把 Dify 的第一个 Workflow API Key 配置到后端开发环境。
2. 后端发送真实的错题统计。
3. 打印一次脱敏后的 Dify 响应。
4. 校验后端能否读取 `clusters`。
5. 再将任务改成异步。

培养能力映射暂时不要照着本教程直接创建。它需要先经过现有能力点，输入和输出都与错题聚类不同，应该等第一批 Workflow 熟悉后单独设计。

## 16. 剩余四个 Workflow 的具体设计

下面四个 Workflow 可以直接按照第一个错题聚类 Workflow 的方式创建。为了避免重复，默认都使用：

```text
Start(request_json) → LLM → End(result)
```

区别只在于 LLM 提示词、结构化输出 Schema 和后端传入的 JSON。每个 Workflow 都建议独立创建和发布，不要把五个功能合并成一个超大的 Workflow。学习风险检测见第 18 节，完全由后端规则引擎处理。

## 17. Workflow 二：教学建议

### 17.1 应用信息

```text
应用名称：course-teaching-suggestions
capability：teachingSuggestions
用途：根据班级学情生成教师可执行的教学干预建议
```

### 17.2 节点和连线

```text
Start → Analyze Teaching Situation（LLM）→ End
```

| 节点 | 输入 | 输出 |
|---|---|---|
| Start | `request_json: Paragraph` | 班级统计、薄弱知识点、错题聚类 |
| Analyze Teaching Situation | `request_json` | `structured_output` |
| End | `Analyze Teaching Situation/structured_output` | `result` |

### 17.3 Start 输入示例

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
    {
      "knowledgePointId": "kp-2",
      "name": "异常处理",
      "mastery": 42,
      "wrongRate": 0.58,
      "affectedStudentCount": 18
    }
  ],
  "recentClusters": [
    {
      "topic": "异常类型判断错误",
      "student_count": 9,
      "knowledge_point_ids": ["kp-2"]
    }
  ]
}
```

### 17.4 LLM 节点输入与提示词

LLM 节点输入：

```text
{{request_json}}
```

系统提示词：

```text
你是高校教师的教学决策助手。

请根据输入的班级学习数据，生成最多 5 条具体、可执行的教学干预建议。

要求：
1. 每条建议必须有输入数据作为依据，不能凭空判断。
2. 建议要具体到复习、练习、分组辅导或教学节奏调整。
3. based_on 必须引用输入中的真实数据特征，例如知识点名称、掌握度、错误率或受影响人数。
4. 不要输出学生姓名、隐私信息或输入中不存在的班级情况。
5. 没有足够证据时返回空 suggestions 数组。
6. suggestion_type 只能是 reteach、practice、individual、pace。
7. target 只能是 whole_class、group、individual。
8. urgency 只能是 high、medium、low。
9. confidence 必须是 0 到 1 之间的小数。
10. 输出使用简体中文，只返回 JSON，不要返回 Markdown。

班级学情数据：
{{request_json}}
```

### 17.5 LLM 节点输出 Schema

```json
{
  "type": "object",
  "properties": {
    "suggestions": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "suggestion_type": {"type": "string"},
          "content": {"type": "string"},
          "target": {"type": "string"},
          "urgency": {"type": "string"},
          "based_on": {"type": "array", "items": {"type": "string"}},
          "confidence": {"type": "number"}
        },
        "required": ["suggestion_type", "content", "target", "urgency", "based_on", "confidence"]
      }
    }
  },
  "required": ["suggestions"]
}
```

End 节点：

```text
输出变量名：result
输出值：Analyze Teaching Situation/structured_output
```

### 17.6 修改建议

- 不要把“生成建议”和“自动执行教学操作”放在一个 Workflow 中。
- 后端应保存建议的依据和生成时间，方便教师判断是否采纳。
- 班级测验结束后异步执行，不要让教师等待页面请求完成。
- 相同课程和相同测验批次不要重复生成，可以使用 `courseCode + assessmentBatchId` 做幂等键。

## 18. 学习风险检测：保留为后端规则引擎

学习风险检测不再创建 Dify Workflow，也不调用 Agent。它必须在后端基于真实数据同步完成，避免班级检测受模型延迟、网络或输出不稳定影响。

### 18.1 后端输入和数据来源

| 数据 | 当前来源 | 用途 |
|---|---|---|
| 学生成绩趋势 | `ExternalDataProvider.getStudentScores` | 最近 10 次成绩，按 `scoredAt` 时间升序 |
| 知识点掌握度历史 | `KnowledgeMasteryHistory` | 识别掌握度的明显回落 |
| 能力点历史 | `CompetencyScoreHistory` | 补充识别能力点分数回落 |
| 学习进度 | `StudentProgressDTO` | 判断进度落后 |
| 最近活动时间 | 学习行为日志 | 判断未活跃和拖延 |

### 18.2 规则和阈值

| 风险类型 | 后端规则 | 等级 |
|---|---|---|
| `low_score` | 最近 3 次均低于 60 | high |
| `score_decline` | 至少 3 条成绩；近期均值比此前个人基线低 20 分以上，且最新低于 70 | medium；证据至少 4 条、差值低于 -30 且单次跌幅至少 25 时为 high |
| `score_volatility` | 至少 4 条成绩；近 6 次标准差至少 18，且涨跌方向至少反转 2 次 | medium；标准差至少 25 且证据至少 5 条时为 high |
| `mastery_drop` | 至少 2 条知识点或能力点历史；单次回落至少 15 | medium；单次回落至少 30 时为 high |
| `inactive` | 3 天未活动 | medium；7 天未活动为 high |
| `procrastination` | 超过 24 小时未活动 | medium；超过 7 天为 high |
| `progress_lag` | 完成率比班级平均低超过 30% | medium |

基线窗口规则：最新 1 至 3 次作为“近期窗口”，此前最多 3 次作为“个人基线”。例如 `[82,79,81,46]` 会计算为 `80.67 → 46`，变化 `-34.67`；它不是“平均分 72”，而是有明确证据的突然下降。

### 18.3 输出和边界

- 所有预警都写入既有 `analytics_risk_alert`，详情字段保存触发规则、均值、差值、标准差、最大跌幅和证据数量。
- 同一学生同一风险类型仍沿用活跃预警去重，避免教师端重复刷屏。
- 没有足够历史数据时不生成趋势类预警；不会根据单次低分直接生成高风险。
- 风险提示用于教师干预，不直接改变学生账号状态，也不自动展示可能造成误解的标签。

## 19. Workflow 四：爬塔诊断报告

> 当前只创建基础诊断层。它只描述知识点、具体错误和复习动作，不评价培养能力；能力点重构完成后再增加独立的能力解释层。

### 19.1 应用信息

```text
应用名称：tower-diagnosis-report
capability：tower-diagnosis-report
用途：根据后端已经判定的爬塔答题结果生成复习建议
```

### 19.2 节点和连线

```text
Start → Generate Diagnosis Report（LLM）→ End
```

| 节点 | 输入 | 输出 |
|---|---|---|
| Start | `request_json: Paragraph` | 节点、知识点、正确率和答题摘要 |
| Generate Diagnosis Report | `request_json` | `structured_output` |
| End | `Generate Diagnosis Report/structured_output` | `result` |

### 19.3 Start 输入示例

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
    },
    {
      "questionId": "q-102",
      "knowledgePointId": "kp-2",
      "correct": true,
      "difficulty": 2,
      "errorSummary": ""
    }
  ]
}
```

### 19.4 LLM 节点输入与提示词

LLM 节点输入：

```text
{{request_json}}
```

系统提示词：

```text
你是课程爬塔学习诊断助手。

请根据后端已经完成判题的本次节点答题记录，生成简洁、具体的学习诊断。

重要规则：
1. correct、correctRate、cleared 和 questionCount 是后端事实，不能重新判定或修改。
2. 只能使用输入中的知识点、题目和错误摘要。
3. weaknesses 最多 3 条，并且每条必须对应具体错误或薄弱知识点。
4. recommendedAction 必须是下一步可以执行的复习或练习建议。
5. 如果学生全部答对，weaknesses 返回空数组，但仍给出巩固建议。
6. 诊断范围仅限本次答题涉及的知识点和具体错误。
7. 禁止评价培养能力、能力图谱、能力迁移或学生总体学习能力。
8. 不要根据正确率单独推断长期掌握情况。
9. 不要输出学生姓名、账号或其他身份信息。
10. 输出使用简体中文，只返回 JSON。

答题记录：
{{request_json}}
```

### 19.5 LLM 节点输出 Schema

```json
{
  "type": "object",
  "properties": {
    "summary": {"type": "string"},
    "weaknesses": {"type": "array", "items": {"type": "string"}},
    "recommendedAction": {"type": "string"},
    "reviewFocus": {"type": "array", "items": {"type": "string"}},
    "confidence": {"type": "number"}
  },
  "required": ["summary", "weaknesses", "recommendedAction", "reviewFocus", "confidence"]
}
```

### 19.6 修改建议

- 保留现有后端 `TowerDiagnosisAsyncService` 的异步触发方式，只替换 AI 执行器。
- Dify 失败时只影响报告，不影响答题判定、掌握度、通关和页面跳转。
- Start 输入中不要传 `studentNo`，使用节点和答题摘要即可。
- 后端应该保存 `reportSource = dify`、Workflow 版本和任务状态。
- 报告生成中的页面显示“诊断生成中”，不要显示空白或无限 loading。
- 当前版本只验收知识点诊断、错误解释和复习建议；不得把旧能力点或尚未确定的培养能力写入报告。
- 能力点重构完成后，新增能力解释应作为独立输出或独立 Workflow，不直接改变本基础诊断报告的事实字段。

## 20. Workflow 五：作业智能评阅

### 20.1 应用信息

```text
应用名称：submission-assessment
capability：assessment
用途：生成教师复核用的作业评阅草稿
```

### 20.2 节点和连线

```text
Start → Generate Assessment Draft（LLM）→ End
```

| 节点 | 输入 | 输出 |
|---|---|---|
| Start | `request_json: Paragraph` | 任务要求、评分标准和学生提交 |
| Generate Assessment Draft | `request_json` | `structured_output` |
| End | `Generate Assessment Draft/structured_output` | `result` |

### 20.3 Start 输入示例

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

### 20.4 LLM 节点输入与提示词

LLM 节点输入：

```text
{{request_json}}
```

系统提示词：

```text
你是高校课程作业辅助评阅助手。

请根据任务要求、评分标准和学生提交内容，生成一份供教师复核的评阅草稿。

规则：
1. 评分必须以输入的 rubric 为依据。
2. 不要因为提交内容较长或代码看起来完整就默认正确。
3. 无法确认的部分必须降低 confidence，并在 summary 中说明。
4. 这只是辅助评阅，needsTeacherReview 必须为 true。
5. 不要修改任务要求、评分标准或学生原始提交。
6. score 和各维度分数必须是 0 到 100 的整数。
7. 输出使用简体中文，只返回 JSON。

评阅材料：
{{request_json}}
```

### 20.5 LLM 节点输出 Schema

```json
{
  "type": "object",
  "properties": {
    "score": {"type": "number"},
    "dimensions": {
      "type": "object",
      "properties": {
        "内容完整性": {"type": "number"},
        "知识点覆盖度": {"type": "number"},
        "逻辑结构": {"type": "number"},
        "表达规范": {"type": "number"},
        "任务要求符合度": {"type": "number"}
      },
      "required": ["内容完整性", "知识点覆盖度", "逻辑结构", "表达规范", "任务要求符合度"]
    },
    "summary": {"type": "string"},
    "suggestions": {"type": "array", "items": {"type": "string"}},
    "riskLevel": {"type": "string"},
    "confidence": {"type": "number"},
    "needsTeacherReview": {"type": "boolean"}
  },
  "required": ["score", "dimensions", "summary", "suggestions", "riskLevel", "confidence", "needsTeacherReview"]
}
```

### 20.6 修改建议

- 不让 Dify 直接修改成绩表，只保存评阅草稿。
- 客观题继续由后端规则判分，不要把所有题目都交给 LLM。
- 附件或代码文件需要先由后端转换成可控文本，再传入 Dify。
- 保留学生原始提交、AI 草稿和教师最终结果三个版本。
- 作业评阅耗时较长，建议使用异步任务和最近结果缓存。

## 21. Workflow 六：批量个性化推荐理由

### 21.1 应用信息

```text
应用名称：learning-recommendation
capability：recommend
用途：一次生成一名学生的全部推荐理由
```

### 21.2 为什么要批量

当前后端会按能力点循环调用 AI。如果一名学生有 10 个能力点，就可能发生 10 次模型调用。迁移时应该把输入改成一个推荐数组，一次生成所有理由。

### 21.3 节点和连线

```text
Start → Generate Recommendation Reasons（LLM）→ End
```

| 节点 | 输入 | 输出 |
|---|---|---|
| Start | `request_json: Paragraph` | 学生匿名信息和推荐对象数组 |
| Generate Recommendation Reasons | `request_json` | `structured_output` |
| End | `Generate Recommendation Reasons/structured_output` | `result` |

### 21.4 Start 输入示例

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

### 21.5 LLM 节点输入与提示词

LLM 节点输入：

```text
{{request_json}}
```

系统提示词：

```text
你是个性化学习推荐助手。

请为输入中的每个推荐对象生成一句不超过 40 个汉字的中文推荐理由。

规则：
1. 每个输入对象必须对应一条输出。
2. 必须原样保留 targetId。
3. 理由必须结合 targetName、score 和 type。
4. 不要虚构学生没有提供的学习行为。
5. 不要输出心理、医疗或升学结论。
6. 不要遗漏输入对象，也不要新增输入中不存在的 targetId。
7. 输出使用简体中文，只返回 JSON。

推荐数据：
{{request_json}}
```

### 21.6 LLM 节点输出 Schema

```json
{
  "type": "object",
  "properties": {
    "reasons": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "targetId": {"type": "string"},
          "reason": {"type": "string"},
          "confidence": {"type": "number"}
        },
        "required": ["targetId", "reason", "confidence"]
      }
    }
  },
  "required": ["reasons"]
}
```

### 21.7 修改建议

- 后端先根据分数决定 `type` 和 `priority`，Dify 只生成理由。
- 后端检查输出的 targetId 集合必须和输入一致。
- 如果某个理由生成失败，使用现有本地模板，不要让整个推荐列表失败。
- 推荐理由可以缓存，缓存键使用 `student + course + abilityScoreVersion`，不要每次打开页面都调用。
- 如果推荐对象超过 10 个，可以在后端分批，每批最多 10 个，不要让 Dify 一次处理过大的输入。

## 22. 四个 Workflow 的共同发布要求

每个 Workflow 发布前都要确认：

- [ ] Start 输入变量名为 `request_json`。
- [ ] LLM 节点引用的是输入变量，而不是把测试数据写死在提示词里。
- [ ] LLM 使用结构化输出。
- [ ] End 节点返回结构化输出变量。
- [ ] 正常、空数据和异常数据测试通过。
- [ ] API Key 只放后端。
- [ ] 后端会校验 ID、数字范围和输出数量。
- [ ] 慢任务由后端异步调用。
- [ ] 失败时有规则或本地文本兜底。
- [ ] 发布后记录 Workflow 版本。

## 23. 迁移顺序

建议实际操作顺序：

1. 已完成并测试：错题问题聚类。
2. 教学建议。
3. 爬塔诊断报告。
4. 批量个性化推荐理由。
5. 作业智能评阅。

学习风险检测不在迁移队列中，已由后端规则引擎完成。推荐先做教学建议和诊断报告；诊断报告虽然结构简单，但必须同步保持异步；作业评阅则必须保留教师复核，不能因为 Workflow 成功就直接视为最终成绩。
