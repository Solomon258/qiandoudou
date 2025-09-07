# 微信登录功能测试指南

## 测试准备

### 1. 数据库准备
执行SQL脚本确保数据库表结构正确：
```bash
mysql -u root -p qiandoudou < wechat_login_setup.sql
```

### 2. 后端服务启动
```bash
cd qiandoudou-backend
# 使用你的IDE启动Spring Boot应用，或使用命令行：
mvn spring-boot:run
```

### 3. 前端准备
在微信开发者工具中：
1. 打开 `qiandoudou-frontend` 项目
2. 确保项目配置正确
3. 在设置中勾选"不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书"

## 测试步骤

### 测试1：后端API测试
```bash
# 运行API测试脚本
node test-wechat-login.js
```

预期结果：
- ✅ 状态码 200
- ✅ 返回包含token和user信息的JSON
- ✅ 控制台显示"微信登录API测试成功"

### 测试2：前端登录流程测试

1. **打开登录页面**
   - 在微信开发者工具中打开小程序
   - 应该显示登录页面（如果未登录）

2. **点击微信登录按钮**
   - 点击"微信登录"按钮
   - 观察控制台日志

3. **检查登录结果**
   - 应该显示"微信登录成功"提示
   - 自动跳转到首页
   - 用户信息应该显示在页面上

### 测试3：演示模式测试

如果没有配置真实的微信AppID和Secret：

1. **检查后端日志**
   ```
   微信配置未正确设置，使用演示模式
   使用演示模式处理微信登录
   创建演示用户: [用户ID]
   ```

2. **验证演示用户创建**
   ```sql
   SELECT * FROM users WHERE openid LIKE 'demo_wx_%' ORDER BY create_time DESC LIMIT 5;
   ```

### 测试4：真实微信登录测试

配置真实AppID和Secret后：

1. **检查后端日志**
   ```
   调用微信API: https://api.weixin.qq.com/sns/jscode2session?appid=***
   微信API响应: {"openid":"...","session_key":"..."}
   获取到用户openid: ox1234567890abcdef
   ```

2. **验证真实用户创建**
   ```sql
   SELECT * FROM users WHERE openid NOT LIKE 'demo_wx_%' ORDER BY create_time DESC LIMIT 5;
   ```

## 常见问题排查

### 问题1：后端API无法访问
**症状**: 前端显示"网络连接失败"
**排查**:
```bash
# 检查后端服务是否启动
curl http://localhost:8080/api/auth/wechat-login -X POST -H "Content-Type: application/json" -d '{"code":"test"}'
```

### 问题2：微信API调用失败
**症状**: 后端日志显示"微信登录失败"
**排查**:
1. 检查AppID和AppSecret配置
2. 检查网络连接到微信服务器
3. 验证code是否有效（微信的code只能使用一次）

### 问题3：JWT生成失败
**症状**: 后端日志显示JWT相关错误
**排查**:
1. 检查JWT配置在application.yml中是否正确
2. 确保JwtUtil类存在并正确配置

### 问题4：用户信息未保存
**症状**: 登录成功但用户信息丢失
**排查**:
1. 检查数据库连接
2. 验证users表结构
3. 查看MyBatis Plus配置

## 日志监控

### 前端日志（微信开发者工具控制台）
```
开始微信登录流程
wx.login 成功，获取到code: 081234567890abcdef
发送code到后端进行验证
微信登录成功，后端返回: {code: 200, data: {...}}
登录信息已保存: 微信用户(演示)
```

### 后端日志
```
开始微信登录，code: 081234567890abcdef
微信配置未正确设置，使用演示模式
使用演示模式处理微信登录
创建演示用户: 1234567890123456789
生成token成功
```

## 性能测试

### 并发登录测试
```bash
# 创建简单的并发测试脚本
for i in {1..10}; do
  node test-wechat-login.js &
done
wait
```

### 登录响应时间
- 演示模式: < 100ms
- 真实模式: < 500ms (取决于微信API响应时间)

## 安全检查

1. **检查敏感信息**
   - AppSecret不应出现在日志中
   - Token应该正确生成和验证

2. **检查SQL注入防护**
   - 使用MyBatis Plus的参数化查询

3. **检查XSS防护**
   - 用户输入应该正确转义

## 测试完成确认

- [ ] 后端API测试通过
- [ ] 前端登录流程正常
- [ ] 演示模式工作正常
- [ ] 数据库记录正确创建
- [ ] 用户会话管理正常
- [ ] 日志输出正确
- [ ] 错误处理适当

完成所有测试后，微信登录功能即可投入使用。
