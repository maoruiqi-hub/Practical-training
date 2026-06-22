# Iteration Log

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
