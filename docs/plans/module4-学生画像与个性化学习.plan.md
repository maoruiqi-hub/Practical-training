# 模块4：学生画像与个性化学习 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> 一句话定位：将 `specs/张文慧/模块4-学生画像与个性化学习-需求规格.md` 的 36 条 EARS 需求拆为可验证、按依赖排序的任务清单，在现有 Spring Boot + Vue 3 代码基础上增量实现。
> 阅读前置：[../../specs/张文慧/模块4-学生画像与个性化学习-需求规格.md](../../specs/张文慧/模块4-学生画像与个性化学习-需求规格.md)、[../architecture/模块接口与协作规范.md](../architecture/模块接口与协作规范.md)
> 状态：草拟

**Goal:** 实现学生画像与个性化学习模块的全部后端 API 和前端页面，包括画像生成、能力评分、个性化推荐、成长激励四大能力。

**Architecture:** 在现有 `com.neu.CoursePlatform` 下新建 `profile` 子包（entity/controller/service/mapper/mock/rule），复用现有 Student 表，新增 4 张表。Mock 层对齐模块间接口契约。前端新增 `/profile` 路由和 StudentProfile.vue 页面，用 Tab 组织四大功能区。

**Tech Stack:** Spring Boot 3.5.15, MyBatis-Plus 3.5.15, MySQL (TiDB Cloud), Vue 3, Element Plus, ECharts 6.x

---

## Context（背景）

当前系统已有 Student CRUD 和成绩统计，但缺少对学生学习状态的持续理解。本模块将任务提交、测验成绩等已有数据转化为画像、评分、推荐和激励，同时通过 Mock 层为尚未实现的模块1/模块2/agentic 提供可替换的占位。

## Goals（做什么）

1. 新增 4 张数据库表 + 对应实体/Mapper/Service/Controller
2. 实现画像 HP/ATK/DEF/EXP 计算逻辑
3. 实现能力评分体系和雷达图数据接口
4. 实现规则驱动的个性化推荐
5. 实现成长值+7徽章+5等级+排行榜
6. 前端学生画像中心页面（4个Tab）
7. Mock 层（知识点/学习日志/agentic）对齐接口契约

## Non-Goals（明确不做）

- 不实现真实 LLM 调用
- 不新建模块1知识点表或模块2日志采集端点
- 不修改其他模块现有表和 API
- 不实现教师端学情分析（模块5范围）

## 回引规格

- 需求文档：`specs/张文慧/模块4-学生画像与个性化学习-需求规格.md`
- 模块接口：`docs/architecture/模块接口与协作规范.md` 第七节
- 覆盖需求编号：R1.1~R7.4（共36条）

## 架构决策

- **包结构**：新建 `com.neu.CoursePlatform.profile` 包，与现有 controller/service 平级 —— 画像逻辑集中管理，不散落
- **主键策略**：沿用项目现有的自增 INT 主键（`IdType.AUTO`）—— 与7张已有表保持一致，不引入 UUID
- **Mock 实现方式**：Mock 类放在 `profile.mock` 包，通过 `@Primary` 或条件注入切换 —— 真实模块就绪后替换不修改业务代码

## 假设

1. TiDB Cloud 数据库可正常连接（已验证）
2. 现有 Student 表数据可复用，画像表通过 student_no 关联
3. 前端开发环境（Node 25 + npm 11）正常运行
4. 模块1/2/agentic 的接口契约以 `docs/architecture/模块接口与协作规范.md` 为准

---

## 文件结构一览

### 后端新增

```
backend/src/main/java/com/neu/CoursePlatform/profile/
├── entity/
│   ├── StudentProfile.java       — 学习画像实体
│   ├── CompetencyScore.java      — 能力评分实体
│   ├── Recommendation.java       — 推荐记录实体
│   └── Achievement.java          — 成就/成长值/徽章实体
├── controller/
│   └── ProfileController.java    — 画像/推荐/成就 REST 接口
├── service/
│   ├── ProfileService.java       — 画像生成接口
│   ├── RecommendationService.java — 推荐生成接口
│   └── IncentiveService.java     — 成长激励接口
├── service/impl/
│   ├── ProfileServiceImpl.java
│   ├── RecommendationServiceImpl.java
│   └── IncentiveServiceImpl.java
├── mapper/
│   ├── StudentProfileMapper.java
│   ├── CompetencyScoreMapper.java
│   ├── RecommendationMapper.java
│   └── AchievementMapper.java
├── mock/
│   ├── MockKnowledgePointService.java  — Mock模块1知识点查询
│   ├── MockLearningLogService.java     — Mock模块2学习日志查询
│   └── MockAgenticClient.java          — Mock agentic LLM调用
└── rule/
    ├── GrowthRuleEngine.java    — 成长值计算规则
    ├── BadgeRuleEngine.java     — 徽章触发规则
    └── TierTitleEngine.java     — 阶段称号判定
```

### 后端修改

```
backend/src/main/resources/schema.sql                    — 新增4张表
backend/src/main/java/com/neu/CoursePlatform/controller/
    StudentController.java                               — 新增导入导出端点
backend/src/main/java/com/neu/CoursePlatform/service/
    StudentService.java                                  — 新增导入导出方法签名
backend/src/main/java/com/neu/CoursePlatform/service/impl/
    StudentServiceImpl.java                              — 实现导入导出
backend/src/main/java/com/neu/CoursePlatform/mapper/
    StudentMapper.java                                   — 新增批量插入方法
```

### 前端新增

```
frontend/src/views/StudentProfile.vue   — 学生画像中心页面（4个Tab）
frontend/src/api/profile.js             — 画像模块 API 封装
```

### 前端修改

```
frontend/src/router/index.js            — 新增 /profile 路由
frontend/src/views/MainLayout.vue       — 导航栏增加"我的画像"入口
frontend/src/views/StudentManage.vue    — 增加批量导入导出按钮
```

---

## 任务清单

### 阶段1：数据层地基

- [ ] **T1 — 创建4张新表**（满足 R2.1, R4.1, R5.1, R6.1）
  - 验收：4 张表在 TiDB Cloud 上创建成功，`SHOW TABLES` 可见
  - 验证：启动后端，检查 schema.sql 自动执行无报错
  - 依赖：无
  - 文件：`backend/src/main/resources/schema.sql`
  - 规模：S

  在 `schema.sql` 末尾追加：

  ```sql
  -- 学生画像表
  CREATE TABLE IF NOT EXISTS student_profile (
      id INT AUTO_INCREMENT PRIMARY KEY,
      student_no INT,
      course_code INT,
      hp INT DEFAULT 100,
      atk INT DEFAULT 50,
      def INT DEFAULT 50,
      exp INT DEFAULT 0,
      level INT DEFAULT 1,
      coins INT DEFAULT 0,
      energy INT DEFAULT 5,
      status VARCHAR(32) DEFAULT '正常学习',
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  -- 能力评分表
  CREATE TABLE IF NOT EXISTS competency_score (
      id INT AUTO_INCREMENT PRIMARY KEY,
      student_no INT,
      course_code INT,
      ability_point_id VARCHAR(64),
      ability_point_name VARCHAR(128),
      score INT DEFAULT 50,
      last_updated DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  -- 推荐记录表
  CREATE TABLE IF NOT EXISTS recommendation (
      id INT AUTO_INCREMENT PRIMARY KEY,
      student_no INT,
      course_code INT,
      type VARCHAR(32),
      target_id VARCHAR(64),
      target_name VARCHAR(256),
      reason TEXT,
      priority INT DEFAULT 0,
      feedback VARCHAR(16),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  -- 成就记录表
  CREATE TABLE IF NOT EXISTS achievement (
      id INT AUTO_INCREMENT PRIMARY KEY,
      student_no INT,
      course_code INT,
      achievement_type VARCHAR(32),
      name VARCHAR(128),
      description VARCHAR(512),
      earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      metadata TEXT
  );
  ```

