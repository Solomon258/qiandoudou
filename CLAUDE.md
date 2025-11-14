# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

## 项目概览

**钱兜兜** (QianDouDou) 是一款社交导向的储蓄应用，具有以下特性：
- AI 伴侣互动
- 个人和情侣储蓄钱包
- 基于梦想的储蓄目标（购物、旅行）
- 具有交互角色的伙伴系统
- 社交功能和用户互动
- 与 AI 参与的互动故事脚本

该项目是一个**单体仓库**，包含 Spring Boot 后端（`qiandoudou-backend/`）和微信小程序前端（`qiandoudou-frontend/`）。

## 技术栈

**后端：** Java 8, Spring Boot 2.7.18, MyBatis Plus 3.5.3.1, MySQL 8.0.33, JWT 认证

**前端：** 微信小程序（JavaScript ES6），Axios，WXML/WXSS

**外部服务：** 字节跳动/火山引擎 AI API，阿里云 OSS，微信小程序 API

## 项目结构

```
qiandoudou/
├── qiandoudou-backend/
│   ├── src/main/java/com/qiandoudou/
│   │   ├── controller/          (12 个 REST 控制器)
│   │   ├── service/             (25+ 个业务逻辑服务)
│   │   ├── entity/              (26 个数据模型)
│   │   ├── mapper/              (MyBatis Plus 数据访问)
│   │   ├── event/listener/      (事件驱动架构)
│   │   ├── config/              (Spring 配置)
│   │   └── util/                (JWT、HTTP 工具)
│   ├── src/main/resources/
│   │   ├── application.yml      (Spring Boot 配置 - 凭证)
│   │   ├── mapper/              (MyBatis XML 映射)
│   │   └── sql/                 (25+ 个数据库迁移脚本)
│   ├── pom.xml
│   ├── start.sh / stop.sh / status.sh
│   └── README_DEPLOY.md
│
├── qiandoudou-frontend/
│   ├── pages/                   (30+ 个微信小程序页面)
│   ├── components/              (可复用 UI 组件)
│   ├── utils/api.js             (集中式 HTTP 客户端)
│   ├── app.js                   (全局状态和事件总线)
│   ├── app.json                 (小程序清单)
│   ├── project.config.json      (微信开发者工具配置)
│   └── package.json
│
└── [根目录 package.json 和 git 配置]
```

## 后端架构

**分层架构：**
- **控制器层** → REST 端点（12 个控制器）
- **服务层** → 业务逻辑（25+ 个服务）
- **映射层** → MyBatis Plus 数据访问
- **数据库** → MySQL 支持逻辑删除

**关键设计模式：**
- 基于服务的架构，关注点分离清晰
- 通过自定义 Event/Listener 包实现事件驱动设计
- JWT 无状态认证（24 小时过期，HS256 算法）
- 通过 `@EnableAsync` 启用异步处理
- 通过 `@EnableScheduling` 实现定时任务

**安全性：**
- Authorization header 中的 JWT Bearer 令牌
- Spring Security 集成
- 逻辑删除（软删除）保护数据
- 通过 WebConfig 配置 CORS

**数据库：** MySQL 8.0.33 在 `8.148.206.18:3306`
- 连接通过 `application.yml` 管理（凭证不在仓库中）
- `src/main/resources/sql/` 中有 25+ 个迁移脚本
- MyBatis Plus 配置中启用了软删除模式

## 前端架构

**页面结构：**
每个页面是微信小程序组件，包含 4 个文件：
```
pages/[page-name]/
├── [page-name].js      (逻辑和生命周期)
├── [page-name].wxml    (WXML 标记)
├── [page-name].wxss    (样式)
└── [page-name].json    (页面配置)
```

**全局状态管理：**
- `app.js` 中的简单事件总线模式
- `App()` 实例中的全局数据存储
- 存储 API 用于持久化
- `utils/api.js` 中的集中式 API 客户端

**API 通信：**
- 所有 HTTP 请求通过集中式 `utils/api.js` 包装器
- 自动注入 JWT 令牌
- 401 错误触发重定向到登录
- 围绕 `wx.request()` 的基于 Promise 的接口

**基础 URL：**
- 开发环境：`http://localhost:8080/api`
- 生产环境：`https://heartllo.cn/api`

## 核心功能模块

**后端控制器：** 认证、用户、钱包、梦想钱包、AI、伙伴、社交、脚本、分享图片、登录日志、测试控制器

**关键服务：**
- **钱包：** WalletService、TransactionService、DreamWalletService、DreamProgressService
- **AI：** AiService、AiPartnerService、AiAutoCommentService、AiLoverInteractionService、ImageToTextService
- **伙伴：** BuddyService、BuddyCharacterService、BuddyWalletInitService、BuddyAutoCommentService
- **社交：** SocialService、UserFollowService、BuddyCircleService
- **工具：** AudioDurationService、ShareImageService、DreamImageConfigService、CharacterVoiceMappingService

