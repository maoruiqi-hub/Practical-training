# 服务器 Kingbase 数据库连接说明

本项目的开发后端不是直接连接本机数据库，而是通过 SSH 隧道连接服务器上的 KingbaseES。

## 连接关系

```text
本机 Spring Boot
    │
    │ JDBC: jdbc:kingbase8://127.0.0.1:54321/course_platform
    │
本机 127.0.0.1:54321  ← SSH 隧道 →  服务器 127.0.0.1:54321
                                      │
                                      └── KingbaseES
```

因此，配置里的 `127.0.0.1:54321` 是本机 SSH 隧道入口，不代表数据库运行在本机。

## 唯一推荐的启动方式

在项目根目录执行：

```bash
bash scripts/run-backend-local.sh
```

这个脚本会按顺序完成：

1. 读取 `backend/.env` 中的 SSH 隧道配置；
2. 使用 `DB_TUNNEL_HOST` 登录服务器；
3. 将本机 `DB_TUNNEL_LOCAL_PORT` 转发到服务器的数据库端口；
4. 检查隧道端口是否可用；
5. 使用 Kingbase JDBC 驱动启动 Spring Boot；
6. 后端退出时自动关闭 SSH 隧道。

不要直接使用下面的命令启动本地开发后端：

```bash
mvn spring-boot:run
```

直接执行 Maven 会跳过 SSH 隧道。即使 Spring Boot 能启动，后续数据库请求也会连接失败。

## `backend/.env` 配置说明

真实配置文件位于：

```text
backend/.env
```

该文件包含密码和服务器登录信息，已经被 `.gitignore` 忽略，不能提交到 Git。新环境应复制模板：

```bash
cp backend/.env.example backend/.env
```

至少需要确认以下配置：

```dotenv
DB_DRIVER=com.kingbase8.Driver
DB_URL=jdbc:kingbase8://127.0.0.1:54321/course_platform?sslmode=disable
DB_USERNAME=服务器数据库用户名
DB_PASSWORD=服务器数据库密码

DB_TUNNEL_HOST=服务器 SSH 登录地址
DB_TUNNEL_LOCAL_PORT=54321
DB_TUNNEL_REMOTE_HOST=127.0.0.1
DB_TUNNEL_REMOTE_PORT=54321
```

字段含义：

| 配置 | 含义 |
|---|---|
| `DB_URL` | Spring Boot 访问的 JDBC 地址，必须指向本机隧道端口 |
| `DB_USERNAME` / `DB_PASSWORD` | 服务器 Kingbase 的数据库账号，不是 SSH 密码 |
| `DB_TUNNEL_HOST` | SSH 登录服务器的地址，例如 `user@server` |
| `DB_TUNNEL_LOCAL_PORT` | 本机隧道端口，默认 `54321` |
| `DB_TUNNEL_REMOTE_HOST` | 从服务器角度看数据库的地址，通常是 `127.0.0.1` |
| `DB_TUNNEL_REMOTE_PORT` | 从服务器角度看 Kingbase 的端口，默认 `54321` |

## 如何确认连接成功

看到下面三类日志，说明后端已经通过隧道连接服务器数据库：

```text
HikariPool-1 - Added connection com.kingbase8.jdbc.KbConnection
Performance indexes initialized
Tomcat started on port 8081 with context path '/practical-training'
```

只看到 `Tomcat started` 还不够，还要确认 Hikari 已经成功建立 Kingbase 连接。

## 常见失败及处理

### `Connection refused` / `54321`

通常是 SSH 隧道没有建立成功。检查：

```bash
nc -z 127.0.0.1 54321
```

不要手动启动一个长期存在的隧道；直接重新运行 `scripts/run-backend-local.sh`。

### `Cannot load driver class: com.kingbase8.Driver`

说明没有使用 Kingbase 本地 Maven profile。推荐脚本会自动执行：

```bash
mvn -Dkingbase-local spring-boot:run
```

### `Local port 54321 is already in use`

说明已有程序占用隧道端口。先确认它是否是本项目遗留的 SSH 隧道，再关闭遗留进程后重新运行脚本。

### SSH 登录失败

检查本机 SSH 密钥、服务器登录权限和 `DB_TUNNEL_HOST`。不要把密码、私钥或真实 `.env` 内容提交到仓库。

## 相关文件

- `scripts/run-backend-local.sh`：本机开发的唯一推荐启动脚本
- `backend/.env`：本机真实配置，不提交 Git
- `backend/.env.example`：脱敏配置模板
- `backend/src/main/resources/application.yml`：通用 Spring Boot 配置
- `backend/src/main/resources/schema-kingbase.sql`：Kingbase 数据库结构
- `backend/src/main/resources/data-kingbase.sql`：Kingbase 初始化数据

`data-kingbase.sql` 只会在显式设置 `DB_INIT_MODE=always` 时执行，默认 `DB_INIT_MODE=never`，生产启动不会自动写入演示账号、课程或学习记录。`scripts/seed-python-learning-records.js` 和 `scripts/SeedPythonCourseStructure.java` 也要求显式设置 `SEED_ALLOW_DEMO=1`，并且应只连接测试数据库。
- `backend/pom.xml`：`kingbase-local` 本地 JDBC 驱动 profile
