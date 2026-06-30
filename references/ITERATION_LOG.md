# Iteration Log

---

## [b15c71e] 2026-06-30 20:04:00 +0800

**Goal of this increment:** 将商店节点从通用弹窗中拆出，接上错题本数据和后端 `shop_purchased` 房间事件能力。

**What was changed:**
- `frontend/src/components/ShopRoom.vue` — 新增独立错题商店组件，复用现有商店背景和商人图标；进入房间时调用错题接口并读取 `wrongList`，展示待处理错题卡、金币和两个购买动作。
- `frontend/src/views/TowerMap.vue` — 当当前节点为 `shop` 时渲染 `ShopRoom`，商店完成后沿用地图的 `completeRoom`，由现有 `shop_purchased` 事件同步后端画像增量。

**Current state of the task:** 商店节点已经拥有独立交互；提示卡购买会发送 `rewardName=hint_card`，净化错题会发送 `rewardName=clean_wrong_card`，后端监听器据此扣金币并增加攻击或防御/经验。宝箱节点保持可用。休息点尚未拆出。

**Next step:** 新建 `RestSiteRoom.vue` 并改造 `TowerMap.vue` 的 `rest` 分支，使用 `rest_taken` 和可选补给入口连接后端画像恢复能力。

**Known risks / blockers:** 错题净化当前不会从错题本后端删除题目，只把错题作为商店交互来源并触发画像收益；如果学生金币不足，前端会禁用动作，后端仍以事件为准。

**What was intentionally NOT touched:** 未改错题本页面，未修改后端商店收益规则，未处理休息点和通用 `GameRoomModal` 中残留的旧商店分支。

---

## [f102335] 2026-06-30 20:00:25 +0800

**Goal of this increment:** 先把宝箱节点从通用房间弹窗中拆出来，接上课程资源后端能力，形成一个可构建、可回退的小切片。

**What was changed:**
- `frontend/src/components/TreasureRoom.vue` — 新增独立宝箱房组件，复用现有宝箱背景和地图图标；打开时按课程和当前知识点读取课程资源，领取资源时记录资源浏览 `start` 事件，并通过 `room-complete` 交还地图流程。
- `frontend/src/views/TowerMap.vue` — 当当前节点为 `treasure` 时渲染 `TreasureRoom`，其他非战斗节点继续沿用原 `GameRoomModal`，避免一次性扩大改动范围。

**Current state of the task:** 宝箱节点已经拥有独立交互，并连接 `/api/resources` 与 `/api/resources/{resourceId}/view-events`；构建通过，但仍存在项目既有的包体积 warning。商店和休息点尚未拆出。

**Next step:** 新建 `ShopRoom.vue` 并改造 `TowerMap.vue` 的 `shop` 分支，正确读取错题接口返回的 `wrongList`，保留宝箱切片不动。

**Known risks / blockers:** 宝箱资源查看事件依赖学生登录态；如果接口因会话失效失败，当前组件会提示记录失败但仍允许完成房间。`treasure_opened` 事件目前后端监听器未处理画像收益，只作为路线房间完成事件上报。

**What was intentionally NOT touched:** 未修改后端事件语义，未改 `GameRoomModal` 的旧宝箱分支，未处理商店、休息点、战斗/诊断事件上报。

---

## 2026-06-23 (第二次提交)

**Goal:** 从 token_reduce 参考项目引入 SDD 方法论，建立规格文档与实施计划的写作规范体系。

**What was changed:**
- `specs/README.md` — 重写增强，融入 EARS 需求句式（5种模式）、唯一编号规则（R<组>.<序>）、诚实纪律、「一句话定位+阅读前置」顶部规范、门控检查点、新增spec清单、README路线表
- `specs/common/authoring-conventions.md` — 新文件。规格文档写作规范：通用排版、EARS句式详解（含示例）、需求文档六小节格式、design.md写法、tasks.md写法、诚实纪律、文件命名、新增spec检查清单
- `docs/plans/plan-writing-guide.md` — 新文件。实施计划写作规范：spec→plan→实现三联、门控生命周期、5条核心原则（依赖图自底向上/垂直切片/原子任务/验收+验证/检查点）、任务定级表（XS~XL）、强制任务格式、反模式
- `docs/plans/_TEMPLATE.md` — 新文件。计划模板：Context/Goals/Non-Goals/架构决策/备选方案/假设/任务清单分阶段/风险/Open Questions/越权红线/DoD。复制即用
- `docs/README.md` — 更新目录结构，新增 `plans/` 目录说明与写作规范链接

**Current state:** 项目现已具备完整的 SDD 方法论体系：
- `specs/common/authoring-conventions.md` — 告诉每个人"规格文档怎么写"
- `docs/plans/plan-writing-guide.md` + `_TEMPLATE.md` — 告诉每个人"实施计划怎么写"
- `docs/architecture/模块接口与协作规范.md` — 告诉每个人"模块间怎么配合"
三份文档构成"写需求→写计划→写接口"的完整方法论链。

**Next step:** 各组成员按模板为各自模块编写 `.plan.md`，启动 Phase 1 开发。

**Known risks / blockers:** 无。

---

## 2026-06-23 (第一次提交)