**前端页面：** 30+ 个页面，覆盖：
- 认证（登录）
- 钱包管理（创建、转账、详情、消息）
- 梦想系统（类型选择、创建、进度、奖励、分享）
- 伙伴系统（模式选择、角色选择、情侣储蓄）
- AI 功能（伴侣、聊天）
- 社交功能（信息流、个人资料、脚本、分享）

## 构建和运行命令

### 后端

```bash
# 构建
cd qiandoudou-backend
mvn clean package -DskipTests

# 运行
./start.sh                              # 通过部署脚本
# 或
java -jar target/qiandoudou-backend-1.0.0.jar

# 管理
./status.sh                             # 检查运行状态
./stop.sh                               # 停止应用
```

**访问地址：** http://localhost:8080/api（开发环境）或 http://8.148.206.18:8080/api（生产环境）

### 前端

```bash
# 安装依赖
npm install

# 代码检查
npm run lint

# 开发/测试
# 在微信开发者工具中打开以进行开发和测试
# (无内置开发服务器 - 需要微信开发者工具)
```

**访问地址：** 微信小程序 ID `wx8372421078267cd9` 或通过微信开发者工具模拟器

## 配置文件

**后端：**
- `src/main/resources/application.yml` - Spring Boot 配置（JWT 密钥、数据库凭证、API 密钥、阿里云 OSS、字节跳动 AI 配置）
  - 文件在 `.gitignore` 中以保护凭证
  - 关键配置：端口 8080、上下文路径 `/api`、MySQL 主机、JWT 24 小时过期、AI API 密钥

**前端：**
- `project.config.json` - 微信开发者工具设置，包含 ES6、PostCSS、压缩、懒加载代码
- `app.json` - 小程序清单，包含所有 30+ 个页面路由
- `package.json` - NPM 配置，包含 axios 和 js-cookie 依赖

**数据库：**
- 主机：`8.148.206.18:3306`，数据库：`qiandoudou`
- 时区：Asia/Shanghai
- 字符编码：UTF-8

## 常见开发任务

### 添加后端新功能

1. **创建实体** 在 `src/main/java/com/qiandoudou/entity/`
2. **创建映射器** 在 `src/main/java/com/qiandoudou/mapper/`（MyBatis Plus 接口扩展 BaseMapper）
3. **创建服务接口** 在 `src/main/java/com/qiandoudou/service/`
4. **创建服务实现** 在 `src/main/java/com/qiandoudou/service/impl/`
5. **创建 REST 控制器** 在 `src/main/java/com/qiandoudou/controller/`，使用 `@RestController` 和 `@RequestMapping`
6. **数据库迁移** - 如需要，在 `src/main/resources/sql/` 中添加 SQL 脚本
7. **测试端点** 通过 HTTP 客户端或 Postman

**服务层模式：** 服务使用依赖注入，业务逻辑写在这里，不在控制器中。

### 添加前端新页面

1. **创建页面目录** 在 `pages/[page-name]/`
2. **创建 4 个文件：**
   - `[page-name].js` - 实现 `Page()` 及其生命周期方法
   - `[page-name].wxml` - WXML 标记
   - `[page-name].wxss` - 样式
   - `[page-name].json` - 页面配置
3. **在 `app.json` 中注册** - 在 pages 数组中添加页面路由
4. **使用 API 客户端** 来自 `utils/api.js` 进行 HTTP 请求
5. **发出事件** - 如需要全局通信，使用事件总线模式

**页面生命周期：** `onLoad()`、`onShow()`、`onHide()`、`onUnload()`。通过 `setData()` 绑定数据。

### 数据库变更

- 在 `src/main/resources/sql/` 中添加迁移 SQL 脚本
- 遵循命名约定：`V[版本]__[描述].sql`
- 软删除已启用 - 在 WHERE 子句中对活跃记录使用 `deleted = 0`
- 通过 MyBatis Plus 或 Spring Flyway 集成应用

### API 认证

- 后端：JWT 密钥在 `application.yml` 中，令牌在 24 小时后过期
- 前端：令牌通过 `utils/api.js` 自动注入请求中
- 无效令牌触发 401 响应 → 前端重定向到登录

### 文件存储

**本地：** `qiandoudou-backend/uploads/` 目录

**云端：** 阿里云 OSS 桶 `qiandoudou` 在广州地区
- 路径：`res/audio`、`res/video`、`res/image`
- 基础 URL：`https://qiandoudou.oss-cn-guangzhou.aliyuncs.com`

### 外部服务集成

