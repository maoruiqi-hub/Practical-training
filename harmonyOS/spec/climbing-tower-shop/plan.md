# Implementation Plan: 爬塔错题商店（鸿蒙学生端）

**Input**: Feature specification from `spec/climbing-tower-shop/spec.md`

## Summary

将已有"连接测试页"（单页 Index.ets）演进为多页面的学生端爬塔鸿蒙 App。完成 4 个页面（登录 → 首页 → 地图 → 商店）和 2 个共享模块（ApiService + ProfilePanel）。使用 Navigation + NavPathStack 实现页面导航，AppStorage 管理跨页面会话状态，完全复用现有 Spring Boot 后端 REST API。

## Technical Context

**Language/Version**: ArkTS (HarmonyOS NEXT API 12)  
**Primary Dependencies**: @kit.NetworkKit (http), @kit.ArkUI (Navigation/NavPathStack/AppStorage)  
**Storage**: 无本地持久化，全部通过 HTTP 与后端交互  
**Testing**: build_project 编译验证 + start_app 设备运行验证  
**Target Platform**: HarmonyOS (phone / tablet / 2in1), API 12+  
**Project Type**: 单模块 entry 鸿蒙 App  
**Performance Goals**: 页面加载 < 2s（局域网 500ms 延迟内），页面切换流畅无卡顿  
**Constraints**: ArkTS 语法限制（无方括号属性访问、无 as 断言、无模板字符串、无 any/unknown、catch 无类型注解、throw 仅限 Error）  
**Scale/Scope**: 4 个页面 + 1 个组件 + 2 个服务文件，5 个后端 API 端点

## Project Structure

### Documentation (this feature)

```text
spec/climbing-tower-shop/
├── spec.md              # 需求规格
├── plan.md              # 本文件（架构设计）
└── tasks.md             # 任务拆分（Phase 3 生成）
```

### Source Code (repository root)

```text
entry/src/main/ets/
├── pages/
│   ├── Index.ets              # 重写：Navigation 根容器 + 页面路由表（唯一 @Entry）
│   ├── LoginPage.ets          # 新建：登录页
│   ├── HomePage.ets           # 新建：学生首页
│   ├── TowerMapPage.ets       # 新建：爬塔地图页
│   └── ShopPage.ets           # 新建：错题商店页
├── components/
│   └── ProfilePanel.ets       # 新建：画像面板（HP/ATK/DEF/EXP/Coins 可复用组件）
├── service/
│   ├── ApiService.ets         # 新建：HTTP 请求封装（静态方法类）
│   └── ApiTypes.ets           # 新建：接口类型定义
├── entryability/
│   └── EntryAbility.ets       # 已存在：入口 Ability（加载 pages/Index）
└── entrybackupability/
    └── EntryBackupAbility.ets # 已存在：备份扩展
```

**Structure Decision**: 单模块 entry 项目，按功能分层——pages（页面）、components（可复用组件）、service（网络+类型）。Index.ets 作为唯一 @Entry 入口，通过 Navigation 动态路由到其他页面，其他页面为纯 @Component 不注册 main_pages.json。

## Complexity Tracking

> 无违规需要说明。本项目严格控制在 8 个文件的单模块范围内。

## Research & Decisions

### RD-01: 页面导航方案

**Decision**: 使用 `Navigation` + `NavPathStack` + `@Builder navDestination`

**Rationale**: 
- Navigation 是华为官方推荐的现代化导航方案，原生支持页面栈管理、转场动画、返回箭头
- NavPathStack 可以通过 `pushPathByName(name, param)` 实现声明式路由，页面名与 builder 中的 if/else 分支一一对应
- @Provide/@Consume 可在组件树中共享同一个 NavPathStack 实例

**Alternatives considered**:
- `@ohos.router`：API 更简单但官方已不推荐新项目使用，不支持 SPA 式的页面内组件路由

### RD-02: 跨页面状态管理

**Decision**: AppStorage（全局单例 Key-Value）存储 baseUrl、sessionCookie、studentNo、courseId；@Provide/@Consume 传递 NavPathStack。

**Rationale**: 
- 会话级数据（后端地址、Cookie、学生 ID、课程 ID）需要跨所有页面共享，AppStorage 无需层层传递
- NavPathStack 是框架对象，用 @Provide/@Consume 在组件树注入，避免作为 @Prop 传参时的类型限制

**Alternatives considered**:
- LocalStorage：页面级隔离，无法跨页面共享
- 全局变量：不响应式，UI 无法自动刷新

### RD-03: HTTP 层架构

**Decision**: 提取为静态方法类 `ApiService`，从当前 Index.ets 的私有方法中提取 normalizeBaseUrl、buildHeaders、saveCookie、request、formatResult。

