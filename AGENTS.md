# AGENTS.md

## 项目概述

AI智慧课程平台（AI Smart Course Platform）— 教育管理及学习辅助平台，涵盖课程管理、知识图谱、智能评阅、学习推荐、爬塔游戏化学习等功能。

## 技术栈

- **前端**：Vue 3 + Element Plus（教师端 + 学生端）
- **后端**：Spring Boot 3.5 + MyBatis-Plus（RESTful API）
- **HarmonyOS**：ArkTS + ArkUI（爬塔游戏化学习 App）
- **AI**：DeepSeek / Dify（LLM 驱动的知识图谱构建、智能评分、个性化推荐）
- **数据库**：PostgreSQL / KingbaseES V9

## 目录结构

- `specs/` - 规格文档（需求分析与实施计划，SDD 唯一事实源）
- `docs/` - 设计文档（架构设计、接口契约、部署方案）
- `frontend/` - Vue 3 前端
- `backend/` - Spring Boot 后端
- `harmonyOS/` - HarmonyOS 爬塔游戏 App
- `agentic/` - AI Agent 服务（预留）
- `resource/` - 教学资源与示例课程
- `scripts/` - 数据种子脚本与工具
- `references/` - 参考文档与迭代日志

## 许可证

- 源代码：AGPL-3.0-only
- 文档：CC BY-NC-SA 4.0
