# 鸿蒙 HarmonyOS NEXT PC端应用开发总结

> 本文档基于 AI智慧课程平台 项目的扩展加分任务需求编写，总结鸿蒙PC端应用开发的核心知识点。

---

## 一、项目背景

### 1.1 原有项目概述
AI智慧课程平台是一个深度融合人工智能技术的教育管理及学习辅助平台，原本基于Web技术开发，包含：
- **前端**：教师端/学生端 Web 应用
- **后端**：API 服务
- **AI Agent**：知识图谱、智能批改、推荐系统等
- **资源管理**：静态资源、教学资源模板

### 1.2 鸿蒙扩展目标
将Web端核心功能移植到鸿蒙PC端应用，实现：
- 教师端：班级管理、学生管理、任务发布、成绩管理
- 学生端：课程管理、作业提交、成绩查看
- 知识图谱可视化
- AI智能推荐与批改功能

---

## 二、鸿蒙开发环境搭建

### 2.1 DevEco Studio 安装

| 项目 | 要求 |
|------|------|
| **IDE** | DevEco Studio（最新版本） |
| **下载地址** | https://developer.huawei.com/consumer/cn/download/ |
| **操作系统** | Windows 10/11 (64位) 或 macOS |
| **内存** | ≥ 16GB RAM（推荐） |
| **磁盘空间** | ≥ 100GB 可用空间 |

### 2.2 环境配置步骤

1. **下载并安装 DevEco Studio**
2. **登录华为开发者账号**（需注册）
3. **下载 HarmonyOS SDK**（API 12 及以上）
4. **配置模拟器**（可选，支持2in1设备模拟）

### 2.3 创建PC端项目

1. 打开 DevEco Studio → **Create Project**
2. 选择 **Application** → **Empty Ability** 模板
3. 配置项目信息：
   - **Project Name**: 自定义项目名
   - **Bundle Name**: 应用包名
   - **Compile SDK**: 选择 API 12 或更高
   - **Compatible SDK**: 选择兼容的最低版本
4. 点击 **Finish** 完成创建

---

## 三、核心开发知识

### 3.1 ArkTS 语言基础

ArkTS 是 HarmonyOS 应用开发的主力语言，基于 TypeScript 扩展，增加了声明式UI描述能力。

#### 基本语法示例

```typescript
// 装饰器：标记组件
@Entry      // 标记页面入口组件
@Component  // 标记自定义组件
struct Index {
  // @State 装饰器：状态变量，变化时触发UI刷新
  @State message: string = 'Hello HarmonyOS';

  build() {
    // 声明式UI描述
    Row() {
      Column() {
        Text(this.message)
          .fontSize(50)
          .fontWeight(FontWeight.Bold)
      }
      .width('100%')
    }
    .height('100%')
  }
}
```

#### 关键装饰器

| 装饰器 | 作用 | 使用场景 |
|--------|------|----------|
| `@Entry` | 标记页面入口组件 | 每个页面的根组件 |
| `@Component` | 标记自定义组件 | 可复用的UI组件 |
| `@State` | 组件内部状态 | 组件内部可变数据 |
| `@Link` | 父子组件双向同步 | 父子组件共享状态 |
| `@Prop` | 父子组件单向同步 | 父组件向子组件传递数据 |
| `@Provide/@Consume` | 跨组件共享 | 祖先与后代组件共享数据 |
| `@Observed/@ObjectLink` | 嵌套对象观察 | 复杂对象的状态管理 |

### 3.2 ArkUI 框架

ArkUI 是鸿蒙的声明式UI开发框架，提供丰富的组件和布局能力。

#### 常用基础组件

```typescript
// 文本组件
Text('文本内容')
  .fontSize(16)
  .fontColor('#333333')
  .fontWeight(FontWeight.Normal)

// 按钮组件
Button('点击我')
  .type(ButtonType.Capsule)
  .backgroundColor('#0D9FFB')
  .width(120)
  .height(40)
  .onClick(() => {
    // 点击事件处理
  })

// 输入框组件
TextInput({ placeholder: '请输入内容' })
  .width('100%')
  .height(40)

// 图片组件
Image($r('app.media.icon'))
  .width(100)
  .height(100)

// 列表组件
List() {
  ForEach(this.items, (item: string) => {
    ListItem() {
      Text(item)
    }
  })
}
```

#### 布局容器

```typescript
// 线性布局（水平）
Row() {
  Text('左侧')
  Text('右侧')
}
.justifyContent(FlexAlign.SpaceBetween)
.width('100%')

// 线性布局（垂直）
Column() {
  Text('上方')
  Text('下方')
}
.width('100%')
.height('100%')

// 弹性布局
Flex({ direction: FlexDirection.Row, wrap: FlexWrap.Wrap }) {
  // 子组件
}

// 相对布局
RelativeContainer() {
  Text('参考组件')
    .id('anchor')
  Text('相对定位')
    .alignRules({
      top: { anchor: 'anchor', align: VerticalAlign.Bottom },
      left: { anchor: 'anchor', align: HorizontalAlign.Start }
    })
}
```