- [ ] **T2 — 创建4个实体类**（满足 R2.1, R4.1, R5.1, R6.1）
  - 验收：实体类编译通过，字段与表结构一致
  - 验证：`mvn compile` 无错误
  - 依赖：T1
  - 文件：`profile/entity/StudentProfile.java`, `profile/entity/CompetencyScore.java`, `profile/entity/Recommendation.java`, `profile/entity/Achievement.java`
  - 规模：M

  **StudentProfile.java**:
  ```java
  package com.neu.CoursePlatform.profile.entity;

  import com.baomidou.mybatisplus.annotation.IdType;
  import com.baomidou.mybatisplus.annotation.TableId;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  @Data
  @NoArgsConstructor
  public class StudentProfile {
      @TableId(type = IdType.AUTO)
      private Integer id;
      private Integer studentNo;
      private Integer courseCode;
      private Integer hp;
      private Integer atk;
      private Integer def;
      private Integer exp;
      private Integer level;
      private Integer coins;
      private Integer energy;
      private String status;
      private java.util.Date updatedAt;
  }
  ```

  **CompetencyScore.java**:
  ```java
  package com.neu.CoursePlatform.profile.entity;

  import com.baomidou.mybatisplus.annotation.IdType;
  import com.baomidou.mybatisplus.annotation.TableId;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  @Data
  @NoArgsConstructor
  public class CompetencyScore {
      @TableId(type = IdType.AUTO)
      private Integer id;
      private Integer studentNo;
      private Integer courseCode;
      private String abilityPointId;
      private String abilityPointName;
      private Integer score;
      private java.util.Date lastUpdated;
  }
  ```

  **Recommendation.java**:
  ```java
  package com.neu.CoursePlatform.profile.entity;

  import com.baomidou.mybatisplus.annotation.IdType;
  import com.baomidou.mybatisplus.annotation.TableId;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  @Data
  @NoArgsConstructor
  public class Recommendation {
      @TableId(type = IdType.AUTO)
      private Integer id;
      private Integer studentNo;
      private Integer courseCode;
      private String type;
      private String targetId;
      private String targetName;
      private String reason;
      private Integer priority;
      private String feedback;
      private java.util.Date createdAt;
  }
  ```

  **Achievement.java**:
  ```java
  package com.neu.CoursePlatform.profile.entity;

  import com.baomidou.mybatisplus.annotation.IdType;
  import com.baomidou.mybatisplus.annotation.TableId;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  @Data
  @NoArgsConstructor
  public class Achievement {
      @TableId(type = IdType.AUTO)
      private Integer id;
      private Integer studentNo;
      private Integer courseCode;
      private String achievementType;
      private String name;
      private String description;
      private java.util.Date earnedAt;
      private String metadata;
  }
  ```

- [ ] **T3 — 创建4个 Mapper 接口**（满足 R2.1, R4.1, R5.1, R6.1）
  - 验收：Mapper 继承 BaseMapper，编译通过
  - 验证：`mvn compile` 无错误
  - 依赖：T2
  - 文件：`profile/mapper/StudentProfileMapper.java`, `profile/mapper/CompetencyScoreMapper.java`, `profile/mapper/RecommendationMapper.java`, `profile/mapper/AchievementMapper.java`
  - 规模：S

  ```java
  // StudentProfileMapper.java
  package com.neu.CoursePlatform.profile.mapper;

  import com.baomidou.mybatisplus.core.mapper.BaseMapper;
  import com.neu.CoursePlatform.profile.entity.StudentProfile;
  import org.apache.ibatis.annotations.Mapper;

  @Mapper
  public interface StudentProfileMapper extends BaseMapper<StudentProfile> {
  }

  // CompetencyScoreMapper.java
  package com.neu.CoursePlatform.profile.mapper;

  import com.baomidou.mybatisplus.core.mapper.BaseMapper;
  import com.neu.CoursePlatform.profile.entity.CompetencyScore;
  import org.apache.ibatis.annotations.Mapper;

  @Mapper
  public interface CompetencyScoreMapper extends BaseMapper<CompetencyScore> {
  }

  // RecommendationMapper.java
  package com.neu.CoursePlatform.profile.mapper;

  import com.baomidou.mybatisplus.core.mapper.BaseMapper;
  import com.neu.CoursePlatform.profile.entity.Recommendation;
  import org.apache.ibatis.annotations.Mapper;

  @Mapper
  public interface RecommendationMapper extends BaseMapper<Recommendation> {
  }

  // AchievementMapper.java
  package com.neu.CoursePlatform.profile.mapper;

  import com.baomidou.mybatisplus.core.mapper.BaseMapper;
  import com.neu.CoursePlatform.profile.entity.Achievement;
  import org.apache.ibatis.annotations.Mapper;

  @Mapper
  public interface AchievementMapper extends BaseMapper<Achievement> {
  }
  ```

### 检查点：阶段1完成

- [ ] 重启后端，4张表自动创建成功
- [ ] `mvn compile` 通过
- [ ] 人审通过再继续

---

### 阶段2：Mock层 + 规则引擎

- [ ] **T4 — 创建 MockKnowledgePointService**（满足 R4.1, R5.2, R5.3）
  - 验收：返回固定知识点和能力点列表，接口签名对齐模块1契约
  - 验证：注入后调用 `getAbilityMap(1)` 返回 ≥ 5 个能力点
  - 依赖：T3
  - 文件：`profile/mock/MockKnowledgePointService.java`
  - 规模：S

  ```java
  package com.neu.CoursePlatform.profile.mock;

  import org.springframework.stereotype.Service;
  import java.util.*;

  @Service
  public class MockKnowledgePointService {

      /** Mock 课程能力点列表（对齐模块1 getAbilityMap 契约） */
      public List<Map<String, Object>> getAbilityMap(Integer courseCode) {
          return List.of(
              Map.of("id", "AP01", "name", "基础语法", "description", "变量、数据类型、运算符"),
              Map.of("id", "AP02", "name", "控制流程", "description", "条件判断、循环结构"),
              Map.of("id", "AP03", "name", "函数定义", "description", "def、参数、返回值"),
              Map.of("id", "AP04", "name", "数据结构", "description", "列表、字典、集合、元组"),
              Map.of("id", "AP05", "name", "文件操作", "description", "文件读写、CSV/JSON处理"),
              Map.of("id", "AP06", "name", "数据分析", "description", "NumPy、Pandas数据处理"),
              Map.of("id", "AP07", "name", "可视化", "description", "Matplotlib图表绘制")
          );
      }

      /** Mock 课程知识点列表（对齐模块1 getKnowledgePointsByCourse 契约） */
      public List<Map<String, Object>> getKnowledgePointsByCourse(Integer courseCode) {
          return List.of(
              Map.of("id", "KP01", "name", "变量与类型", "level", 1),
              Map.of("id", "KP02", "name", "条件判断", "level", 1),
              Map.of("id", "KP03", "name", "循环结构", "level", 2),
              Map.of("id", "KP04", "name", "函数定义", "level", 2),
              Map.of("id", "KP05", "name", "列表与字典", "level", 3),
              Map.of("id", "KP06", "name", "文件读写", "level", 3),
              Map.of("id", "KP07", "name", "NumPy数组", "level", 4),
              Map.of("id", "KP08", "name", "Pandas DataFrame", "level", 4),
              Map.of("id", "KP09", "name", "数据清洗", "level", 5),
              Map.of("id", "KP10", "name", "分组聚合", "level", 5),
              Map.of("id", "KP11", "name", "Matplotlib绘图", "level", 5),
              Map.of("id", "KP12", "name", "统计分析", "level", 6),
              Map.of("id", "KP13", "name", "综合项目", "level", 6)
          );
      }
  }
  ```

- [ ] **T5 — 创建 MockLearningLogService**（满足 R2.6, R3.3）
  - 验收：返回模拟学习日志，接口签名对齐模块2契约
  - 验证：注入后调用返回非空 List
  - 依赖：T3
  - 文件：`profile/mock/MockLearningLogService.java`
  - 规模：S

  ```java
  package com.neu.CoursePlatform.profile.mock;

  import org.springframework.stereotype.Service;
  import java.util.*;

  @Service
  public class MockLearningLogService {

      /** Mock 学生行为日志（对齐模块2 getStudentLogs 契约） */
      public List<Map<String, Object>> getStudentLogs(Integer studentNo, Integer courseCode) {
          return List.of(
              Map.of("actionType", "answer", "resourceType", "quiz",
                     "durationMs", 300000, "timestamp", new Date(),
                     "detail", "完成选择题练习"),
              Map.of("actionType", "answer", "resourceType", "quiz",
                     "durationMs", 180000, "timestamp", new Date(),
                     "detail", "完成填空题练习"),
              Map.of("actionType", "video_play", "resourceType", "video",
                     "durationMs", 600000, "timestamp", new Date(),
                     "detail", "观看Python基础视频"),
              Map.of("actionType", "code_submit", "resourceType", "program",
                     "durationMs", 900000, "timestamp", new Date(),
                     "detail", "提交编程作业")
          );
      }
  }
  ```

