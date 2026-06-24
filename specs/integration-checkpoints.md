# 模块集成检查点

> **定位**：五人模块合并前必须通过的检查清单。覆盖模块间依赖、接口兼容性、爬塔游戏系统端到端验证。
> **阅读前置**：[../specs/模块接口与协作规范.md](../specs/模块接口与协作规范.md)（接口契约）、各模块 plan 文件
> **状态**：草拟，待团队评审

---

## 检查点 0：共享基础设施（全员，合并前）

- [ ] **0.1 共享常量已引入**
  - `common/GameEventTypes.java` — 所有人发送/接收游戏事件时使用常量，不用裸字符串
  - `common/SharedIds.java` — 所有模块生成 ID 时用 `SharedIds.newId()` 而非自造 UUID
  - 验证：`grep -r '"answer_correct"' src/` 在全代码库中不出现（应使用 `GameEventTypes.ANSWER_CORRECT`）

- [ ] **0.2 ID 格式统一**
  - 所有新表主键为 `VARCHAR(36)`，代码中 `@TableId(type = IdType.ASSIGN_UUID)`
  - 现有 INT AUTO_INCREMENT 表暂不强制迁移，但跨模块引用字段（如 `student_id`）必须为 `VARCHAR(36)`
  - 验证：`mvn test` 全部通过，无类型不匹配

- [ ] **0.3 表名前缀对齐**
  - 各模块建表 SQL 按 §12.3 约定的前缀命名
  - 模块1: `course_`, `resource_`, `knowledge_point_`, `knowledge_relation_`, `ability_point_`
  - 模块2: `task_`, `submission_`, `learning_log_`
  - 模块3: `question_`, `exam_`, `answer_`, `grade_result_`
  - 模块4: `student_`, `profile_`, `competency_score_`, `recommendation_`, `achievement_`
  - 模块5: `analytics_class`, `analytics_risk_alert`, `analytics_report`, `analytics_teaching_suggestion`
  - 验证：`grep -r 'CREATE TABLE' src/main/resources/schema.sql` 确认所有表名

- [ ] **0.4 Mapper XML 可扫描**
  - `application.yml` 中 `mapper-locations: classpath*:/mapper/**/*.xml`
  - 每个模块的 Mapper XML 放在 `mapper/<模块包名>/` 子目录下
  - 验证：应用启动日志无 "mapper not found" 警告

---

## 检查点 1：Phase 1 完成 — 各自 CRUD + ID 可用（Week 2 末）

**目标**：每个模块至少一个实体可 CRUD，产出的 ID 格式已告知全队。

- [ ] **1.1 模块1**：CourseID / KnowledgePointID 可产出 → 告知全队 ID 格式
- [ ] **1.2 模块2**：TaskID 可产出 → 告知全队 ID 格式
- [ ] **1.3 模块3**：QuestionID 可产出 → 告知全队 ID 格式
- [ ] **1.4 模块4**：StudentID 可产出 → 告知全队 ID 格式
- [ ] **1.5 模块5**：ClassID 可产出 → 告知全队 ID 格式 ✅（已实现，UUID v4）

- [ ] **1.6 交叉验证**
  - 每人用 Postman 调通其他人的一个 GET 端点
  - 确认返回的 ID 格式与告知的一致

---

## 检查点 2：Phase 2 完成 — 核心业务 + 跨模块调用（Week 5 末）

**目标**：核心业务流程跨模块走通，Mock 全部替换为真实调用。

- [ ] **2.1 模块2 ↔ 模块4**（学生提交任务）
  - 模块4 的 StudentID 被模块2 正确引用
  - `POST /api/tasks/{id}/submit` → 模块2 记录 → 模块4 可查询该生的 Submission 列表
  - 验证：教师创建任务 → 学生提交 → 模块4 查询画像时包含该提交

- [ ] **2.2 模块1 ↔ 模块3**（试题关联知识点）
  - 模块3 的 Question 可关联模块1 的 KnowledgePointID
  - `POST /api/questions/{id}/link-kp` → 关联成功
  - 验证：创建试题时指定 KnowledgePointID → 查询试题时返回关联的知识点信息

- [ ] **2.3 模块3 ↔ 模块5**（成绩分析）
  - 模块5 调用模块3 的 `getStudentScores()` 获取成绩数据
  - 模块5 调用模块3 的 `getClassMistakeStats()` 获取错题统计
  - 验证：`GET /api/classes/{id}/scores?course_id=X` → 返回包含真实成绩的聚合数据

- [ ] **2.4 模块2 ↔ 模块5**（进度监控）
  - 模块5 调用模块2 的 `getStudentProgress()` / `getTaskCompletionStats()`
  - 验证：`GET /api/classes/{id}/progress?course_id=X` → 返回真实进度数据

- [ ] **2.5 🔴 模块4 ↔ 模块5 循环依赖已解除**
  - 应用启动无 `BeanCurrentlyInCreationException`
  - 模块5 向模块4 查询学生列表正常
  - 模块4 向模块5 查询班级信息正常
  - **解除方式**：模块5 注入模块4 依赖处已加 `@Lazy`；模块4 注入模块5 依赖处同理
  - 验证：`mvn spring-boot:run` 启动成功，两个模块的端点均可访问