### 3.3 页面路由

```typescript
import { BusinessError } from '@kit.BasicServicesKit';

// 页面跳转
let uiContext: UIContext = this.getUIContext();
let router = uiContext.getRouter();

// 跳转到指定页面
router.pushUrl({ url: 'pages/Second' }).then(() => {
  console.info('跳转成功');
}).catch((err: BusinessError) => {
  console.error(`跳转失败: ${err.message}`);
});

// 返回上一页
router.back();
```

#### 路由配置（main_pages.json）

```json
{
  "src": [
    "pages/Index",
    "pages/StudentHome",
    "pages/TeacherHome",
    "pages/CourseList",
    "pages/TaskManage",
    "pages/GradeView"
  ]
}
```

---

## 四、PC端开发专项

### 4.1 设备类型配置

在 `module.json5` 中配置支持PC端（2in1设备）：

```json5
{
  "module": {
    "name": "entry",
    "type": "entry",
    "deviceTypes": [
      "phone",    // 手机
      "tablet",   // 平板
      "2in1"      // PC/二合一设备
    ],
    // ...其他配置
  }
}
```

### 4.2 响应式布局（断点系统）

鸿蒙提供断点系统适配不同屏幕尺寸：

| 断点 | 屏幕宽度 | 典型设备 |
|------|----------|----------|
| `xs` | < 320vp | 小屏手机 |
| `sm` | 320vp - 520vp | 标准手机 |
| `md` | 520vp - 840vp | 平板/折叠屏 |
| `lg` | 840vp - 1200vp | 小屏PC |
| `xl` | 1200vp - 1440vp | 标准PC |
| `xxl` | ≥ 1440vp | 大屏PC |

#### 断点使用示例

```typescript
@Entry
@Component
struct ResponsiveLayout {
  // 获取当前断点
  @StorageProp('breakpoint') breakpoint: string = 'md';

  build() {
    Row() {
      if (this.breakpoint === 'lg' || this.breakpoint === 'xl' || this.breakpoint === 'xxl') {
        // PC端：侧边栏 + 内容区
        SideBar()
        ContentArea()
      } else {
        // 移动端：单栏布局
        ContentArea()
      }
    }
  }
}
```

#### 栅格布局（GridRow/GridCol）

```typescript
GridRow({ columns: { sm: 4, md: 8, lg: 12 } }) {
  GridCol({ span: { sm: 4, md: 4, lg: 6 } }) {
    // 左侧内容
    Text('左侧区域')
  }
  GridCol({ span: { sm: 4, md: 4, lg: 6 } }) {
    // 右侧内容
    Text('右侧区域')
  }
}
```

### 4.3 窗口管理

PC端支持多窗口和自由窗口模式：

```typescript
import { window } from '@kit.ArkUI';

// 获取主窗口
let windowClass: window.Window;

// 设置窗口属性
windowClass.setWindowLayoutFullScreen(true);

// 监听窗口尺寸变化
windowClass.on('windowSizeChange', (data: window.Size) => {
  console.info(`窗口尺寸变化: ${data.width} x ${data.height}`);
  // 根据尺寸调整布局
});

// 设置窗口最小尺寸
windowClass.setMinWindowStyle(800, 600);
```

### 4.4 键盘鼠标交互

PC端需要处理桌面特有的交互：

```typescript
// 鼠标悬停效果
Text('悬停文本')
  .onHover((isHover: boolean) => {
    if (isHover) {
      // 鼠标进入
      this.isHovered = true;
    } else {
      // 鼠标离开
      this.isHovered = false;
    }
  })

// 右键菜单
Text('右键菜单')
  .bindContextMenu(() => {
    return new MenuBuilder()
      .addItem(new MenuItem({ content: '复制' }))
      .addItem(new MenuItem({ content: '粘贴' }))
      .build();
  }, ResponseType.LongPress)

// 键盘快捷键
Text('快捷键')
  .onKeyEvent((event: KeyEvent) => {
    if (event.keyCode === KeyCode.KEY_S && event.ctrlKey) {
      // Ctrl+S 保存
      this.save();
    }
  })
```

---

## 五、Stage 应用模型

### 5.1 应用生命周期

```typescript
// EntryAbility.ets
import { AbilityConstant, UIAbility, Want } from '@kit.AbilityKit';

export default class EntryAbility extends UIAbility {
  // 应用创建
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    console.info('应用创建');
  }

  // 窗口创建
  onWindowStageCreate(windowStage: window.WindowStage): void {
    console.info('窗口创建');
    // 加载首页
    windowStage.loadContent('pages/Index', (err) => {
      if (err) {
        console.error(`加载失败: ${err.message}`);
        return;
      }
      console.info('加载成功');
    });
  }

  // 窗口销毁
  onWindowStageDestroy(): void {
    console.info('窗口销毁');
  }

  // 应用前台
  onForeground(): void {
    console.info('应用进入前台');
  }

  // 应用后台
  onBackground(): void {
    console.info('应用进入后台');
  }

  // 应用销毁
  onDestroy(): void {
    console.info('应用销毁');
  }
}
```

