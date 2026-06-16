# AI智慧课程平台

深度融合人工智能技术的教育管理及学习辅助平台，以"智能化生产、结构化管理、个性化学习"为核心目标。

## 项目结构
```
├── docs/       # 需求、设计、API规格文档
├── frontend/   # 前端应用（教师端/学生端）
├── backend/    # 后端API服务
├── agentic/    # AI Agent服务（知识图谱、智能批改、推荐系统等）
└── resource/   # 静态资源、教学资源模板
```

## 数据模型

### Student（学生）

| 字段 | 类型 | 说明 |
|------|------|------|
| `studentNo` | INT (PK) | 学号，自增主键 |
| `name` | VARCHAR | 学生姓名 |
| `college` | VARCHAR | 所属学院 |
| `className` | VARCHAR | 班级，如"计科202班" |
| `courseGrades` | TEXT | 课程成绩（预留，JSON或文本格式） |
| `username` | VARCHAR | 登录用户名，唯一 |
| `password` | VARCHAR | 登录密码 |

### Teacher（教师）

| 字段 | 类型 | 说明 |
|------|------|------|
| `teacherNo` | INT (PK) | 教职工码，自增主键 |
| `name` | VARCHAR | 教师姓名 |
| `college` | VARCHAR | 所属学院 |
| `major` | VARCHAR | 专业方向（系） |
| `phone` | VARCHAR | 联系电话 |
| `role` | VARCHAR | 角色：`teacher` 教师 / `admin` 管理员 |
| `username` | VARCHAR | 登录用户名，唯一 |
| `password` | VARCHAR | 登录密码 |

### Course（课程）

| 字段 | 类型 | 说明 |
|------|------|------|
| `courseCode` | INT (PK) | 课程编号，自增主键 |
| `courseName` | VARCHAR | 课程名称 |
| `teacher` | VARCHAR | 授课教师姓名（关联 Teacher.name） |
| `credits` | INT | 学分 |
| `hours` | INT | 总学时 |
| `coverUrl` | VARCHAR | 封面图片路径，如 `resource/CourseResource/xxx.png` |
| `lessons` | (虚拟) | 课时列表，仅内存映射，不持久化 |

### Lesson（课时）

| 字段 | 类型 | 说明 |
|------|------|------|
| `lessonNo` | INT (PK) | 课时编号，自增主键 |
| `courseCode` | INT | 所属课程编号（外键 → Course） |
| `lessonTitle` | VARCHAR | 课时标题 |
| `resourceType` | VARCHAR | 资源类型：`video` / `ppt` / `doc` / `img` |
| `resourceUrl` | VARCHAR | 资源文件路径，上传后自动记录 |
| `description` | TEXT | 内容简介 |

### LearningTask（学习任务）

| 字段 | 类型 | 说明 |
|------|------|------|
| `taskNo` | INT (PK) | 任务编号，自增主键 |
| `courseCode` | INT | 所属课程编号（外键 → Course） |
| `taskType` | VARCHAR | 任务类型，如"编程作业""课堂测验" |
| `description` | TEXT | 任务说明 |
| `deadline` | DATETIME | 截止时间 |
| `submitMethod` | VARCHAR | 提交方式，如"在线提交""文档上传" |
| `score` | INT | 任务分值 |

### TaskSubmission（任务提交记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| `submissionId` | INT (PK) | 提交编号，自增主键 |
| `taskNo` | INT | 关联任务编号（外键 → LearningTask） |
| `studentNo` | INT | 提交学生学号（外键 → Student） |
| `content` | TEXT | 文字提交内容 |
| `filePath` | VARCHAR | 上传附件路径，如 `resource/HomeworkUpload/xxx.pdf` |
| `submitTime` | DATETIME | 提交时间 |
| `score` | INT | 教师评分 |
| `status` | VARCHAR | 状态：`submitted` 已提交 / `graded` 已批改 |
| `feedback` | TEXT | 教师评语/反馈 |

## 技术栈

### 后端

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.5.15 | Web 框架 |
| MyBatis-Plus | 3.5.15 | ORM（含逻辑删除、驼峰映射） |
| MySQL | runtime | 数据库（`mysql-connector-j`） |
| Lombok | provided | 简化 Java 代码 |
| JUnit 5 | (Spring Boot 内置) | 测试框架 |
| DevTools | optional | 开发热重载 |

### 前端

| 组件 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| Element Plus | 2.14+ | UI 组件库 |
| Vue Router | 4.x | 前端路由 |
| Axios | 1.x | HTTP 请求 |
| ECharts | 6.x | 成绩可视化图表 |
| Pinia | 3.x | 状态管理（预留） |

### 构建工具

| 组件 | 版本 |
|------|------|
| Maven | 3.9+ |
| Vue CLI | 5.x |

## API 接口

> 鉴权基于 HttpSession，角色分三类：`student` / `teacher` / `admin`

### 权限矩阵

