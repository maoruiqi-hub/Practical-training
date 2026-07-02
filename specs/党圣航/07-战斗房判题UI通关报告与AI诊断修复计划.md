# 剩余三项任务修复实施计划

## 一、说明

原计划中的“图一题目判定错误”已经进入实际修复阶段：前端战斗房/侦察房已接入统一答案匹配工具，后端任务提交评分已支持 `A/B/C/D` 与选项文本互认，并补充了“自动关闭文件资源”中文用例。

本计划覆盖原 `07-战斗房判题UI通关报告与AI诊断修复计划.md`，只针对剩余三个任务重新制定可执行方案：

- 图二：侦察房、战斗房 UI 调整。
- 图三：通关报告正确率改为战斗房答题正确率。
- 图四：诊断结果必须真实来自 AI，AI 生成中显示加载，明确失败后再显示失败与重试。

## 二、涉及文件

前端：

- `frontend/src/views/FloorView.vue`
- `frontend/src/components/GameHud.vue`
- `frontend/src/components/BattleRoom.vue`
- `frontend/src/components/BossRoom.vue`
- `frontend/src/components/DiagnosisRoom.vue`
- `frontend/src/components/AiTutorPanel.vue`
- `frontend/src/api/index.js`

后端：

- `backend/src/main/java/com/neu/CoursePlatform/service/impl/TowerRunServiceImpl.java`
- `backend/src/main/java/com/neu/CoursePlatform/service/TowerRunService.java`
- `backend/src/main/java/com/neu/CoursePlatform/agentic/AgenticClient.java`
- `backend/src/main/java/com/neu/CoursePlatform/agentic/AgenticRequest.java`
- `backend/src/main/java/com/neu/CoursePlatform/entity/StudentTowerAttempt.java`
- `backend/src/main/java/com/neu/CoursePlatform/mapper/StudentTowerAttemptMapper.java`

建议新增：

- `backend/src/main/java/com/neu/CoursePlatform/service/AnswerMatchingService.java`
- `backend/src/test/java/com/neu/CoursePlatform/service/impl/TowerRunServiceImplCorrectRateTest.java`
- `backend/src/test/java/com/neu/CoursePlatform/service/impl/TowerRunServiceImplAiDiagnosisTest.java`

## 三、任务一：侦察房、战斗房 UI 调整

### 1. 问题拆解

用户要求：

- 删除顶部“等级”右侧 HP 血条。
- 删除“学习者 HP”下方的“提示 / AI 导师 / 护盾 / 跳过”按钮。
- 右上角 AI 导师按钮保留。
- 侦察房、战斗房都要调整。
- 敌人 HP 放到敌人头顶。
- “概念攻击 / 答错 -10HP”整体右移。

当前定位：

- 顶部 HP 来自 `GameHud.vue` 的 `hp-block`，在 `FloorView.vue` 中调用 `<GameHud :profile="profile" :course-name="courseName" compact />` 时默认显示。
- 战斗房左上角学习者 HP 和敌人 HP 来自 `BattleRoom.vue` 的 `.combat-mini-hud`。
- “提示 / AI 导师 / 护盾 / 跳过”来自 `BattleRoom.vue` 的 `.scene-tools`。
- 右上角 AI 导师按钮来自 `FloorView.vue` 的 `.header-actions`，必须保留。
- Boss 房 `BossRoom.vue` 复用 `BattleRoom.vue`，战斗房改动会自然覆盖 Boss 房。

### 2. 实施步骤

1. `FloorView.vue`
   - 将 GameHud 调用改为 `:show-hp="false"`。
   - 只影响爬塔房间页，不改变其它页面默认 HUD。
   - 保留右上角 `AI 导师` 和 `补给` 按钮。

2. `BattleRoom.vue`
   - 移除 `.combat-mini-hud` 中的学习者 HP。
   - 移除 `.combat-mini-hud` 中原右侧敌人 HP。
   - 在 `.enemy-actor` 内新增 `.enemy-overhead-hp`，包含敌人名称、`enemyHp/enemyMaxHp`、血条。
   - 删除模板中的 `.scene-tools`，不再显示“提示 / AI 导师 / 护盾 / 跳过”。
   - 保留内部方法 `useHint/gainBlock/skipQuestion` 的清理作为同一提交内的代码整理项：若完全无引用则删除，避免死代码。
   - 调整 `.intent-bubble` 的 `right/top/transform`，让“概念攻击 / 答错 -10HP”移动到敌人右上侧，避免压住敌人头顶血条。

3. `DiagnosisRoom.vue`
   - 确认侦察房没有战斗工具条。
   - 如果“直接进入战斗”仍需保留，作为单独行动按钮保留在底部，不归入“学习者 HP 下方工具条”。
   - 确认顶部 HP 已通过 `FloorView.vue` 关闭。