**字节跳动/火山引擎 AI API：**
- 模型：`doubao-seed-1-6-vision-250815`
- 在 `application.yml` 中的 `ai.bytedance` 下配置
- 由 AiService 及相关类使用

**字节跳动 TTS：**
- 应用 ID 和访问令牌在 `application.yml` 的 `ai.bytedance.tts` 下

**OpenAI API：**
- 作为备用服务可用（未主动配置）

## 当前开发状态

**当前分支：** `demo`（开发）
**主分支：** `master`（生产）

**最近变更（demo 分支上未提交）：**
- 梦想详情页逻辑和 UI
- 首页布局和逻辑改进
- 钱包详情页增强
- API 客户端工具

**需要审查的修改文件：**
- `qiandoudou-frontend/pages/dream-detail/dream-detail.js`
- `qiandoudou-frontend/pages/home/home.js`
- `qiandoudou-frontend/pages/home/home.wxml`
- `qiandoudou-frontend/pages/wallet-detail/wallet-detail.js`
- `qiandoudou-frontend/pages/wallet-detail/wallet-detail.wxml`
- `qiandoudou-frontend/pages/wallet-detail/wallet-detail.wxss`
- `qiandoudou-frontend/utils/api.js`

## 关键开发说明

### 后端开发

- **无可见的测试套件** - 测试在 Maven 构建中被跳过（`-DskipTests`）
- **事件系统** - 使用事件和监听器进行解耦的功能集成
- **异步/定时任务** - 已启用，使用 `@Async` 和 `@Scheduled` 装饰器
- **软删除** - 始终在查询中检查 `deleted = 0`；MyBatis Plus 通过全局配置处理
- **实体 ID** - 通过 MyBatis Plus 使用分布式 ID 策略（ASSIGN_ID）

### 前端开发

- **无热重载** - `compileHotReLoad` 已禁用；在微信开发者工具中使用实时刷新
- **懒加载** - 通过 `lazyCodeLoading: "requiredComponents"` 启用以提高性能
- **微信 API 访问** - 直接使用 `wx.*` API（wx.request、wx.getStorageSync 等）
- **数据绑定** - 模板中的双向绑定；通过 `setData()` 更新
- **组件预加载** - 如需要，在页面 JSON 中配置

### 部署

- **后端：** 使用 `start.sh`、`stop.sh`、`status.sh` 脚本进行生命周期管理
- **前端：** 通过微信开发者工具打包，上传到生产环境
- **特定环境 URL：** 前端根据环境在 localhost:8080 和生产之间自动切换

## API 端点结构

所有端点都在 `/api` 上下文路径下：

```
认证：                POST   /auth/login、/auth/register、/auth/wechat-login
用户管理：            GET/PUT /user/*
钱包操作：            GET/POST /wallet/*、/wallet/transfer-in、/wallet/transfer-out
梦想系统：            GET/POST /dream-wallet/*、/dream-wallet/items、/dream-wallet/create
伙伴系统：            GET/POST /buddy/*、/buddy-character/*、/buddy-interaction/*
社交功能：            GET/POST /social/*、/user-follow/*、/buddy-circle/*
AI 功能：             POST /ai/*、/ai-partner/*、/ai-chat/*
脚本内容：            GET /script/*、/script-chapter/*
分享：                POST /share-image/*、GET /badge-share/*、/car-share/*
管理/测试：           GET/POST /test/*（测试工具）
```

**认证：** Authorization header 中的 Bearer 令牌（`Authorization: Bearer [token]`），24 小时过期

## 性能考虑

- 启用懒加载代码（仅加载必需组件）
- MyBatis Plus 缓存和查询优化
- 长时间运行操作的异步任务（AI 处理、文件上传）
- 软删除而非硬删除以保证数据完整性
- 云存储（阿里云 OSS）用于大文件而非服务器存储

## Git 工作流

- 在 `demo` 分支上开发
- 合并到 `master` 以进行生产发布
- 提交信息使用中文（遵循现有约定）
- `application.yml` 被排除在仓库外（凭证受保护）

## 微信小程序开发约束

**前端开发必须完全遵循微信官方文档：**
- 所有网络请求必须使用 `wx.request()` API
- 必须通过微信官方的权限系统（如相机、位置、联系人）
- 所有 API 调用必须经过微信安全验证（HTTPS + 白名单）
- 小程序加密数据解密使用官方提供的算法
- 遵守微信的隐私政策和内容审核规范

**开发工作流：**
- 前端代码编译和清空缓存由你手动执行
- 后端代码编译和启动由你在 IntelliJ IDEA 中手动执行
- SQL 脚本由你自己去数据库执行
- 所有 git 操作由你亲自执行，我只提供操作步骤指导