- [ ] **T6 — 创建 MockAgenticClient**（满足 R5.4, R6.3）
  - 验收：模拟 LLM 返回结构，接口签名对齐 agentic 契约
  - 验证：调用 `generateRecommendReason()` 返回非空字符串
  - 依赖：无
  - 文件：`profile/mock/MockAgenticClient.java`
  - 规模：S

  ```java
  package com.neu.CoursePlatform.profile.mock;

  import org.springframework.stereotype.Service;
  import java.util.*;

  @Service
  public class MockAgenticClient {

      /** Mock 推荐理由生成（对齐 agentic /recommend 契约） */
      public String generateRecommendReason(String targetName, String type, int score) {
          Map<String, String> reasons = Map.of(
              "knowledge_point", "根据你最近的学习表现，" + targetName + "是需要重点巩固的内容",
              "review_material", targetName + "的正确率低于60%，建议复习相关基础内容",
              "extended_material", targetName + "掌握得很好，可以挑战进阶内容了",
              "practice", targetName + "建议通过专项练习巩固薄弱环节"
          );
          return reasons.getOrDefault(type, "系统为你推荐: " + targetName);
      }

      /** Mock 学习反馈生成（对齐 agentic /chat 契约） */
      public String generateFeedback(Map<String, Object> profileData) {
          int hp = (int) profileData.getOrDefault("hp", 80);
          if (hp < 40) return "你的信心值偏低，建议先休息或复习基础内容再继续挑战。";
          if (hp < 70) return "状态不错，继续保持！薄弱知识点建议优先复习。";
          return "表现优秀！可以考虑挑战进阶内容或拓展材料。";
      }
  }
  ```

- [ ] **T7 — 创建3个规则引擎**（满足 R6.2, R6.3, R6.5, R3.3）
  - 验收：三个引擎独立可测，输入答题数据，输出正确的等级/徽章/称号
  - 验证：单元逻辑可手工推演验证
  - 依赖：无
  - 文件：`profile/rule/GrowthRuleEngine.java`, `profile/rule/BadgeRuleEngine.java`, `profile/rule/TierTitleEngine.java`
  - 规模：M

  **GrowthRuleEngine.java**:
  ```java
  package com.neu.CoursePlatform.profile.rule;

  import org.springframework.stereotype.Component;

  @Component
  public class GrowthRuleEngine {

      public int calcExpGain(String taskType, boolean correct) {
          if (correct) {
              return switch (taskType) {
                  case "quiz" -> 40;
                  case "boss" -> 150;
                  default -> 10;
              };
          }
          return 0;
      }

      public int calcCoinGain(String taskType, boolean correct) {
          if (correct) {
              return switch (taskType) {
                  case "quiz" -> 80;
                  case "boss" -> 300;
                  default -> 20;
              };
          }
          return 0;
      }

      public int calcLevel(int exp) {
          if (exp >= 2000) return 5;  // 精通
          if (exp >= 1000) return 4;  // 熟练
          if (exp >= 500) return 3;   // 中级
          if (exp >= 200) return 2;   // 初级
          return 1;                    // 入门
      }

      public String getLevelName(int level) {
          return switch (level) {
              case 5 -> "精通";
              case 4 -> "熟练";
              case 3 -> "中级";
              case 2 -> "初级";
              default -> "入门";
          };
      }
  }
  ```

  **BadgeRuleEngine.java**:
  ```java
  package com.neu.CoursePlatform.profile.rule;

  import org.springframework.stereotype.Component;
  import java.util.*;

  @Component
  public class BadgeRuleEngine {

      public record BadgeCheck(boolean earned, String name, String description) {}

      public List<BadgeCheck> checkAll(int totalCorrect, int consecutiveCorrect,
                                        boolean timedComplete, boolean fullScore,
                                        int nightSessions, int helpfulFeedback,
                                        int selfCorrections, List<String> existingBadges) {
          List<BadgeCheck> results = new ArrayList<>();
          String earned = String.join(",", existingBadges);

          if (!earned.contains("连击王") && consecutiveCorrect >= 10)
              results.add(new BadgeCheck(true, "连击王", "连续答对10题"));
          if (!earned.contains("完美主义") && fullScore)
              results.add(new BadgeCheck(true, "完美主义", "单次测验满分"));
          if (!earned.contains("速通者") && timedComplete)
              results.add(new BadgeCheck(true, "速通者", "限时内完成测验"));
          if (!earned.contains("Pythonic") && totalCorrect >= 20)
              results.add(new BadgeCheck(true, "Pythonic", "正确答题超过20题"));
          if (!earned.contains("Debug之眼") && selfCorrections >= 5)
              results.add(new BadgeCheck(true, "Debug之眼", "自行修正错误5次"));
          if (!earned.contains("夜枭") && nightSessions >= 5)
              results.add(new BadgeCheck(true, "夜枭", "非上课时段完成5题"));
          if (!earned.contains("助人者") && helpfulFeedback >= 3)
              results.add(new BadgeCheck(true, "助人者", "有价值的提问3次"));

          return results;
      }
  }
  ```

  **TierTitleEngine.java**:
  ```java
  package com.neu.CoursePlatform.profile.rule;

  import org.springframework.stereotype.Component;

  @Component
  public class TierTitleEngine {

      public String getTitle(int level, int consecutiveCorrect, int badgeCount) {
          if (level >= 4 && badgeCount >= 5) return "塔之征服者";
          if (level >= 3 && badgeCount >= 3) return "知识探险家";
          if (level >= 2 && badgeCount >= 1) return "编程学徒";
          if (consecutiveCorrect >= 5) return "连击新星";
          return "初入塔境";
      }

      public String getNextTitleHint(int level, int badgeCount) {
          if (level < 2) return "获得第1个徽章即可晋升为'编程学徒'";
          if (level < 3) return "等级达到中级并拥有3个徽章可晋升为'知识探险家'";
          if (level < 4) return "等级达到熟练并拥有5个徽章可晋升为'塔之征服者'";
          return "已是最高称号！";
      }
  }
  ```

### 检查点：阶段2完成

- [ ] `mvn compile` 通过
- [ ] Mock 类可被 Spring 扫描并注入
- [ ] 规则引擎逻辑可手工推演验证

---

### 阶段3：Service层

