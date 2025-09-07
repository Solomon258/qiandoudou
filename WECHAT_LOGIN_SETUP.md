# 微信小程序登录配置指南

## 概述

本项目已实现完整的微信小程序登录功能，支持真实的微信登录和演示模式。

## 配置步骤

### 1. 获取微信小程序配置

1. 登录 [微信公众平台](https://mp.weixin.qq.com/)
2. 进入小程序管理后台
3. 在"开发" -> "开发管理" -> "开发设置"中找到：
   - **AppID (小程序ID)**: 形如 `wx1234567890abcdef`
   - **AppSecret (小程序密钥)**: 形如 `abcdef1234567890abcdef1234567890`

### 2. 配置后端

#### 方法一：修改配置文件（推荐）

编辑 `qiandoudou-backend/src/main/resources/application.yml`：

```yaml
wechat:
  miniprogram:
    appid: 你的小程序AppID
    secret: 你的小程序AppSecret
```

#### 方法二：使用环境变量

设置环境变量：
- `WX_APPID`: 你的小程序AppID
- `WX_SECRET`: 你的小程序AppSecret

### 3. 配置前端

#### 修改小程序配置

编辑 `qiandoudou-frontend/project.config.json`：

```json
{
  "appid": "你的小程序AppID",
  // ... 其他配置
}
```

#### 配置服务器域名

在微信公众平台的"开发" -> "开发管理" -> "开发设置" -> "服务器域名"中添加：

**request合法域名**：
- `https://你的域名` (如果使用HTTPS)
- 开发阶段可以在微信开发者工具中勾选"不校验合法域名"

### 4. 测试登录功能

#### 演示模式
如果没有配置真实的AppID和AppSecret，系统会自动使用演示模式：
- 创建演示用户账号
- 生成有效的JWT token
- 可以正常使用应用功能

#### 真实模式
配置真实参数后：
- 调用微信官方API获取用户openid
- 自动创建或关联微信用户
- 支持真实的微信登录流程

## 登录流程

1. 用户点击"微信登录"按钮
2. 调用 `wx.login()` 获取临时授权码(code)
3. 发送code到后端API `/auth/wechat-login`
4. 后端调用微信API `https://api.weixin.qq.com/sns/jscode2session` 获取openid
5. 根据openid查找或创建用户
6. 生成JWT token返回给前端
7. 前端保存token和用户信息，跳转到首页

## 安全注意事项

1. **不要**将AppSecret提交到代码仓库
2. 生产环境使用环境变量配置敏感信息
3. 定期轮换AppSecret
4. 配置正确的服务器域名白名单

## 常见问题

### Q: 提示"微信配置未正确设置"
A: 检查application.yml中的appid和secret配置，确保不是默认值`your_wx_appid`

### Q: 登录时提示"code2session调用失败"
A: 检查网络连接和微信API配置，确保AppID和AppSecret正确

### Q: 开发阶段如何测试？
A: 可以暂时使用演示模式，或在微信开发者工具中配置测试号

## 部署注意事项

1. 确保后端服务器可以访问微信API (api.weixin.qq.com)
2. 配置HTTPS证书（生产环境必需）
3. 在微信公众平台配置正确的服务器域名
4. 设置适当的环境变量

## 支持

如有问题，请检查：
1. 微信开发者工具控制台日志
2. 后端服务器日志
3. 网络连接状态
4. 微信小程序配置是否正确