**Goal:** 基于模块化分工原则，编写模块接口定义与协作规范，为5人并行开发建立接口契约。

**What was changed:**
- `docs/architecture/模块接口与协作规范.md` — 新文档。定义了5个业务模块 + agentic 公共能力层的完整接口契约：
  - 每个模块的核心实体、对外API清单、模块间数据查询方法、上下游依赖关系
  - 共享标识符（ID）规范与数据库表命名约定
  - 模块间数据流向图
  - 4个开发阶段（地基→核心业务→AI智能→集成联调）及每阶段的产出与依赖
  - 协作规范：包结构约定、Mock策略、Git分支策略、接口变更流程
  - 每人启动检查清单

**Current state:** 模块划分已定稿（方案B经过Codex修正）。5个模块接口边界清晰，可直接按文档分工开发。技术栈（后端语言/框架）尚未最终确定，但接口定义与技术无关。

**Next step:** 
1. 确定后端语言/框架（Python FastAPI / Java Spring Boot 等）
2. 搭建金仓数据库，每人开始建表
3. Phase 1 启动：每人完成核心实体 CRUD

**Known risks / blockers:** 技术栈选型需尽快确定，否则脚手架代码风格不一致。

---

## [e2e4327] 2026-06-22

**Goal:** 更新详细设计说明书目录结构和项目需求文档，完善需求规格。

**What was changed:**
- `specs/毛瑞琪/详细设计说明书.md` — 新增系统总体设计、功能设计、工程文件组织设计架构章节；补充数据库设计章节（第6章）；交互原型设计从第3章调整为第7章
- `specs/项目需求.md` — 标题细化为"AI智慧课程（选定python编程与数据分析这一门课作为对象）"；新增需求项：学习风险预警与干预建议、班级共性问题聚类、学生激励；补充课件PPT智能讲解说明

**Current state:** 详细设计说明书骨架已扩展，项目需求从4项扩展到7项，更贴合实际教学场景。分支 `docs/update-specs` 已推送至 origin。

**Next step:** 如需合并到 master，可创建 PR 或直接 merge；继续填充详细设计说明书中的空白内容。

**Known risks / blockers:** 无。

**What was intentionally NOT touched:** 未修改代码目录，仅涉及 specs/ 下的需求与设计文档。

---

## [63b28a9] 2026-06-22

**Goal:** 完善项目文档体系 — 效仿 token_reduce 项目风格重写 README，为 specs/ 和 docs/ 建立独立 README 说明各自职责边界。

**What was changed:**
- 重写 `README.md` — 新增架构图（ASCII）、完整项目结构、开发流程（specs → 代码 → docs）、技术栈、核心功能、快速启动
- 新增 `specs/README.md` — 规格文档体系说明，明确 specs/ = 待做与需求，含写作规范与门控规则
- 新增 `docs/README.md` — 实现文档说明，明确 docs/ = 确定后的实现，区分与 specs/ 的边界
- 追踪 `specs/毛瑞琪/创新点-数据库.md` 和 `specs/毛瑞琪/创新点-鸿蒙应用.md`

**Current state:** 三级 README（根 / specs / docs）全部到位，项目结构与开发流程一目了然，新人可按 CLAUDE.md → specs/README.md → ITERATION_LOG.md 顺序快速上手。

**Next step:** 合并到 master 并推送。

**Known risks / blockers:** 无。

**What was intentionally NOT touched:** 未修改任何代码目录（frontend/backend/agentic 仍为空骨架）。

---

## [7d76c11] 2026-06-22

**Goal:** 将之前未追踪的参考文档和规格说明书纳入版本管理，整理文档目录结构。

**What was changed:**
- 新增 `references/ITERATION_LOG.md` — 迭代日志文件
- 新增 `specs/毛瑞琪/详细设计说明书.md` — 详细设计说明书
- 将 `docs/鸿蒙PC端应用开发总结.md` 移动到 `specs/毛瑞琪/` 目录下 — 归类到个人规格目录

**Current state:** 所有文档已追踪，目录结构更加清晰（references 放参考/日志，specs 放规格说明书）。

**Next step:** 合并到 master 分支并推送到远程。

**Known risks / blockers:** 无。

**What was intentionally NOT touched:** 未修改任何代码文件，仅处理文档追踪和目录整理。

---

## [3800a86] 2026-06-14

**Goal:** 初始化项目仓库，创建一级目录结构，同步到 GitHub。

**What was changed:**
- 创建 `README.md` — 项目概述与目录说明
- 创建 `CLAUDE.md` — Claude 项目上下文
- 创建 `.gitignore` — 忽略 node_modules、.env、IDE 配置等
- 创建一级目录：`docs/`、`frontend/`、`backend/`、`agentic/`、`resource/`
- 将原有 `项目需求.md` 纳入版本管理

**Current state:** 项目骨架已建立，已推送到 `https://github.com/maoruiqi-hub/Practical-training.git` (master)。

**Next step:** 细化需求分析，确定技术栈选型（前端框架、后端语言、Agentic方案），撰写技术规格文档。

**Known risks / blockers:** 无。

**What was intentionally NOT touched:** 未创建任何二级目录或代码文件，仅建立顶层结构。