4. CSS 响应式修正
   - 桌面 1920×1080 下：敌人 HP 在敌人头顶，不遮挡题干和选项。
   - 窄屏下：敌人 HP 仍跟随敌人，不与右侧选项栈重叠。
   - 意图提示与敌人 HP 至少保留 12px 间距。

### 3. 验收标准

- 顶部等级右侧不再出现 HP 血条。
- 战斗房不再出现“提示 / AI 导师 / 护盾 / 跳过”按钮。
- 右上角 AI 导师按钮仍可打开 AI 导师面板。
- 战斗房、精英房、Boss 房敌人 HP 都在敌人头顶。
- 侦察房同样没有顶部 HP。
- “概念攻击 / 答错 -10HP”明显右移，不遮挡敌人 HP。

## 四、任务二：通关报告正确率改为战斗房答题正确率

### 1. 问题拆解

用户要求：通关报告的正确率有时不正确，需要判断“我在战斗房答题的正确率”。

当前风险：

- `BattleRoom.vue` 的 `currentCorrectRate()` 目前用 `correctCount / denominator`，分母混合了可评分题数量、已答数量、题目总数。敌人提前被击败、失败提前结束、存在非自动评分题时都可能出错。
- `FloorView.vue` 的通关报告直接显示 `battleResult.correctRate`。
- `TowerRunServiceImpl.completeNode` 当前信任前端传入的 `correctRate`，没有从 `answerSummary` 做后端复算。
- 诊断房的 `correctRate` 和战斗房 `correctRate` 在前端状态中容易混用。

### 2. 正确率口径

统一口径如下：

- 只统计战斗房中实际结算过的题。
- 单选、多选、填空属于自动评分题，计入分母。
- 简答、编程如果没有 AI/规则评分结果，不计入分母。
- 跳过按钮按图二要求删除后，新流程不产生跳过；若历史数据或接口仍传入 `skipped=true`，计入分母且视为错误。
- 诊断房正确率不得出现在“通关报告”的战斗正确率字段中。

### 3. 实施步骤

1. 前端 `BattleRoom.vue`
   - 新增 `answerRecords = ref([])`。
   - 每次 `resolveCurrentAnswer` 结算时写入：
     - `questionId`
     - `type`
     - `autoGradable`
     - `answered`
     - `skipped`
     - `correct`
     - `studentAnswer`
     - `correctAnswer`
     - `knowledgePointId`
     - `source: 'battle_room'`
   - `currentCorrectRate()` 改为只从 `answerRecords` 中统计 `autoGradable=true` 的记录。
   - `answerSummary` 直接使用 `answerRecords`，并补齐未进入分母的非自动评分题。

2. 前端 `FloorView.vue`
   - `handleBattleEnd` 中把 `battleResult` 和 `diagnosisResult` 明确拆开。
   - 通关报告显示：
     - 优先后端返回的 `payload.correctRate`。
     - 其次前端 `result.battleCorrectRate`。
     - 不再用 `diagnosisResult.correctRate` 兜底通关报告。
   - 如果是诊断全对跳过战斗，报告文案显示“诊断正确率”，不显示为“战斗正确率”。

3. 后端共用判题能力
   - 把 `TaskSubmissionServiceImpl` 中已修复的答案匹配逻辑抽为 `AnswerMatchingService`。
   - `TaskSubmissionServiceImpl` 调用该服务。
   - `TowerRunServiceImpl` 也调用该服务，用 questionId 读取题目后复算 `answerSummary.correct`。

4. 后端 `TowerRunServiceImpl.completeNode`
   - 新增 `calculateBattleCorrectRate(request)`：
     - 从 `answerSummary` 中筛选 `source='battle_room'` 或当前 node 的题目记录。
     - 自动评分题用 `AnswerMatchingService` 复算。
     - 如果没有可评分记录，返回前端传入值但标记 `correctRateSource='client_fallback'`。
   - 保存 `StudentTowerAttempt.correctRate` 时使用后端复算值。
   - 返回体增加：
     - `correctRate`
     - `correctRateSource`
     - `gradedCount`
     - `correctCount`

### 4. 测试用例

- 战斗房 5 题，只答 3 题且提前击败敌人，正确率为 `3 题中的正确数 / 3`。
- 战斗房 5 题，答对 2、答错 1、历史跳过 1，正确率为 `2 / 4`。
- 诊断房 100%，战斗房 60%，通关报告必须显示 60%。
- 包含编程题且未评分时，编程题不计入分母。
- 后端保存到 `student_tower_attempt.correct_rate` 的值与通关报告一致。

