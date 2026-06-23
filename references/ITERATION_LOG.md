# Iteration Log

---

## 2026-06-23

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
