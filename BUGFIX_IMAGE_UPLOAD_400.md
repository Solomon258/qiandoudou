# Bug 修复：图片上传返回400错误

## 问题描述
图片上传接口报 HTTP 400 错误。

**错误日志：**
```
Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'
For input string: "[objectNull]"
```

## 根本原因分析

### 后端期望
```java
@PostMapping("/upload-user-image")
public Result<Map<String, String>> uploadUserImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "type", required = false, defaultValue = "general") String type,
        @RequestParam(value = "userId", required = false) Long userId)  // 期望 Long 类型
```

### 前端问题
前端在调用上传函数时，**没有传递 userId 参数**，导致：
1. 前端：`userId = null`
2. FormData 中：`userId = "[objectNull]"`（JavaScript 自动转换）
3. 后端：无法将 `"[objectNull]"` 字符串转换为 Long 类型 → 400 错误

## 问题代码位置

### ❌ 错误的调用方式

**transfer-in.js (第260行)：**
```javascript
uploadUserImage(tempFilePath, 'transfer_in')  // 缺少 userId
```

**transfer-out.js (第354行)：**
```javascript
uploadUserImage(tempFilePath, 'transfer_out')  // 缺少 userId
```

**login.js (第563行)：**
```javascript
uploadUserImage(downloadRes.tempFilePath, 'avatar')  // 缺少 userId
```

### ✅ 正确的调用方式

```javascript
uploadUserImage(tempFilePath, 'transfer_in', userId)
uploadUserImage(tempFilePath, 'transfer_out', userId)
uploadUserImage(downloadRes.tempFilePath, 'avatar', userId)
```

## 修复方案

### 1. transfer-in.js (第260行)
```javascript
// 原代码
const { uploadUserImage } = require('../../utils/api.js')
uploadUserImage(tempFilePath, 'transfer_in')

// 修复后
const { uploadUserImage } = require('../../utils/api.js')
const userId = app.globalData.userInfo?.id
uploadUserImage(tempFilePath, 'transfer_in', userId)
```

### 2. transfer-out.js (第354行)
```javascript
// 原代码
const { uploadUserImage } = require('../../utils/api.js')
uploadUserImage(tempFilePath, 'transfer_out')

// 修复后
const { uploadUserImage } = require('../../utils/api.js')
const userId = app.globalData.userInfo?.id
uploadUserImage(tempFilePath, 'transfer_out', userId)
```

### 3. login.js (第563行)
```javascript
// 原代码
uploadUserImage(downloadRes.tempFilePath, 'avatar')

// 修复后（uploadWechatProfile 方法已有 userId 参数）
uploadUserImage(downloadRes.tempFilePath, 'avatar', userId)
```

## 修改文件清单

- ✅ `qiandoudou-frontend/pages/transfer-in/transfer-in.js`
- ✅ `qiandoudou-frontend/pages/transfer-out/transfer-out.js`
- ✅ `qiandoudou-frontend/pages/login/login.js`

## 部署步骤

1. **清除微信小程序缓存**
   - 微信开发者工具 → 清空缓存 → 清空所有缓存

2. **重新编译小程序**
   - 保存文件后自动编译或按 Ctrl+Shift+P 重新编译

3. **测试上传功能**
   - 测试转入图片上传
   - 测试转出图片上传
   - 测试微信登录头像上传

## 测试方法

### 转入上传测试
```
1. 进入转入页面
2. 点击选择图片
3. 选择一张图片
4. 确认上传成功（显示"图片上传成功"提示）
5. 检查后端日志：no errors
```

### 转出上传测试
```
1. 进入转出页面
2. 点击选择图片
3. 选择一张图片
4. 确认上传成功
```

### 微信登录头像上传测试
```
1. 使用微信登录
2. 授权获取头像和昵称
3. 等待头像上传完成
4. 检查最终显示的头像是否正确
```

## 性能影响
- ✅ 无性能影响
- ✅ 无数据库影响
- ✅ 完全向后兼容

## 相关知识点

### 为什么需要 userId？
后端使用 userId 来生成唯一的文件名：
```java
String filename = userId + "_" + System.currentTimeMillis() + "." + extension;
```

这样可以：
1. 避免不同用户的文件名冲突
2. 方便后续追踪文件来源
3. 支持用户隐私保护

### 函数签名
```javascript
function uploadUserImage(filePath, type = 'general', userId = null) {
  return uploadFile(filePath, '/wallet/upload-user-image', { type, userId })
}
```

参数说明：
- `filePath`：本地图片路径
- `type`：图片类型（'avatar', 'transfer_in', 'transfer_out', 'general'）
- `userId`：用户ID（必需，用于生成唯一文件名）

## 日期
修复时间: 2025-11-11