## 五、任务三：诊断结果必须真实来自 AI

### 1. 问题拆解

用户指出图四诊断结果不是 AI 给出的。

当前定位：

- `TowerRunServiceImpl.diagnoseNode` 会调用 `diagnosisReport(...)`，但 AI 调用失败或 mock 返回空内容时，会静默使用固定兜底文案。
- `TowerRunServiceImpl.completeNode` 战斗完成路径目前只记录 attempt，没有生成 AI 诊断报告。
- `FloorView.vue.handleBattleEnd` 在后端返回前会先设置一段本地固定 report，后端如果没有返回 AI report，这段固定文案会继续显示。
- `AgenticClient.invoke("tower-diagnosis-report", ...)` 没有专门的结构化 mock，也没有把 AI 不可用原因传给前端。

### 2. 目标行为

- 战斗完成后，诊断报告必须走真实 AI 调用，不能用本地固定文案冒充 AI。
- 侦察诊断完成后，诊断报告必须走真实 AI 调用，不能只展示前端或后端模板。
- AI 诊断还没生成完成时，前端只显示“AI 诊断生成中”的加载态，不显示规则兜底、不显示失败态、不显示“降级来源”。
- 只有后端明确返回失败、超时或配置不可用时，前端才展示“AI 诊断生成失败 / 重试”。
- 开发环境如果使用 mock，需要明确标记为“模拟 AI”，不能作为验收通过依据。
- 后端 attempt 中保存 `aiReportJson`，并记录 `aiReportStatus=pending|success|failed|mock`、失败原因和调用耗时，方便确认是否真的调用了 AI。

### 3. 后端实施步骤

1. `TowerRunServiceImpl`
   - 新增 AI 诊断状态机：
     - `pending`：AI 正在生成，前端展示加载态。
     - `success`：AI 已返回有效诊断，前端展示 AI 结果。
     - `failed`：AI 明确失败、超时或配置不可用，前端展示失败和重试。
     - `mock`：开发模拟 AI，仅用于联调，不作为验收通过。
   - 创建诊断报告请求时先写入 `pending` 状态，禁止在 pending 阶段写入本地模板报告。
   - 新增统一方法 `buildTowerDiagnosisReport(run, node, request, correctRate, cleared, sourceStage)`。
   - `sourceStage` 支持：
     - `diagnosis_room`
     - `battle_room`
   - `diagnoseNode` 调用该方法替换旧 `diagnosisReport`。
   - `completeNode` 在复算正确率后也调用该方法，并将 report 写入 `recordAttempt(..., report)`。

2. AI 请求上下文
   - 传给 AI 的 context 至少包含：
     - `studentNo`
     - `courseCode`
     - `knowledgePointId`
     - `roomType`
     - `stage`
     - `correctRate`
     - `cleared`
     - `answers`
     - `wrongAnswers`
     - `questionCount`
   - AI prompt 要求返回 JSON：
     - `summary`
     - `weaknesses`
     - `recommendedAction`
     - `reviewFocus`
     - `source`

3. `AgenticClient`
   - 给 `tower-diagnosis-report` 增加明确 system prompt。
   - 增加 `isConfiguredForRealAi()` 或等价配置检查：
     - `deepseek` 必须存在 `ANTHROPIC_AUTH_TOKEN`。
     - `dify` 必须存在对应 API key。
     - `http` 必须配置可用 `agentic.base-url`。
   - mock 模式只允许开发联调，返回结构化模拟 AI 文案并标记 `source='mock_ai'`。
   - deepseek/dify/http 调用失败时返回失败原因，不生成规则诊断内容。

4. AI 失败处理
   - 如果 AI 调用仍在进行，后端返回生成中状态，而不是规则诊断：
     - `aiAvailable=null`
     - `aiReportStatus='pending'`
     - `errorMessage=null`
     - `retryable=false`
     - `diagnosis=null`
   - 前端收到 `pending` 后继续显示加载态，并按固定间隔轮询报告状态。
   - 如果 AI 不可用，后端返回失败态，而不是规则诊断：
     - `aiAvailable=false`
     - `aiReportStatus='failed'`
     - `errorMessage`
     - `retryable=true`
     - `diagnosis=null`
   - 数据库 attempt 记录失败状态和失败原因，便于排查配置或网络问题。
   - 不把 `fallback_rule` 作为最终报告来源，除非用户明确要求“AI 不可用时用规则兜底”。
   - 超时策略建议：
     - 0-30 秒：保持“AI 诊断生成中”。
     - 超过后端配置的最长等待时间仍无结果：后端标记 `failed`，前端显示失败和重试。

