# 钱兜兜 - 智能理财助手

一个集成AI伴侣、剧本攒钱和社交分享功能的创新理财应用。

## 项目特色

### 🎯 核心功能
- **智能钱包管理** - 支持个人钱包和情侣钱包两种模式
- **AI伴侣系统** - 个性化AI男友/女友，提供情感支持和理财建议
- **剧本攒钱** - 通过互动式剧本体验，让储蓄变得有趣
- **社交圈分享** - 分享理财心得，与好友互动交流

### 🤖 AI驱动
- AI生成剧本内容和图片
- AI伴侣智能评论和语音互动
- 图片识别自动生成文字描述
- 个性化理财建议

### 📱 移动优先
- 响应式设计，完美适配移动设备
- 类原生App体验
- 流畅的动画和交互效果

## 技术架构

### 后端技术栈
- **Spring Boot 2.7.x** - 主框架
- **MyBatis Plus** - 数据访问层
- **MySQL 8.0** - 数据库
- **Spring Security + JWT** - 安全认证
- **Redis** - 缓存
- **Maven** - 构建工具

### 前端技术栈
- **Vue 3** - 前端框架
- **Element Plus** - UI组件库
- **Vue Router** - 路由管理
- **Vuex** - 状态管理
- **Axios** - HTTP客户端

## 快速开始

### 环境要求
- JDK 1.8+
- MySQL 8.0
- Node.js 16+
- Maven 3.6+

### 后端启动

1. **创建数据库**
```sql
-- 执行 qiandoudou-backend/src/main/resources/sql/init.sql
-- 执行 qiandoudou-backend/src/main/resources/sql/sample_data.sql
```

2. **配置数据库连接**
```yaml
# 修改 qiandoudou-backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qiandoudou?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: your_parootssword
```

3. **启动后端服务**
```bash
cd qiandoudou-backend
mvn spring-boot:run
```

服务将在 http://localhost:8080 启动

### 前端启动

1. **安装依赖**
```bash
cd qiandoudou-frontend
npm install
```

2. **启动开发服务器**
```bash
npm run serve
```

应用将在 http://localhost:3000 启动

### 演示账号
- 用户名: demo_user
- 密码: 123456

或点击"演示登录"按钮直接体验

## 项目结构

```
qiandoudou/
├── qiandoudou-backend/          # 后端项目
│   ├── src/main/java/
│   │   └── com/qiandoudou/
│   │       ├── controller/      # 控制器层
│   │       ├── service/         # 服务层
│   │       ├── mapper/          # 数据访问层
│   │       ├── entity/          # 实体类
│   │       ├── config/          # 配置类
│   │       └── util/            # 工具类
│   ├── src/main/resources/
│   │   ├── sql/                 # 数据库脚本
│   │   └── application.yml      # 配置文件
│   └── pom.xml                  # Maven配置
│
├── qiandoudou-frontend/         # 前端项目
│   ├── src/
│   │   ├── views/               # 页面组件
│   │   ├── components/          # 公共组件
│   │   ├── store/               # Vuex状态管理
│   │   ├── router/              # 路由配置
│   │   ├── utils/               # 工具函数
│   │   └── assets/              # 静态资源
│   ├── public/                  # 公共文件
│   ├── package.json             # 项目配置
│   └── vue.config.js            # Vue CLI配置
│
└── README.md                    # 项目说明
```

## 功能展示

### 登录注册
- 支持用户名密码登录
- 新用户注册功能
- 演示账号快速体验

### 钱包管理
- 创建个人钱包和情侣钱包
- 资金转入转出操作
- 交易记录查看
- 多种背景主题选择

### AI伴侣
- 选择不同性格的AI伴侣
- 智能评论和点赞
- 语音互动（预留接口）
- 个性化理财建议

### 剧本攒钱
- 互动式剧本体验（开发中）
- 多结局选择系统
- AI生成内容和图片
- 储蓄目标达成奖励

### 社交圈
- 发布理财动态
- 图片分享功能
- 点赞评论互动
- 关注好友动态

## 开发计划

### Phase 1 ✅ 已完成
- [x] 基础框架搭建
- [x] 用户认证系统
- [x] 钱包管理功能
- [x] 社交圈基础功能
- [x] 移动端适配

### Phase 2 🚧 开发中
- [ ] 剧本攒钱完整实现
- [ ] AI接口集成
- [ ] 图片上传功能
- [ ] 语音功能实现
- [ ] 消息通知系统

### Phase 3 📋 计划中
- [ ] 数据统计分析
- [ ] 个人理财报告
- [ ] 更多AI伴侣角色
- [ ] 小程序版本
- [ ] 性能优化

## API接口

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/demo-login` - 演示登录

### 钱包接口
- `GET /api/wallet/list` - 获取钱包列表
- `POST /api/wallet/create` - 创建钱包
- `POST /api/wallet/transfer-in` - 转入资金
- `POST /api/wallet/transfer-out` - 转出资金
- `GET /api/wallet/transactions` - 获取交易记录

### AI接口（预留）
- `POST /api/ai/generate-script` - 生成剧本内容
- `POST /api/ai/generate-image` - 生成图片
- `POST /api/ai/generate-comment` - 生成AI评论
- `POST /api/ai/text-from-image` - 图片识别

## 贡献指南

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

如有问题或建议，请通过以下方式联系：

- 项目Issues: [GitHub Issues](https://github.com/your-repo/qiandoudou/issues)
- 邮箱: your-email@example.com

---

**钱兜兜** - 让理财变得更有趣！ 💰✨