### 5.2 项目目录结构

```
├── AppScope/
│   ├── app.json5                # 应用级配置
│   └── resources/               # 应用级资源
├── entry/
│   ├── src/
│   │   └── main/
│   │       ├── ets/
│   │       │   ├── entryability/    # 入口能力
│   │       │   ├── pages/           # 页面
│   │       │   ├── components/      # 公共组件
│   │       │   ├── model/           # 数据模型
│   │       │   ├── service/         # 业务服务
│   │       │   └── utils/           # 工具类
│   │       ├── resources/           # 模块资源
│   │       └── module.json5         # 模块配置
│   ├── build-profile.json5         # 构建配置
│   └── oh-package.json5            # 依赖配置
└── build-profile.json5             # 工程配置
```

---

## 六、网络请求与数据管理

### 6.1 HTTP 请求

```typescript
import { http } from '@kit.NetworkKit';

// GET 请求
async function fetchData() {
  let httpRequest = http.createHttp();
  let response = await httpRequest.request(
    'https://api.example.com/courses',
    {
      method: http.RequestMethod.GET,
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer token'
      }
    }
  );

  if (response.responseCode === 200) {
    let data = JSON.parse(response.result as string);
    return data;
  }
}

// POST 请求
async function submitData(data: object) {
  let httpRequest = http.createHttp();
  let response = await httpRequest.request(
    'https://api.example.com/submit',
    {
      method: http.RequestMethod.POST,
      header: {
        'Content-Type': 'application/json'
      },
      extraData: JSON.stringify(data)
    }
  );
  return response;
}
```

### 6.2 本地存储

```typescript
import { preferences } from '@kit.ArkData';

// 保存数据
async function saveData(key: string, value: string) {
  let context = getContext(this);
  let dataPreferences = await preferences.getPreferences(context, 'settings');
  await dataPreferences.put(key, value);
  await dataPreferences.flush();
}

// 读取数据
async function loadData(key: string) {
  let context = getContext(this);
  let dataPreferences = await preferences.getPreferences(context, 'settings');
  let value = await dataPreferences.get(key, '');
  return value;
}
```

---

## 七、AI智慧课程平台鸿蒙端功能规划

### 7.1 页面结构设计

```
├── pages/
│   ├── Login.ets              # 登录页
│   ├── Register.ets           # 注册页
│   ├── StudentHome.ets        # 学生主页
│   ├── TeacherHome.ets        # 教师主页
│   ├── CourseList.ets         # 课程列表
│   ├── CourseDetail.ets       # 课程详情
│   ├── TaskList.ets           # 任务列表
│   ├── TaskSubmit.ets         # 任务提交
│   ├── GradeView.ets          # 成绩查看
│   ├── KnowledgeGraph.ets     # 知识图谱
│   ├── ResourceCenter.ets     # 资源中心
│   └── Profile.ets            # 个人中心
```

### 7.2 核心功能模块

| 模块 | 功能说明 | 优先级 |
|------|----------|--------|
| 用户认证 | 登录、注册、角色切换 | P0 |
| 课程管理 | 课程列表、详情、搜索 | P0 |
| 任务管理 | 任务发布、提交、查看 | P0 |
| 成绩管理 | 成绩录入、统计、可视化 | P1 |
| 知识图谱 | 图谱构建、可视化展示 | P1 |
| 资源管理 | 资源上传、下载、预览 | P2 |
| AI推荐 | 学习内容智能推荐 | P2 |

### 7.3 与Web端API对接

鸿蒙端复用Web端的后端API，需要：
1. 统一API基础URL配置
2. 实现Token认证机制
3. 处理请求/响应数据格式
4. 错误处理与重试机制

---

## 八、开发资源与参考

### 8.1 官方文档
- **华为开发者官网**: https://developer.huawei.com/consumer/cn/doc/
- **HarmonyOS 开发指南**: 开发 → 应用开发
- **ArkTS API参考**: API参考 → ArkTS API
- **ArkUI组件库**: 开发 → UI开发 → 组件

### 8.2 开发工具
- **DevEco Studio**: 官方IDE
- **HarmonyOS SDK**: API 12+
- **模拟器**: 支持2in1设备模拟

### 8.3 学习资源
- 华为开发者学堂（视频教程）
- HarmonyOS 开发者社区
- OpenHarmony 开源项目（Gitee）

---

## 九、开发注意事项

1. **API版本兼容**: 建议使用 API 12 及以上版本
2. **PC端适配**: 必须处理不同屏幕尺寸的响应式布局
3. **交互差异**: PC端需适配鼠标悬停、右键菜单、键盘快捷键
4. **性能优化**: 大数据列表使用懒加载，图片使用缓存
5. **状态管理**: 合理使用装饰器管理组件状态
6. **网络处理**: 处理弱网环境和请求超时
7. **安全合规**: 遵守华为应用市场审核要求

---

*文档编写日期: 2026年6月14日*
*基于 HarmonyOS NEXT (API 12) 编写*