- [ ] **T8 — 创建 ProfileService 接口和实现**（满足 R2.1~R2.7, R3.1~R3.6, R4.2~R4.5）
  - 验收：`getOrCreateProfile(studentNo, courseCode)` 返回有效画像，属性计算正确
  - 验证：注入后调用返回非空 StudentProfile，HP/ATK/DEF/EXP 值在有效范围内
  - 依赖：T3, T4, T5, T7
  - 文件：`profile/service/ProfileService.java`, `profile/service/impl/ProfileServiceImpl.java`
  - 规模：M

  **ProfileService.java**:
  ```java
  package com.neu.CoursePlatform.profile.service;

  import com.neu.CoursePlatform.profile.entity.CompetencyScore;
  import com.neu.CoursePlatform.profile.entity.StudentProfile;
  import java.util.*;

  public interface ProfileService {
      StudentProfile getOrCreateProfile(Integer studentNo, Integer courseCode);
      void updateProfileFromSubmission(Integer studentNo, Integer courseCode,
                                        boolean correct, String taskType);
      List<CompetencyScore> getCompetencyScores(Integer studentNo, Integer courseCode);
      void updateCompetencyScores(Integer studentNo, Integer courseCode,
                                   String abilityPointId, boolean correct);
      Map<String, Object> getProfileSummary(Integer studentNo, Integer courseCode);
  }
  ```

  **ProfileServiceImpl.java**:
  ```java
  package com.neu.CoursePlatform.profile.service.impl;

  import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
  import com.neu.CoursePlatform.profile.entity.*;
  import com.neu.CoursePlatform.profile.mapper.*;
  import com.neu.CoursePlatform.profile.mock.*;
  import com.neu.CoursePlatform.profile.rule.GrowthRuleEngine;
  import com.neu.CoursePlatform.profile.service.ProfileService;
  import org.springframework.stereotype.Service;
  import java.util.*;
  import java.util.stream.Collectors;

  @Service
  public class ProfileServiceImpl implements ProfileService {

      private final StudentProfileMapper profileMapper;
      private final CompetencyScoreMapper competencyMapper;
      private final MockKnowledgePointService mockKP;
      private final GrowthRuleEngine growthEngine;

      public ProfileServiceImpl(StudentProfileMapper profileMapper,
                                 CompetencyScoreMapper competencyMapper,
                                 MockKnowledgePointService mockKP,
                                 GrowthRuleEngine growthEngine) {
          this.profileMapper = profileMapper;
          this.competencyMapper = competencyMapper;
          this.mockKP = mockKP;
          this.growthEngine = growthEngine;
      }

      @Override
      public StudentProfile getOrCreateProfile(Integer studentNo, Integer courseCode) {
          LambdaQueryWrapper<StudentProfile> q = new LambdaQueryWrapper<>();
          q.eq(StudentProfile::getStudentNo, studentNo)
           .eq(StudentProfile::getCourseCode, courseCode);
          StudentProfile profile = profileMapper.selectOne(q);
          if (profile != null) return profile;

          profile = new StudentProfile();
          profile.setStudentNo(studentNo);
          profile.setCourseCode(courseCode);
          profile.setHp(100);
          profile.setAtk(50);
          profile.setDef(50);
          profile.setExp(0);
          profile.setLevel(1);
          profile.setCoins(0);
          profile.setEnergy(5);
          profile.setStatus("正常学习");
          profileMapper.insert(profile);

          // 初始化能力评分
          List<Map<String, Object>> abilityMap = mockKP.getAbilityMap(courseCode);
          for (Map<String, Object> ap : abilityMap) {
              CompetencyScore cs = new CompetencyScore();
              cs.setStudentNo(studentNo);
              cs.setCourseCode(courseCode);
              cs.setAbilityPointId((String) ap.get("id"));
              cs.setAbilityPointName((String) ap.get("name"));
              cs.setScore(50);
              competencyMapper.insert(cs);
          }

          return profile;
      }

      @Override
      public void updateProfileFromSubmission(Integer studentNo, Integer courseCode,
                                               boolean correct, String taskType) {
          StudentProfile profile = getOrCreateProfile(studentNo, courseCode);

          int expGain = growthEngine.calcExpGain(taskType, correct);
          int coinGain = growthEngine.calcCoinGain(taskType, correct);

          if (correct) {
              profile.setHp(Math.min(100, profile.getHp() + 5));
              profile.setAtk(Math.min(100, profile.getAtk() + 2));
          } else {
              profile.setHp(Math.max(0, profile.getHp() - 10));
              profile.setAtk(Math.max(0, profile.getAtk() - 1));
          }

          profile.setExp(profile.getExp() + expGain);
          profile.setCoins(profile.getCoins() + coinGain);
          profile.setLevel(growthEngine.calcLevel(profile.getExp()));
          profile.setUpdatedAt(new Date());
          profileMapper.updateById(profile);
      }

      @Override
      public List<CompetencyScore> getCompetencyScores(Integer studentNo, Integer courseCode) {
          getOrCreateProfile(studentNo, courseCode);
          LambdaQueryWrapper<CompetencyScore> q = new LambdaQueryWrapper<>();
          q.eq(CompetencyScore::getStudentNo, studentNo)
           .eq(CompetencyScore::getCourseCode, courseCode);
          return competencyMapper.selectList(q);
      }

      @Override
      public void updateCompetencyScores(Integer studentNo, Integer courseCode,
                                          String abilityPointId, boolean correct) {
          LambdaQueryWrapper<CompetencyScore> q = new LambdaQueryWrapper<>();
          q.eq(CompetencyScore::getStudentNo, studentNo)
           .eq(CompetencyScore::getCourseCode, courseCode)
           .eq(CompetencyScore::getAbilityPointId, abilityPointId);
          CompetencyScore cs = competencyMapper.selectOne(q);
          if (cs != null) {
              cs.setScore(correct
                  ? Math.min(100, cs.getScore() + 2)
                  : Math.max(0, cs.getScore() - 1));
              cs.setLastUpdated(new Date());
              competencyMapper.updateById(cs);
          }
      }

      @Override
      public Map<String, Object> getProfileSummary(Integer studentNo, Integer courseCode) {
          StudentProfile profile = getOrCreateProfile(studentNo, courseCode);
          List<CompetencyScore> scores = getCompetencyScores(studentNo, courseCode);
          Map<String, Object> summary = new LinkedHashMap<>();
          summary.put("profile", profile);
          summary.put("competencyScores", scores);
          summary.put("abilityMap", mockKP.getAbilityMap(courseCode));
          return summary;
      }
  }
  ```

- [ ] **T9 — 创建 RecommendationService 接口和实现**（满足 R5.1~R5.6）
  - 验收：`generateRecommendations()` 返回 ≥ 3 条推荐，各有优先级和理由
  - 验证：注入后调用返回非空 List，每条有 type/targetName/reason/priority 字段
  - 依赖：T3, T4, T6, T8
  - 文件：`profile/service/RecommendationService.java`, `profile/service/impl/RecommendationServiceImpl.java`
  - 规模：M

  **RecommendationService.java**:
  ```java
  package com.neu.CoursePlatform.profile.service;

  import com.neu.CoursePlatform.profile.entity.Recommendation;
  import java.util.List;

  public interface RecommendationService {
      List<Recommendation> generateRecommendations(Integer studentNo, Integer courseCode);
      List<Recommendation> getRecommendations(Integer studentNo, Integer courseCode);
      void recordFeedback(Integer recommendationId, String feedback);
  }
  ```

  **RecommendationServiceImpl.java**:
  ```java
  package com.neu.CoursePlatform.profile.service.impl;

  import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
  import com.neu.CoursePlatform.profile.entity.*;
  import com.neu.CoursePlatform.profile.mapper.RecommendationMapper;
  import com.neu.CoursePlatform.profile.mock.MockAgenticClient;
  import com.neu.CoursePlatform.profile.mock.MockKnowledgePointService;
  import com.neu.CoursePlatform.profile.service.ProfileService;
  import com.neu.CoursePlatform.profile.service.RecommendationService;
  import org.springframework.stereotype.Service;
  import java.util.*;

  @Service
  public class RecommendationServiceImpl implements RecommendationService {

      private final RecommendationMapper recommendationMapper;
      private final ProfileService profileService;
      private final MockKnowledgePointService mockKP;
      private final MockAgenticClient mockAgentic;

      public RecommendationServiceImpl(RecommendationMapper recommendationMapper,
                                        ProfileService profileService,
                                        MockKnowledgePointService mockKP,
                                        MockAgenticClient mockAgentic) {
          this.recommendationMapper = recommendationMapper;
          this.profileService = profileService;
          this.mockKP = mockKP;
          this.mockAgentic = mockAgentic;
      }

      @Override
      public List<Recommendation> generateRecommendations(Integer studentNo, Integer courseCode) {
          // 清除旧推荐
          LambdaQueryWrapper<Recommendation> delQ = new LambdaQueryWrapper<>();
          delQ.eq(Recommendation::getStudentNo, studentNo)
              .eq(Recommendation::getCourseCode, courseCode);
          recommendationMapper.delete(delQ);

          List<CompetencyScore> scores = profileService.getCompetencyScores(studentNo, courseCode);
          List<Recommendation> newRecs = new ArrayList<>();

          for (CompetencyScore cs : scores) {
              Recommendation rec = new Recommendation();
              rec.setStudentNo(studentNo);
              rec.setCourseCode(courseCode);
              rec.setTargetId(cs.getAbilityPointId());
              rec.setTargetName(cs.getAbilityPointName());

              if (cs.getScore() < 40) {
                  rec.setType("review_material");
                  rec.setPriority(1);
                  rec.setReason(mockAgentic.generateRecommendReason(cs.getAbilityPointName(), "review_material", cs.getScore()));
              } else if (cs.getScore() < 60) {
                  rec.setType("practice");
                  rec.setPriority(2);
                  rec.setReason(mockAgentic.generateRecommendReason(cs.getAbilityPointName(), "practice", cs.getScore()));
              } else if (cs.getScore() >= 80) {
                  rec.setType("extended_material");
                  rec.setPriority(3);
                  rec.setReason(mockAgentic.generateRecommendReason(cs.getAbilityPointName(), "extended_material", cs.getScore()));
              } else {
                  rec.setType("knowledge_point");
                  rec.setPriority(2);
                  rec.setReason(mockAgentic.generateRecommendReason(cs.getAbilityPointName(), "knowledge_point", cs.getScore()));
              }

              rec.setCreatedAt(new Date());
              recommendationMapper.insert(rec);
              newRecs.add(rec);
          }

          return newRecs;
      }

      @Override
      public List<Recommendation> getRecommendations(Integer studentNo, Integer courseCode) {
          LambdaQueryWrapper<Recommendation> q = new LambdaQueryWrapper<>();
          q.eq(Recommendation::getStudentNo, studentNo)
           .eq(Recommendation::getCourseCode, courseCode)
           .orderByAsc(Recommendation::getPriority);
          return recommendationMapper.selectList(q);
      }

      @Override
      public void recordFeedback(Integer recommendationId, String feedback) {
          Recommendation rec = recommendationMapper.selectById(recommendationId);
          if (rec != null) {
              rec.setFeedback(feedback);
              recommendationMapper.updateById(rec);
          }
      }
  }
  ```