| 接口 | student | teacher | admin | 备注 |
|------|:---:|:---:|:---:|------|
| **登录注册** | | | | |
| `POST /student/login` `POST /student/register` | ✅ | ✅ | ✅ | 公开 |
| `POST /teacher/login` `POST /teacher/register` | ✅ | ✅ | ✅ | 公开 |
| **模糊查询** | | | | |
| `GET /teacher/search?keyword=` | ✅ | ✅ | ✅ | 按姓名/学院 |
| `GET /course/search?keyword=` | ✅ | ✅ | ✅ | 按名称/编号 |
| `GET /lesson/search?keyword=` | ✅ | ✅ | ✅ | 按标题/简介 |
| `GET /task/search?keyword=` | ✅ | ✅ | ✅ | 按类型/说明 |
| **关联查询** | | | | |
| `GET /course/{courseCode}/lessons` | ✅ | ✅ | ✅ | 课程下级课时 |
| `GET /lesson/{courseCode}` | ✅ | ✅ | ✅ | 某课程下所有课时 |
| `GET /lesson/detail/{lessonNo}` | ✅ | ✅ | ✅ | 课时详情（含课程名、教师名） |
| `GET /task/{courseCode}` | ✅ | ✅ | ✅ | 课程下级任务 |
| **管理员专用** | | | | |
| `GET/PUT/DELETE /student/{studentNo}` | ❌ | ❌ | ✅ | |
| `GET/PUT/DELETE /teacher/{teacherNo}` | ❌ | ❌ | ✅ | |
| `GET /student/list` `GET /teacher/list` | ❌ | ❌ | ✅ | |
| `GET /course/list` `GET /course/{courseCode}` | ❌ | ❌ | ✅ | |
| `POST /course` `DELETE /course/{courseCode}` | ❌ | ❌ | ✅ | |
| **授课教师可操作** | | | | |
| `PUT /course/{courseCode}` | ❌ | ✅ ① | ✅ | ① 仅限该课授课教师 |
| `POST /lesson` `PUT/DELETE /lesson/{code}/{no}` | ❌ | ✅ ① | ✅ | |
| `POST /task` `PUT/DELETE /task/{code}/{no}` | ❌ | ✅ ① | ✅ | |
| **任务提交** | | | | |
| `POST /submission` | ✅ | ❌ | ❌ | 提交文字+附件 |
| `GET /submission/my` | ✅ | ❌ | ❌ | 查看自己的提交 |
| `GET /submission/task/{taskNo}` | ❌ | ✅ ① | ✅ | 查看某任务所有提交 |
| `PUT /submission/{submissionId}` | ❌ | ✅ ① | ✅ | 打分+反馈 |
| **成绩统计** | | | | |
| `GET /stats/student/{studentNo}` | ✅ ② | ❌ | ✅ | 个人成绩总览+趋势 |
| `GET /stats/course/{courseCode}` | ❌ | ✅ ① | ✅ | 课程各任务统计 |

> ① 仅限该课授课教师 &nbsp; ② 仅限学生本人

### 实体关系

```
Course ──1:N──▶ Lesson
Course ──1:N──▶ LearningTask ──1:N──▶ TaskSubmission
Student ──────────────────────1:N──▶ TaskSubmission
Teacher ──(授课)──▶ Course
```

## 待解决问题

1. **学生与课程绑定**：目前学生和课程没有直接关联，无法控制学生能看到哪些课程和任务。需要引入选课/课程分配机制。

2. **班级管理**：尚未引入班级概念。理想方式是老师认领班级及班级下学生，学生和老师分别只看自己班级/授课范围内的任务，避免不同班级学生互相看到彼此的任务提交。

3. **批量导入导出**：学生和老师信息目前只能逐条录入，缺少 Excel/CSV 批量导入功能。成绩报表、学生名单等也需要导出功能。

4. **前端 UI 美化**：当前前端功能可用但样式较简陋，需整体润色排版、配色、间距等视觉效果。

## 注意事项

1. **数据库延迟**：数据库部署在 TiDB Cloud（新加坡节点），网络延迟较高。首次加载或重启后查询可能需等待 10~30 秒，页面出现短暂空白或 loading 属正常现象，不是数据丢失或接口损坏。

2. **先登录再操作**：所有功能页面需要先登录获取 session 后才能正常访问。如果直接通过 URL 跳转到 `/courses`、`/stats` 等内页而未登录，接口会返回 `请先登录` 或显示空白/报错，属于预期行为。

3. **课程与教师的关系**：指导老师对"教师对课程是一对一还是一对多"未明确说明。当前后端 `Course.teacher` 存教师姓名，天然支持多对多，若后续要求改为一对一，基本只改前端即可，后端无需大动。

4. **底层代码不是死的**：现有的 Controller、Service、Mapper 只是搭了个骨架，不是不能动。如果觉得结构不合理、方法多余、或者有更好的写法，直接改，不用问我。这些代码是参考，不是约束。

## 代码规范

1. **别跨层**：Controller → Service → Mapper，各调各的。Controller 别直接调 Mapper，也别注别人的 Service。比如 CourseController 要用 Lesson 的数据，应该走 CourseService 去委托 LessonService，而不是 Controller 自己注一个 LessonService 进来。

2. **Service 能不写就不写**：IService 自带增删改查了（`getById` `save` `list` 这些），别写重复的包装方法。只有自定义 SQL（模糊查 LIKE、按外键查）和业务逻辑（登录、注册）才需要声明。

3. **我自己也有漏的**：上面两条我也没全部遵守，有些地方可能偷懒直接注了别的 Service。翻到直接改。

## 资源目录说明

| 目录 | 内容 |
|------|------|
| `backend/src/main/resources/` | mapper XML、`application.yml`、`schema.sql`、`data.sql`，放项目配置和 SQL |
| `resource/`（项目根） | 上传的课件、作业附件、课程封面、测试图片。`WebConfig` 映射到 `/resource/**` 对外访问。里面 `red1.png` `white1.png` `python.png` 是占位玩的，以后换了就行 |