**Rationale**:
- 避免在每个页面重复 HTTP 逻辑
- 静态方法无需实例化，直接从 AppStorage 读写配置
- saveCookie 的 Cookie 提取逻辑（Object.keys + Object.values + 循环）已验证通过 ArkTS 编译

**Alternatives considered**:
- 单例实例：需要管理生命周期，对当前简单场景过度设计
- 每个页面独立请求：代码重复严重

### RD-04: 页面参数传递

**Decision**: 通过 `navPathStack.pushPathByName('PageName', data)` 的第二个参数传递页面间数据；接收方从 stack 获取参数。

**Rationale**:
- NavPathStack 原生支持参数传递
- 避免了 AppStorage 的额外 key 管理

**Alternatives considered**:
- AppStorage 全局变量：会导致隐式耦合，需要额外清理
- 路由 URL query string：对非简单字符串类型序列化复杂

### RD-05: 商店操作后的状态同步

**Decision**: 操作成功后调用 game-event API，该 API 返回更新后的 profile（含新金币余额），直接刷新 ShopPage 内的金币显示和按钮状态。

**Rationale**:
- 原后端 `ProfileApiController.receiveGameEvent()` 返回 `Result.ok(profileService.getProfileSummary(...))` — 即操作后直接返回最新画像数据
- 无需额外 API 调用即可获得最新金币数
- 按钮变为"已完成"后置灰，防止重复操作

**Alternatives considered**:
- 操作后不刷新：金币数值不准确
- 额外调用 profile API：多一次网络请求

### RD-06: ArkUI 组件选择与"不造轮子"

**Decision**: 纯 ArkUI 内置组件实现，不使用任何第三方 UI 库。图标使用 Unicode 符号（💰🛡️⚔️❤️⭐🏪）。

**Rationale**:
- HarmonyOS 生态中无可用的第三方 ArkUI 组件库（无 Element Plus 等 Web 框架等价物）
- ArkUI 内置组件（Button, Text, Progress, Scroll, Column, Row, Navigation, NavDestination）足以覆盖所有 UI 需求
- Unicode 符号作为图标，零依赖，在所有设备上渲染一致
- 深色主题配色方案参考原 Vue 实现（#F8EDCF 文字, #DFA54F 强调, #1E1E2E 卡片背景）

**Alternatives considered**:
- 自定义 SVG/PNG 图标：增加资源维护成本，spec 明确 Non-goal

### API 响应结构（来自原代码验证）

**Mistakes API**: `GET /api/students/{no}/mistakes`
```json
{ "code": 200, "data": { "wrongList": [ { "questionId": 1, "questionStem": "...", "stem": "...", "knowledgePointName": "..." } ] } }
```

**Game-event API**: `POST /api/students/{id}/game-event` — **无需认证头**
```json
// Request:
{ "course_id": "1", "event_type": "shop_purchased", "payload": {} }
// Response (返回更新后的画像):
{ "code": 200, "data": { "nickname": "...", "coins": 85, "hp": 100, ... } }
```

**Tower-map API**: `GET /api/students/{no}/tower-map?course_id={id}`
```json
{ "code": 200, "data": [ { "nodeId": 1, "kpName": "...", "roomType": "shop", "level": 1, "status": "unlocked", ... } ] }
```

## Data Model

### ApiTypes.ets 类型定义

```typescript
// 登录请求体
interface LoginBody {
  username: string;
  password: string;
}

// 登录响应（后端返回的 JSON 结构，字段宽松）
interface LoginResponse {
  code: number;
  message: string;
  data: StudentBrief;
}

interface StudentBrief {
  no: string;
  name: string;
}

// 学生画像（从 /api/students/{no}/profile 返回）
interface StudentProfile {
  nickname: string;
  level: number;
  hp: number;
  maxHp: number;
  attack: number;
  defense: number;
  exp: number;
  coins: number;
  energy: number;
  maxEnergy: number;
}

// 爬塔节点（从 /api/students/{no}/tower-map 返回）
interface TowerNode {
  id: number;
  name: string;
  roomType: string;       // 'diagnosis'|'battle'|'elite'|'boss'|'treasure'|'shop'|'rest'|'event'
  floor: number;
  status: string;         // 'locked'|'unlocked'|'completed'
  kpId: number;
  kpName: string;
}

// 错题记录（从 /api/students/{no}/mistakes 返回）
interface MistakeItem {
  questionId: number;
  questionStem: string;
  stem: string;
  knowledgePointName: string;
}

// 游戏事件请求体
interface GameEventBody {
  course_id: string;
  event_type: string;     // 'shop_purchased'
  payload: Object;
}

// 通用 API 响应包装
interface ApiResponse {
  code: number;
  message: string;
  data: Object;
}
```