- [ ] **2.6 模块5 风险检测**
  - `POST /api/classes/{id}/risk-detect?course_id=X` → 遍历全班学生 → 拉取真实数据 → 生成预警
  - 验证：制造模拟数据（低分学生 + 拖延学生）→ 检测到对应预警类型

---

## 检查点 3：Phase 3 完成 — 爬塔游戏系统端到端（Week 8 末）

**目标**：爬塔事件链路全部走通，`game_mode_enabled = true`。

- [ ] **3.1 游戏事件字符串一致性**
  - 各模块发送事件时使用 `GameEventTypes.ANSWER_CORRECT` 等常量
  - 模块4 接收事件时使用同一常量比较
  - 验证：`grep -rn '"answer' src/` 不出现裸字符串；`grep -rn 'GameEventTypes.' src/` 在所有发事件模块中出现

- [ ] **3.2 答题事件链路（模块3 → 模块4）**
  - 学生答题 → 模块3 判分 → 发 `answer_correct` / `answer_wrong` 到模块4
  - 模块4 接收 → 更新 HP/ATK/EXP/金币/连胜
  - 验证：答对一题 → 调用 `GET /api/students/{id}/profile` → HP/EXP/金币有变化

- [ ] **3.3 楼层通关链路（模块1 → 模块4）**
  - 某层所有题完成 → 模块1 发 `floor_cleared` → 模块4 更新楼层状态
  - 模块4 回调模块1 `PUT /api/knowledge-points/{id}/floor-status` 解锁下一层
  - 验证：通关 1F → 2F 由 🔒 变为 🔓

- [ ] **3.4 Boss 战链路（模块3 → 模块4）**
  - Boss 层批改完成 → 模块3 发 `boss_defeated` → 模块4 发大额奖励
  - 验证：Boss 通关后 EXP +150~400，金币 +300~800

- [ ] **3.5 风险事件链路（模块4 → 模块5）**
  - 学生 HP 降到 30 以下 → 模块4 发 `hp_critical` → 模块5 创建 RiskAlert
  - 同一知识点连败 3 次 → 模块4 发 `stuck_detected` → 模块5 创建 RiskAlert
  - 验证：
    1. 制造 HP<30 场景 → `GET /api/classes/{id}/risk-alerts` 出现 `hp_critical` 预警
    2. 制造连续答错场景 → 预警列表出现 `stuck` 预警

- [ ] **3.6 补给事件链路（模块2 → 模块4）**
  - 学生使用补给 → 模块2 发 `supply_used` → 模块4 扣金币/恢复HP
  - 验证：使用生命药水 → HP 回升 → `supply_used` 日志可见

- [ ] **3.7 塔地图查询**
  - `GET /api/students/{id}/tower-map?course_id=X` → 返回 14 层的四色状态
  - 数据来自模块1（前置依赖）+ 模块3（成绩）+ 模块4（掌握度计算）
  - 验证：不同进度的学生看到不同的塔地图颜色

- [ ] **3.8 game_mode_enabled 开关**
  - `GET /api/courses/{id}/config` 返回 `game_mode_enabled`
  - 各模块在 `game_mode_enabled = false` 时不发送游戏事件
  - 验证：关闭游戏模式 → 答题后 `GET /api/students/{id}/profile` HP/EXP 不变

---

## 检查点 4：Phase 4 完成 — 全链路（Week 10 末）

- [ ] **4.1 教师端全流程**
  - 创建课程 → 创建班级 → 添加学生 → 发布任务 → 学生答题 → 教师查看成绩/进度/预警/聚类/建议
  - 验证：端到端手工走通，无 500 错误

- [ ] **4.2 学生端爬塔流程**
  - 登录 → 看到塔地图（四色标记）→ 进入楼层 → 诊断 → 战斗 → 通关 → 解锁下一层 → Boss → 区域通关
  - 验证：手工爬通 1F→4F，属性面板实时更新

- [ ] **4.3 Agentic fallback**
  - 关停 agentic 服务 → 聚类/建议接口返回 503 "AI 服务暂不可用"
  - 成绩/进度/风险/报表功能不受影响
  - 验证：`docker stop agentic` → 核心功能正常 → 聚类/建议返回友好错误

- [ ] **4.4 前端集成**
  - 所有 API 的返回字段名、错误码格式与前端约定一致
  - 验证：前端对接人独立通过 Postman Collection 调通全部接口

---

## 红线（不合规 = 不准合并）

1. ❌ 裸字符串发游戏事件（必须用 `GameEventTypes` 常量）
2. ❌ 绕过模块边界直接查其他模块的数据库表
3. ❌ ID 格式不一致导致跨模块引用失败
4. ❌ `mvn spring-boot:run` 启动失败（循环依赖未解决）
5. ❌ agentic 不可用时系统崩溃（500）而非降级（503）

---

> 📁 本文档属于 `docs/` — 合并前的集成验收清单。检查点通过后由全员签字确认。
