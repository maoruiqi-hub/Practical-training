# KingbaseES 接入说明

后端已保留默认 H2/MySQL 开发配置，并新增 `kingbase` Spring profile。

## 启动配置

设置环境变量：

```bash
SPRING_PROFILES_ACTIVE=kingbase
DB_URL=jdbc:kingbase8://localhost:54321/practical_training
DB_USERNAME=SYSTEM
DB_PASSWORD=your-password
```

首次初始化空库时可临时打开：

```bash
DB_INIT_MODE=always
```

初始化完成后建议恢复为：

```bash
DB_INIT_MODE=never
```

## 相关文件

- `backend/src/main/resources/application-kingbase.yml`
- `backend/src/main/resources/schema-kingbase.sql`
- `backend/src/main/resources/data-kingbase.sql`

## 设计约定

当前 Java 代码大量以 `String` 传递学生、教师、课程、任务、题目等 ID。为避免 KingbaseES 中出现 `integer = character varying` 一类类型比较错误，金仓 DDL 将业务主键和跨表引用统一为 `VARCHAR`。

历史自增 ID 在金仓中通过序列默认值生成数字字符串，例如 `student_no` 默认来自 `student_no_seq`。新模块的 UUID 主键继续由 MyBatis-Plus `ASSIGN_UUID` 在应用侧生成。