### 状态模型

| Key | Type | Scope | 写入方 | 读取方 |
|-----|------|-------|--------|--------|
| `baseUrl` | string | AppStorage | LoginPage | ApiService |
| `sessionCookie` | string | AppStorage | ApiService.saveCookie | ApiService.buildHeaders |
| `studentNo` | string | AppStorage | LoginPage | HomePage, TowerMapPage, ShopPage, ApiService |
| `courseId` | string | AppStorage | LoginPage | HomePage, TowerMapPage, ShopPage, ApiService |
| `navPathStack` | NavPathStack | @Provide/@Consume | Index.ets | 所有子页面 |

## Contracts & Interfaces

### ApiService 方法契约

所有方法为静态方法，从 AppStorage 读取 baseUrl 和 cookie。

| 方法 | 签名 | 行为 |
|------|------|------|
| `setBaseUrl` | `(url: string): void` | 写入 AppStorage('baseUrl') |
| `getBaseUrl` | `(): string` | 读取 AppStorage('baseUrl')，标准化（去尾部 /） |
| `getCookie` | `(): string` | 读取 AppStorage('sessionCookie') |
| `buildHeaders` | `(): Record<string, string>` | 构造 Content-Type + Cookie 头 |
| `saveCookie` | `(rawHeader: Object): void` | 从响应头提取 Set-Cookie 写入 AppStorage |
| `get` | `async (path: string): Promise<string>` | GET 请求，返回原始 body 字符串 |
| `post` | `async (path: string, body: Object): Promise<string>` | POST 请求，返回原始 body 字符串 |
| `formatResult` | `(raw: string): string` | JSON 格式化（用于调试），入参非 JSON 时返回原文 |

### 后端 API 契约

| 端点 | 方法 | 请求 | 响应关键字段 | 使用页面 |
|------|------|------|-------------|---------|
| `/api/students/login` | POST | `{username, password}` (JSON) | Set-Cookie 响应头, JSON body: `{code, data: {no, name}}` | LoginPage |
| `/api/students/{no}/profile?course_id={id}` | GET | Cookie 请求头 | `{nickname, level, hp, maxHp, attack, defense, exp, coins, energy}` | HomePage, TowerMapPage |
| `/api/students/{no}/tower-map?course_id={id}` | GET | Cookie | 节点数组 `[{id, name, roomType, floor, status, kpId, kpName}]` | TowerMapPage |
| `/api/students/{no}/mistakes` | GET | Cookie | `{wrongList: [{questionId, questionStem, knowledgePointName}]}` | ShopPage |
| `/api/students/{no}/game-event` | POST | `{course_id, event_type, payload}` (JSON) | `{code, message}` | ShopPage |

### 页面间路由契约

| 路由名 (`pushPathByName`) | 源页面 | 目标页面 | 参数 |
|---------------------------|--------|---------|------|
| `'Home'` | LoginPage | HomePage | `undefined` |
| `'TowerMap'` | HomePage | TowerMapPage | `undefined` |
| `'Shop'` | TowerMapPage | ShopPage | `TowerNode`（被点击的商店节点） |

### 组件接口：ProfilePanel

```typescript
@Component
struct ProfilePanel {
  @Prop hp: number = 0;
  @Prop maxHp: number = 0;
  @Prop attack: number = 0;
  @Prop defense: number = 0;
  @Prop exp: number = 0;
  @Prop coins: number = 0;
  build() { /* 横向 Row 展示五个属性 */ }
}
```

### Navigation 路由表（Index.ets navDestination builder）

```typescript
@Builder
pageMap(name: string, param: Object) {
  if (name === 'Home') {
    HomePage()
  } else if (name === 'TowerMap') {
    TowerMapPage()
  } else if (name === 'Shop') {
    ShopPage({ nodeData: param as TowerNode })  // 注意：as 断言仅在 builder 中不可避免
  }
}
```

> **注意**：navDestination builder 中的 `as` 断言是框架约定的参数类型转换，属于编译器认可的模式。其他业务代码中严格禁止使用 `as`。

### 错误处理契约

各页面 `aboutToAppear` 或点击事件中的 API 调用统一包裹 `try-catch`：

```typescript
try {
  const raw: string = await ApiService.get('/path');
  // 解析并更新 UI
} catch (err) {
  this.statusText = 'XXX请求失败，请检查网络和后端状态。';
}
```

- catch 子句**不带**类型注解
- 不 throw（吞掉异常，转为 UI 提示）
- 不在 catch 中嵌套网络请求