- [ ] **T10 — 创建 IncentiveService 接口和实现**（满足 R6.1~R6.6, R7.3）
  - 验收：`checkAndAwardBadges()` 返回新获得的徽章列表，`getLeaderboard()` 返回排序列表
  - 验证：注入后调用返回正确数据结构
  - 依赖：T3, T7, T8
  - 文件：`profile/service/IncentiveService.java`, `profile/service/impl/IncentiveServiceImpl.java`
  - 规模：M

  **IncentiveService.java**:
  ```java
  package com.neu.CoursePlatform.profile.service;

  import com.neu.CoursePlatform.profile.entity.Achievement;
  import java.util.*;

  public interface IncentiveService {
      List<Achievement> checkAndAwardBadges(Integer studentNo, Integer courseCode,
              int totalCorrect, int consecutiveCorrect, boolean timedComplete,
              boolean fullScore, int nightSessions, int helpfulFeedback, int selfCorrections);
      List<Achievement> getAchievements(Integer studentNo, Integer courseCode);
      List<Map<String, Object>> getLeaderboard(Integer courseCode, String type);
      String getTitle(Integer studentNo, Integer courseCode);
  }
  ```

  **IncentiveServiceImpl.java**:
  ```java
  package com.neu.CoursePlatform.profile.service.impl;

  import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
  import com.neu.CoursePlatform.profile.entity.*;
  import com.neu.CoursePlatform.profile.mapper.AchievementMapper;
  import com.neu.CoursePlatform.profile.mapper.StudentProfileMapper;
  import com.neu.CoursePlatform.profile.rule.BadgeRuleEngine;
  import com.neu.CoursePlatform.profile.rule.TierTitleEngine;
  import com.neu.CoursePlatform.profile.service.IncentiveService;
  import com.neu.CoursePlatform.profile.service.ProfileService;
  import org.springframework.stereotype.Service;
  import java.util.*;
  import java.util.stream.Collectors;

  @Service
  public class IncentiveServiceImpl implements IncentiveService {

      private final AchievementMapper achievementMapper;
      private final StudentProfileMapper profileMapper;
      private final ProfileService profileService;
      private final BadgeRuleEngine badgeEngine;
      private final TierTitleEngine titleEngine;

      public IncentiveServiceImpl(AchievementMapper achievementMapper,
                                   StudentProfileMapper profileMapper,
                                   ProfileService profileService,
                                   BadgeRuleEngine badgeEngine,
                                   TierTitleEngine titleEngine) {
          this.achievementMapper = achievementMapper;
          this.profileMapper = profileMapper;
          this.profileService = profileService;
          this.badgeEngine = badgeEngine;
          this.titleEngine = titleEngine;
      }

      @Override
      public List<Achievement> checkAndAwardBadges(Integer studentNo, Integer courseCode,
              int totalCorrect, int consecutiveCorrect, boolean timedComplete,
              boolean fullScore, int nightSessions, int helpfulFeedback, int selfCorrections) {
          List<Achievement> existing = getAchievements(studentNo, courseCode);
          List<String> existingNames = existing.stream()
                  .filter(a -> "badge".equals(a.getAchievementType()))
                  .map(Achievement::getName)
                  .collect(Collectors.toList());

          List<BadgeRuleEngine.BadgeCheck> checks = badgeEngine.checkAll(
                  totalCorrect, consecutiveCorrect, timedComplete, fullScore,
                  nightSessions, helpfulFeedback, selfCorrections, existingNames);

          List<Achievement> newBadges = new ArrayList<>();
          for (BadgeRuleEngine.BadgeCheck check : checks) {
              if (check.earned()) {
                  Achievement a = new Achievement();
                  a.setStudentNo(studentNo);
                  a.setCourseCode(courseCode);
                  a.setAchievementType("badge");
                  a.setName(check.name());
                  a.setDescription(check.description());
                  a.setEarnedAt(new Date());
                  achievementMapper.insert(a);
                  newBadges.add(a);
              }
          }
          return newBadges;
      }

      @Override
      public List<Achievement> getAchievements(Integer studentNo, Integer courseCode) {
          LambdaQueryWrapper<Achievement> q = new LambdaQueryWrapper<>();
          q.eq(Achievement::getStudentNo, studentNo)
           .eq(Achievement::getCourseCode, courseCode)
           .orderByDesc(Achievement::getEarnedAt);
          return achievementMapper.selectList(q);
      }

      @Override
      public List<Map<String, Object>> getLeaderboard(Integer courseCode, String type) {
          LambdaQueryWrapper<StudentProfile> q = new LambdaQueryWrapper<>();
          q.eq(StudentProfile::getCourseCode, courseCode);
          if ("coins".equals(type)) {
              q.orderByDesc(StudentProfile::getCoins);
          } else if ("exp".equals(type)) {
              q.orderByDesc(StudentProfile::getExp);
          } else {
              q.orderByDesc(StudentProfile::getExp);
          }
          List<StudentProfile> profiles = profileMapper.selectList(q);

          List<Map<String, Object>> board = new ArrayList<>();
          int rank = 1;
          for (StudentProfile p : profiles) {
              Map<String, Object> entry = new LinkedHashMap<>();
              entry.put("rank", rank++);
              entry.put("studentNo", p.getStudentNo());
              entry.put("level", p.getLevel());
              entry.put("exp", p.getExp());
              entry.put("coins", p.getCoins());
              entry.put("badgeCount", getAchievements(p.getStudentNo(), courseCode).size());
              board.add(entry);
          }
          return board.stream().limit(20).collect(Collectors.toList());
      }

      @Override
      public String getTitle(Integer studentNo, Integer courseCode) {
          StudentProfile profile = profileService.getOrCreateProfile(studentNo, courseCode);
          int badgeCount = (int) getAchievements(studentNo, courseCode).stream()
                  .filter(a -> "badge".equals(a.getAchievementType())).count();
          return titleEngine.getTitle(profile.getLevel(), 0, badgeCount);
      }
  }
  ```

### 检查点：阶段3完成

- [ ] `mvn compile` 通过
- [ ] Service 注入无循环依赖
- [ ] 核心画像计算逻辑可推演验证

---

### 阶段4：Controller 层 + 学生管理增强