5. 返回体
   - `completeNode` 返回 `diagnosis/report`。
   - `diagnoseNode` 返回 `diagnosis/report`。
   - 返回体中包含 `aiAvailable`、`aiReportStatus`、`reportSource`、`errorMessage`、`retryable`。
   - `aiReportStatus='pending'` 时，`diagnosis/report` 为空，前端不得使用本地模板补齐。

6. 重试接口
   - 增加报告状态查询接口，例如：
     - `GET /api/students/{studentId}/tower-run/{runId}/nodes/{nodeId}/diagnosis-report`
   - 前端在 `pending` 状态下轮询该接口，直到返回 `success` 或 `failed`。
   - 增加或复用一个后端接口用于重新生成诊断报告，例如：
     - `POST /api/students/{studentId}/tower-run/{runId}/nodes/{nodeId}/diagnosis-report/retry`
   - 前端点击“重试 AI 诊断”时调用该接口。
   - 重试成功后更新 attempt 的 `aiReportJson` 和前端诊断卡片。

### 4. 前端实施步骤

1. `FloorView.vue`
   - `handleBattleEnd` 初始只显示“AI 诊断生成中”，不再伪造最终诊断。
   - `completeTowerNode` 如果返回 `aiReportStatus='pending'`，保持加载态并启动轮询。
   - 轮询过程中展示 loading/skeleton，不展示任何固定诊断文案。
   - `completeTowerNode` 或轮询返回 `success` 后，用后端返回的 `diagnosis/report` 覆盖诊断卡片。
   - 如果 `aiAvailable === false` 或 `aiReportStatus === 'failed'`，显示“AI 诊断生成失败”，并提供“重试 AI 诊断”按钮。
   - 不显示“以下为规则诊断”作为最终诊断结果，避免用户误以为 AI 已给方案。
   - 如果 `report.source === 'mock_ai'`，显示“模拟 AI 诊断”，并且该状态不算验收通过。

2. `DiagnosisRoom.vue` 与 `FloorView.vue`
   - 诊断房完成后继续调用 `diagnoseTowerNode`。
   - 诊断卡片只展示后端 report，不再混入本地模板。
   - 诊断房提交后如果 AI 仍在生成，同样显示“AI 诊断生成中”，直到状态变为 `success` 或 `failed`。

### 5. 测试用例

- AI 成功返回 JSON：前端展示 AI summary、weaknesses、recommendedAction。
- AI 返回纯文本：后端包装成 `summary`，source 标为 `ai_text`。
- AI 正在生成：前端显示“AI 诊断生成中”加载态，不展示规则诊断、不展示失败态。
- AI 从生成中变为成功：加载态自动替换为 AI 报告。
- AI 未配置：前端显示“AI 诊断生成失败 / 重试”，不展示规则诊断内容。
- AI 调用失败后点击重试，成功时替换为真实 AI 报告。
- 战斗房失败后也生成 report，并保存到 `StudentTowerAttempt.aiReportJson`。
- 诊断房全对跳过战斗时，诊断报告 source 仍可识别。
- 验收环境必须使用真实 AI 配置，`mock_ai` 不算通过。

## 六、执行顺序

1. 先做任务二：正确率口径和后端复算。
   - 原因：AI 诊断依赖正确率和错题数据，先把数据源打准。

2. 再做任务三：AI 诊断报告。
   - 原因：AI 输入需要使用任务二整理出的 `answerSummary` 和后端复算结果。

3. 最后做任务一：UI 调整。
   - 原因：逻辑联调完成后再改布局，能减少调试时界面频繁变化。

## 七、验证清单

- `mvn -Dtest=TaskSubmissionServiceImplAnswerMatchingTest,TowerRunServiceImplCorrectRateTest,TowerRunServiceImplAiDiagnosisTest test`
- `npm run build`
- 手动验证战斗房：
  - 顶部无 HP。
  - 无“提示 / AI 导师 / 护盾 / 跳过”按钮。
  - 右上角 AI 导师仍可打开。
  - 敌人 HP 在敌人头顶。
  - 意图提示右移。
- 手动验证通关报告：
  - 战斗房答题正确率与后端 attempt 保存值一致。
  - 诊断正确率不污染战斗正确率。
- 手动验证 AI 诊断：
  - AI 未生成完成时展示“AI 诊断生成中”。
  - AI 生成完成后再展示 AI 报告。
  - AI 可用时展示 AI 报告。
  - AI 不可用时展示失败态和重试按钮，不再展示规则诊断冒充 AI。
  - 验收时确认 `agentic.mode` 是 `deepseek/dify/http` 中的一种真实 AI 模式，并且对应 token/base-url 可用。
