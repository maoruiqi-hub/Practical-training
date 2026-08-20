# Dify 五个 Workflow 真实联调报告

> 测试日期：2026-08-20  
> 测试分支：`summer-update`  
> 测试方式：`AgenticClient -> DifyClient -> /v1/workflows/run` 真实调用  
> 说明：本文不记录 API Key、完整请求内容或完整模型输出。

## 最终结论

充值后使用完整业务输入重新联调，5 个 Workflow 均成功到达并执行 Dify。

| Workflow | 结果 | 结论 |
|---|---|---|
| `clusterProblems` | 成功，返回 3 条聚类 | 后端题目统计透传和读取超时问题已修复，端到端通过 |
| `teachingSuggestions` | 成功，返回 4 条建议 | 已验证新的中文目标标签格式，端到端通过 |
| `tower-diagnosis-report` | 成功，返回具体诊断字段 | 端到端通过 |
| `assessment` | 成功，返回分数、维度、摘要和建议 | 后端已兼容维度数组，并补传评分规则 |
| `recommend` | 成功，返回 3 条带 `targetId` 的推荐理由 | 端到端通过 |

## 已修复的后端问题

### 1. 错题聚类题目统计没有透传

原实现固定发送空的 `questionStats`，导致 Workflow 无法使用题目错误数量、错误率和常见错误模式。

现已修改 `AgenticClient.buildClusterWorkflowInput()`：

- 优先读取上游 `questionStats`，兼容读取旧字段 `questions`；
- 规范化传入 `questionId`、`knowledgePointId`、`wrongCount`、`wrongRate`、`commonWrongPatterns`；
- 增加单元测试验证字段透传。

修复后使用同一组完整测试数据复测，Workflow 返回 3 条有效聚类，包含知识点 ID、题目 ID、错误模式、建议动作和置信度，端到端通过。

### 2. 作业评阅维度格式不兼容

真实 Workflow 返回的 `dimensions` 是数组，例如：

```json
[
  {"name":"问题分析","score":8,"reason":"..."},
  {"name":"实验过程","score":5,"reason":"..."}
]
```

原后端只支持对象格式，会把固定维度解析为 0。现已支持：

- 原有对象格式：`{"内容完整性": 80}`；
- Workflow 实际返回的数组格式：`[{"name":"问题分析","score":8}]`；
- 缺少的固定维度补 0，保持数据库结构稳定。

### 3. 生产评阅没有传评分标准

原生产代码始终发送空 `rubric`。现已从 `LearningTask.gradingRule` 生成评分规则：

- JSON 数组：按结构化数组传入；
- JSON 对象：按单项规则传入；
- 普通文本：包装为描述项传入；
- 没有评分规则：保持空数组。

## Workflow 更新后的验证

`teachingSuggestions.target` 已重新联调验证。当前返回的目标标签已从之前的具体知识点描述调整为更稳定的中文受众标签，例如：

- `全班`
- `9人小组`
- `教学计划及8名风险学生`

本次后端只做透传，4 条建议均正常返回。若后续前端或数据库需要机器筛选，建议在 Workflow 输出中同时保留稳定的 `targetType`（如 `whole_class|group|individual`）和展示用的 `target` 文本；当前中文标签可作为展示字段使用。

## 验证结果

后端完整测试：359 项通过，5 项真实联调测试默认跳过。新增的聚类输入透传测试通过。

真实联调使用的测试文件仍保留 `@Disabled`，避免普通构建消耗 Dify 配额。人工复测时临时移除注解：

```bash
cd backend
mvn -q -Dtest=DifyWorkflowLiveTest test
```