- [ ] **T11 — 创建 ProfileController**（满足 R2.1, R3.1, R4.5, R5.1, R6.5, R7.1）
  - 验收：8 个 REST 端点全部可访问，返回统一 Result 格式
  - 验证：启动后端，用 curl 测试每个端点返回 200
  - 依赖：T8, T9, T10
  - 文件：`profile/controller/ProfileController.java`
  - 规模：L

  ```java
  package com.neu.CoursePlatform.profile.controller;

  import com.neu.CoursePlatform.common.Auth;
  import com.neu.CoursePlatform.common.Result;
  import com.neu.CoursePlatform.entity.Student;
  import com.neu.CoursePlatform.profile.entity.*;
  import com.neu.CoursePlatform.profile.service.*;
  import jakarta.servlet.http.HttpSession;
  import org.springframework.web.bind.annotation.*;
  import java.util.*;

  @RestController
  @RequestMapping("/profile")
  public class ProfileController {

      private final ProfileService profileService;
      private final RecommendationService recommendationService;
      private final IncentiveService incentiveService;
      private final Auth auth;

      public ProfileController(ProfileService profileService,
                                RecommendationService recommendationService,
                                IncentiveService incentiveService,
                                Auth auth) {
          this.profileService = profileService;
          this.recommendationService = recommendationService;
          this.incentiveService = incentiveService;
          this.auth = auth;
      }

      /** 获取画像总览 | 学生本人 */
      @GetMapping("/{studentNo}/{courseCode}")
      public Result<Map<String, Object>> summary(@PathVariable Integer studentNo,
                                                  @PathVariable Integer courseCode,
                                                  HttpSession session) {
          Student login = (Student) session.getAttribute("student");
          if (login == null || !login.getStudentNo().equals(studentNo.toString())) {
              if (!auth.isAdmin(session)) return Result.fail("无权限");
          }
          return Result.ok(profileService.getProfileSummary(studentNo, courseCode));
      }

      /** 获取能力评分 | 学生本人 */
      @GetMapping("/{studentNo}/{courseCode}/competency")
      public Result<List<CompetencyScore>> competency(@PathVariable Integer studentNo,
                                                       @PathVariable Integer courseCode,
                                                       HttpSession session) {
          Student login = (Student) session.getAttribute("student");
          if (login == null || !login.getStudentNo().equals(studentNo.toString())) {
              if (!auth.isAdmin(session)) return Result.fail("无权限");
          }
          return Result.ok(profileService.getCompetencyScores(studentNo, courseCode));
      }

      /** 获取推荐列表 | 学生本人 */
      @GetMapping("/{studentNo}/{courseCode}/recommendations")
      public Result<List<Recommendation>> recommendations(@PathVariable Integer studentNo,
                                                           @PathVariable Integer courseCode,
                                                           HttpSession session) {
          Student login = (Student) session.getAttribute("student");
          if (login == null || !login.getStudentNo().equals(studentNo.toString())) {
              if (!auth.isAdmin(session)) return Result.fail("无权限");
          }
          List<Recommendation> recs = recommendationService.getRecommendations(studentNo, courseCode);
          if (recs.isEmpty()) recs = recommendationService.generateRecommendations(studentNo, courseCode);
          return Result.ok(recs);
      }

      /** 刷新推荐 | 学生本人 */
      @PostMapping("/{studentNo}/{courseCode}/recommendations/generate")
      public Result<List<Recommendation>> generateRecommendations(@PathVariable Integer studentNo,
                                                                    @PathVariable Integer courseCode,
                                                                    HttpSession session) {
          Student login = (Student) session.getAttribute("student");
          if (login == null || !login.getStudentNo().equals(studentNo.toString())) {
              if (!auth.isAdmin(session)) return Result.fail("无权限");
          }
          return Result.ok(recommendationService.generateRecommendations(studentNo, courseCode));
      }

      /** 推荐反馈 | 学生本人 */
      @PutMapping("/recommendations/{id}/feedback")
      public Result<Void> feedback(@PathVariable Integer id, @RequestParam String feedback,
                                    HttpSession session) {
          if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
          recommendationService.recordFeedback(id, feedback);
          return Result.ok();
      }

      /** 获取成就列表 | 学生本人 */
      @GetMapping("/{studentNo}/{courseCode}/achievements")
      public Result<List<Achievement>> achievements(@PathVariable Integer studentNo,
                                                     @PathVariable Integer courseCode,
                                                     HttpSession session) {
          Student login = (Student) session.getAttribute("student");
          if (login == null || !login.getStudentNo().equals(studentNo.toString())) {
              if (!auth.isAdmin(session)) return Result.fail("无权限");
          }
          return Result.ok(incentiveService.getAchievements(studentNo, courseCode));
      }

      /** 获取称号 | 学生本人 */
      @GetMapping("/{studentNo}/{courseCode}/title")
      public Result<String> title(@PathVariable Integer studentNo,
                                   @PathVariable Integer courseCode,
                                   HttpSession session) {
          Student login = (Student) session.getAttribute("student");
          if (login == null || !login.getStudentNo().equals(studentNo.toString())) {
              if (!auth.isAdmin(session)) return Result.fail("无权限");
          }
          return Result.ok(incentiveService.getTitle(studentNo, courseCode));
      }

      /** 排行榜 | 登录用户 */
      @GetMapping("/leaderboard")
      public Result<List<Map<String, Object>>> leaderboard(@RequestParam(defaultValue = "1") Integer courseCode,
                                                            @RequestParam(defaultValue = "exp") String type,
                                                            HttpSession session) {
          if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
          return Result.ok(incentiveService.getLeaderboard(courseCode, type));
      }
  }
  ```

- [ ] **T12 — 扩展 StudentController 增加导入导出**（满足 R1.3, R1.4, R1.5）
  - 验收：Excel 导入创建学生，导出下载文件
  - 验证：用 curl/POSTMAN 上传 Excel 文件，检查数据库中新增学生
  - 依赖：T3
  - 文件：`controller/StudentController.java` (修改), `service/StudentService.java` (修改), `service/impl/StudentServiceImpl.java` (修改)
  - 规模：M

  **StudentController.java 追加方法**:
  ```java
  /** 批量导入学生 | admin */
  @PostMapping("/import")
  public Result<String> importStudents(@RequestParam MultipartFile file, HttpSession session) {
      if (!auth.isAdmin(session)) return Result.fail("无权限");
      try {
          int count = studentService.importFromExcel(file);
          return Result.ok("成功导入 " + count + " 名学生");
      } catch (IOException e) {
          return Result.fail("导入失败: " + e.getMessage());
      }
  }

  /** 导出学生 | admin */
  @GetMapping("/export")
  public void exportStudents(HttpServletResponse response, HttpSession session) throws IOException {
      if (!auth.isAdmin(session)) {
          response.setStatus(403);
          return;
      }
      response.setContentType("application/vnd.ms-excel");
      response.setHeader("Content-Disposition", "attachment; filename=students.xls");
      studentService.exportToExcel(response.getOutputStream());
  }
  ```

  **StudentService.java 追加方法签名**:
  ```java
  int importFromExcel(MultipartFile file) throws IOException;
  void exportToExcel(OutputStream out) throws IOException;
  ```

  **StudentServiceImpl.java 追加实现**:
  ```java
  @Override
  public int importFromExcel(MultipartFile file) throws IOException {
      int count = 0;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
          String header = reader.readLine(); // skip header
          String line;
          while ((line = reader.readLine()) != null) {
              String[] cols = line.split(",");
              if (cols.length < 5) continue;
              Student s = new Student();
              s.setName(cols[0].trim());
              s.setCollege(cols[1].trim());
              s.setClassName(cols[2].trim());
              s.setUsername(cols[3].trim());
              s.setPassword(cols[4].trim());
              if (baseMapper.selectByUsername(s.getUsername()) != null) continue;
              save(s);
              count++;
          }
      }
      return count;
  }

  @Override
  public void exportToExcel(OutputStream out) throws IOException {
      List<Student> students = list();
      StringBuilder sb = new StringBuilder("姓名,学院,班级,用户名\n");
      for (Student s : students) {
          sb.append(String.join(",",
              s.getName(), s.getCollege(), s.getClassName(), s.getUsername()
          )).append("\n");
      }
      out.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      out.flush();
  }
  ```

### 检查点：阶段4完成

- [ ] `mvn compile` 通过
- [ ] `/profile/1/1` 返回画像 JSON，结构正确
- [ ] `/profile/leaderboard` 返回排行榜
- [ ] 学生导入导出可正常执行

---

### 阶段5：前端实现

- [ ] **T13 — 创建前端 API 封装**（满足全部 R 组的前端数据获取）
  - 验收：API 函数可正确调用后端接口
  - 验证：前端编译通过，调用返回数据
  - 依赖：T11
  - 文件：`frontend/src/api/profile.js`
  - 规模：S

  ```javascript
  import api from './index'

  export const getProfileSummary = (studentNo, courseCode) =>
      api.get(`/profile/${studentNo}/${courseCode}`)

  export const getCompetency = (studentNo, courseCode) =>
      api.get(`/profile/${studentNo}/${courseCode}/competency`)

  export const getRecommendations = (studentNo, courseCode) =>
      api.get(`/profile/${studentNo}/${courseCode}/recommendations`)

  export const generateRecommendations = (studentNo, courseCode) =>
      api.post(`/profile/${studentNo}/${courseCode}/recommendations/generate`)

  export const feedbackRecommendation = (id, feedback) =>
      api.put(`/profile/recommendations/${id}/feedback`, null, { params: { feedback } })

  export const getAchievements = (studentNo, courseCode) =>
      api.get(`/profile/${studentNo}/${courseCode}/achievements`)

  export const getTitle = (studentNo, courseCode) =>
      api.get(`/profile/${studentNo}/${courseCode}/title`)

  export const getLeaderboard = (courseCode, type) =>
      api.get('/profile/leaderboard', { params: { courseCode, type } })

  export const importStudents = (formData) =>
      api.post('/student/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

  export const exportStudents = () =>
      api.get('/student/export', { responseType: 'blob' })
  ```

- [ ] **T14 — 创建 StudentProfile.vue 学生画像页面**（满足 R2.1, R3.1, R4.5, R5.1, R6.1, R7.1）
  - 验收：页面可访问，4个Tab均有数据展示，雷达图可渲染
  - 验证：浏览器打开 `/profile`，切换Tab查看数据
  - 依赖：T13
  - 文件：`frontend/src/views/StudentProfile.vue`
  - 规模：L

  ```vue
  <template>
    <div class="profile-page">
      <!-- 画像总览 -->
      <el-row :gutter="20">
        <el-col :span="16">
          <el-card>
            <template #header>
              <span>属性面板</span>
              <el-tag style="margin-left:10px" :type="statusType">{{ profile.status || '正常学习' }}</el-tag>
              <el-tag style="margin-left:8px">{{ title }}</el-tag>
            </template>
            <el-row :gutter="16">
              <el-col :span="6" v-for="attr in attributes" :key="attr.key">
                <div style="text-align:center">
                  <div style="font-size:28px;font-weight:bold" :style="{color:attr.color}">{{ profile[attr.key] || 0 }}</div>
                  <div style="color:#999;font-size:12px">{{ attr.label }}</div>
                  <el-progress :percentage="attr.percent(profile)" :color="attr.color" :show-text="false" />
                </div>
              </el-col>
            </el-row>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <template #header><span>成长数据</span></template>
            <div>等级：<el-tag>{{ levelName }}</el-tag></div>
            <div style="margin-top:8px">金币：{{ profile.coins || 0 }}</div>
            <div style="margin-top:8px">称号：{{ title }}</div>
            <div style="margin-top:8px">徽章：{{ badgeCount }} 个</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Tab区域 -->
      <el-tabs v-model="activeTab" style="margin-top:20px">
        <el-tab-pane label="能力评分" name="competency">
          <el-card>
            <div ref="radarChart" style="width:100%;height:400px"></div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="个性化推荐" name="recommendations">
          <el-card>
            <div style="margin-bottom:12px">
              <el-button type="primary" @click="refreshRecs" :loading="recLoading">刷新推荐</el-button>
            </div>
            <el-empty v-if="!recommendations.length" description="暂无推荐" />
            <div v-for="rec in recommendations" :key="rec.id"
                 style="padding:12px;margin-bottom:8px;background:#f5f7fa;border-radius:6px;display:flex;justify-content:space-between;align-items:center">
              <div>
                <el-tag size="small" :type="recTypeTag(rec.type)">{{ recTypeLabel(rec.type) }}</el-tag>
                <span style="margin-left:8px;font-weight:bold">{{ rec.targetName }}</span>
                <div style="color:#999;font-size:13px;margin-top:4px">{{ rec.reason }}</div>
              </div>
              <div>
                <el-button size="small" @click="feedbackRec(rec.id, 'useful')">有用</el-button>
                <el-button size="small" @click="feedbackRec(rec.id, 'skip')">跳过</el-button>
              </div>
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="我的成就" name="achievements">
          <el-card>
            <el-empty v-if="!achievements.length" description="暂无成就" />
            <el-row :gutter="12">
              <el-col :span="8" v-for="a in achievements" :key="a.id" style="margin-bottom:12px">
                <div style="text-align:center;padding:16px;background:#fef0f0;border-radius:8px">
                  <div style="font-size:36px">{{ badgeIcon(a.name) }}</div>
                  <div style="font-weight:bold">{{ a.name }}</div>
                  <div style="font-size:12px;color:#999">{{ a.description }}</div>
                </div>
              </el-col>
            </el-row>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="排行榜" name="leaderboard">
          <el-card>
            <el-radio-group v-model="rankType" @change="loadLeaderboard" style="margin-bottom:12px">
              <el-radio-button value="exp">经验排行</el-radio-button>
              <el-radio-button value="coins">金币排行</el-radio-button>
            </el-radio-group>
            <el-table :data="leaderboard" stripe>
              <el-table-column prop="rank" label="排名" width="60" />
              <el-table-column prop="studentNo" label="学号" width="100" />
              <el-table-column prop="level" label="等级" width="80" />
              <el-table-column prop="exp" label="经验值" />
              <el-table-column prop="coins" label="金币" />
              <el-table-column prop="badgeCount" label="徽章数" width="80" />
            </el-table>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </div>
  </template>

  <script setup>
  import { ref, computed, onMounted, nextTick, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import * as echarts from 'echarts'
  import { getProfileSummary, getCompetency, getRecommendations, generateRecommendations,
           feedbackRecommendation, getAchievements, getTitle, getLeaderboard } from '@/api/profile'

  const user = JSON.parse(localStorage.getItem('user') || '{}')
  const studentNo = ref(parseInt(user.studentNo) || 1)
  const courseCode = ref(1)
  const activeTab = ref('competency')
  const profile = ref({})
  const competencyScores = ref([])
  const recommendations = ref([])
  const achievements = ref([])
  const title = ref('')
  const leaderboard = ref([])
  const rankType = ref('exp')
  const recLoading = ref(false)

  const attributes = [
    { key: 'hp', label: 'HP 信心值', color: '#f56c6c',
      percent: p => (p.hp || 0) },
    { key: 'atk', label: 'ATK 解题力', color: '#e6a23c',
      percent: p => (p.atk || 0) },
    { key: 'def', label: 'DEF 基础度', color: '#409eff',
      percent: p => (p.def || 0) },
    { key: 'exp', label: 'EXP 经验值', color: '#67c23a',
      percent: p => Math.min(100, (p.exp || 0) / 20) }
  ]

  const statusType = computed(() => {
    const s = profile.value.status
    if (s === '存在风险') return 'danger'
    if (s === '进度滞后') return 'warning'
    if (s === '能力提升') return 'success'
    return ''
  })

  const levelName = computed(() => {
    const lv = profile.value.level || 1
    const names = { 1: '入门', 2: '初级', 3: '中级', 4: '熟练', 5: '精通' }
    return names[lv] || '入门'
  })

  const badgeCount = computed(() =>
    achievements.value.filter(a => a.achievementType === 'badge').length
  )

  const recTypeTag = (type) => {
    const map = { review_material: 'danger', practice: 'warning',
                  extended_material: 'success', knowledge_point: '' }
    return map[type] || ''
  }
  const recTypeLabel = (type) => {
    const map = { review_material: '复习', practice: '练习',
                  extended_material: '拓展', knowledge_point: '学习' }
    return map[type] || type
  }

  const badgeIcon = (name) => {
    const icons = { '连击王': '🔥', '完美主义': '💎', '速通者': '🏃',
                    'Pythonic': '🐍', 'Debug之眼': '🔍', '夜枭': '🦉', '助人者': '🤝' }
    return icons[name] || '🏆'
  }

  const loadProfile = async () => {
    try {
      const { data } = await getProfileSummary(studentNo.value, courseCode.value)
      if (data.code === 200) {
        profile.value = data.data.profile
        competencyScores.value = data.data.competencyScores || []
        await nextTick()
        if (activeTab.value === 'competency') renderRadar()
      }
    } catch (e) { /* ignore */ }
  }

  const loadRecs = async () => {
    try {
      const { data } = await getRecommendations(studentNo.value, courseCode.value)
      if (data.code === 200) recommendations.value = data.data
    } catch (e) { /* ignore */ }
  }

  const loadAchievements = async () => {
    try {
      const { data } = await getAchievements(studentNo.value, courseCode.value)
      if (data.code === 200) achievements.value = data.data
    } catch (e) { /* ignore */ }
  }

  const loadTitle = async () => {
    try {
      const { data } = await getTitle(studentNo.value, courseCode.value)
      if (data.code === 200) title.value = data.data
    } catch (e) { /* ignore */ }
  }

  const loadLeaderboard = async () => {
    try {
      const { data } = await getLeaderboard(courseCode.value, rankType.value)
      if (data.code === 200) leaderboard.value = data.data
    } catch (e) { /* ignore */ }
  }

  const refreshRecs = async () => {
    recLoading.value = true
    try {
      const { data } = await generateRecommendations(studentNo.value, courseCode.value)
      if (data.code === 200) {
        recommendations.value = data.data
        ElMessage.success('推荐已刷新')
      }
    } catch (e) { /* ignore */ }
    recLoading.value = false
  }

  const feedbackRec = async (id, feedback) => {
    try {
      await feedbackRecommendation(id, feedback)
      ElMessage.success(feedback === 'useful' ? '感谢反馈！' : '已跳过')
      loadRecs()
    } catch (e) { /* ignore */ }
  }

  const radarChart = ref(null)
  const renderRadar = () => {
    if (!radarChart.value || !competencyScores.value.length) return
    const chart = echarts.init(radarChart.value)
    chart.setOption({
      radar: {
        indicator: competencyScores.value.map(c => ({ name: c.abilityPointName, max: 100 }))
      },
      series: [{
        type: 'radar',
        data: [{ value: competencyScores.value.map(c => c.score), name: '能力评分' }],
        areaStyle: { color: 'rgba(64,158,255,0.2)' },
        lineStyle: { color: '#409eff' },
        itemStyle: { color: '#409eff' }
      }]
    })
    window.addEventListener('resize', () => chart.resize())
  }

  watch(activeTab, (tab) => {
    if (tab === 'competency') nextTick(() => renderRadar())
    if (tab === 'leaderboard') loadLeaderboard()
  })

  onMounted(() => {
    loadProfile()
    loadRecs()
    loadAchievements()
    loadTitle()
  })
  </script>

  <style scoped>
  .profile-page { padding: 20px; }
  </style>
  ```

- [ ] **T15 — 更新路由和导航**（满足 R3.6, R7.4）
  - 验收：导航栏出现"我的画像"，点击可跳转
  - 验证：浏览器点击导航链接正确跳转
  - 依赖：T14
  - 文件：`frontend/src/router/index.js` (修改), `frontend/src/views/MainLayout.vue` (修改), `frontend/src/views/StudentManage.vue` (修改)
  - 规模：S

  **router/index.js** — 在 children 数组中追加:
  ```javascript
  { path: 'profile', name: 'StudentProfile', component: () => import('../views/StudentProfile.vue') }
  ```

  **MainLayout.vue** — 在 `<el-menu>` 中 `<el-menu-item index="/stats">` 后追加:
  ```html
  <el-menu-item index="/profile" v-if="user.role==='student'"><el-icon><User /></el-icon>我的画像</el-menu-item>
  ```

  在 `<script setup>` 的 import 中追加:
  ```javascript
  // el-icon 中增加 User 图标（检查 @element-plus/icons-vue 导入）
  ```

  **StudentManage.vue** — 在操作区追加导入导出按钮和上传组件（追加到 template 顶部工具栏）:
  ```html
  <el-upload :show-file-list="false" :before-upload="handleImport" accept=".csv" style="display:inline-block;margin-right:8px">
    <el-button type="success" size="small">导入学生</el-button>
  </el-upload>
  <el-button type="warning" size="small" @click="handleExport">导出学生</el-button>
  ```

  在 `<script setup>` 中追加:
  ```javascript
  import { importStudents, exportStudents } from '@/api/profile'

  const handleImport = async (file) => {
    const formData = new FormData()
    formData.append('file', file)
    try {
      const { data } = await importStudents(formData)
      if (data.code === 200) { ElMessage.success(data.msg); loadData() }
      else ElMessage.error(data.msg)
    } catch (e) { ElMessage.error('导入失败') }
    return false
  }

  const handleExport = async () => {
    try {
      const { data } = await exportStudents()
      const blob = new Blob([data], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = 'students.csv'; a.click()
      URL.revokeObjectURL(url)
    } catch (e) { ElMessage.error('导出失败') }
  }
  ```

### 检查点：阶段5完成

- [ ] `npm run serve` 编译通过，无 lint 错误
- [ ] `/profile` 页面可访问，4个Tab有数据
- [ ] 雷达图正常渲染
- [ ] 推荐列表 ≥ 3 条
- [ ] 排行榜有排序数据
- [ ] 人审通过再继续

---

### 阶段6：集成验证

- [ ] **T16 — 端到端验证与Bug修复**（满足全部 R 组）
  - 验收：全流程走通——学生登录 → 查看画像 → 能力雷达图 → 刷新推荐 → 查看徽章 → 排行榜
  - 验证：手动端到端测试 + curl 验证所有 API
  - 依赖：T1~T15
  - 文件：所有本次改动的文件
  - 规模：L

  ```bash
  # 验证 API
  # 1. 画像总览
  curl -s http://localhost:8080/practical-training/profile/1/1 | python -m json.tool

  # 2. 能力评分
  curl -s http://localhost:8080/practical-training/profile/1/1/competency | python -m json.tool

  # 3. 推荐列表
  curl -s http://localhost:8080/practical-training/profile/1/1/recommendations | python -m json.tool

  # 4. 生成推荐
  curl -s -X POST http://localhost:8080/practical-training/profile/1/1/recommendations/generate | python -m json.tool

  # 5. 排行
  curl -s "http://localhost:8080/practical-training/profile/leaderboard?courseCode=1&type=exp" | python -m json.tool
  ```

### 检查点：阶段6完成 → 模块4实现完成

- [ ] 所有 API 返回 200 + 正确数据结构
- [ ] 前端页面全部可访问且数据展示正确
- [ ] 与现有功能无回归（登录、课程、成绩等不受影响）
- [ ] 更新 `docs/plans/README.md` 状态

---

## 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| TiDB Cloud 延迟导致 API 超时 | 中 | 画像数据量小（单表 < 1000 行），查询走主键索引，不影响 |
| 新表与旧表命名冲突 | 低 | 新表独立命名，不修改现有7张表 |
| ECharts 雷达图渲染失败 | 低 | 使用 Element Plus 内置 Loading 兜底，数据为空显示 Empty |
| 导入 CSV 编码问题 | 低 | 使用 UTF-8，加 BOM 头可被 Excel 识别 |

## Open Questions

- 无

## 越权红线

- 不修改 `student`/`teacher`/`course` 等现有表结构
- 不修改现有 Controller/Service 的行为逻辑（仅在 StudentController 追加导入导出方法）
- 不删除任何已有 API 端点
- 不引入新的 Maven 依赖
- 不引入新的 npm 依赖（ECharts 已在 package.json 中）

## 完成定义（DoD）

- [ ] 4张新表创建成功
- [ ] 8个 Profile API 端点全部可访问
- [ ] 画像 HP/ATK/DEF/EXP 计算逻辑正确
- [ ] 能力雷达图数据接口返回 ≥ 5 个能力点
- [ ] 推荐列表 ≥ 3 条，按优先级排序
- [ ] 7个徽章均可正确触发
- [ ] 排行榜按经验/金币排序
- [ ] 前端 `/profile` 页面 4 个 Tab 均正常展示
- [ ] 学生导入导出功能可用
- [ ] 所有代码可编译启动

## 端到端验证

```bash
# 1. 启动后端
export JAVA_HOME="D:\Java17\jdk-17"
cd backend && mvn spring-boot:run

# 2. 启动前端
cd frontend && npm run serve

# 3. 浏览器访问 http://localhost:3000/
# 4. 学生登录 → 导航栏点击"我的画像" → 查看画像总览
# 5. 切换到"能力评分"Tab → 确认雷达图渲染
# 6. 切换到"个性化推荐"Tab → 点击"刷新推荐" → 确认 ≥ 3 条
# 7. 切换到"我的成就"Tab → 确认徽章展示
# 8. 切换到"排行榜"Tab → 切换经验/金币排行
```
