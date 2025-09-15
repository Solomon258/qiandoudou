# OSS配置指南

## 问题描述
微信头像上传失败，错误信息显示"文件上传失败"，这是由于OSS（对象存储服务）配置缺失导致的。

## 解决方案

### 1. 配置OSS环境变量（推荐）

在系统环境变量或启动脚本中设置以下变量：

```bash
# Windows 系统
set OSS_ACCESS_KEY_ID=你的AccessKey_ID
set OSS_ACCESS_KEY_SECRET=你的AccessKey_Secret  
set OSS_BUCKET_NAME=你的存储桶名称
set OSS_BASE_URL=https://你的存储桶名称.oss-cn-guangzhou.aliyuncs.com

# Linux/Mac 系统
export OSS_ACCESS_KEY_ID=你的AccessKey_ID
export OSS_ACCESS_KEY_SECRET=你的AccessKey_Secret
export OSS_BUCKET_NAME=你的存储桶名称
export OSS_BASE_URL=https://你的存储桶名称.oss-cn-guangzhou.aliyuncs.com
```

### 2. 直接修改配置文件

如果不想使用环境变量，可以直接在 `application.yml` 中替换默认值：

```yaml
aliyun:
  oss:
    endpoint: https://oss-cn-guangzhou.aliyuncs.com
    access-key-id: 你的AccessKey_ID
    access-key-secret: 你的AccessKey_Secret
    bucket-name: 你的存储桶名称
    base-url: https://你的存储桶名称.oss-cn-guangzhou.aliyuncs.com
```

### 3. 获取OSS配置信息

如果还没有阿里云OSS服务，请按以下步骤操作：

1. 登录阿里云控制台
2. 开通对象存储OSS服务
3. 创建存储桶（Bucket）
4. 获取AccessKey ID和AccessKey Secret
5. 配置CORS跨域访问规则

### 4. 配置CORS规则

在OSS控制台的存储桶设置中，添加以下CORS规则：

```json
[
  {
    "allowedOrigins": ["*"],
    "allowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "allowedHeaders": ["*"],
    "exposedHeaders": ["ETag", "x-oss-version-id"],
    "maxAgeSeconds": 3600
  }
]
```

### 5. 测试OSS连接

配置完成后，可以通过以下接口测试OSS连接状态：

```
GET /api/wallet/test-oss
```

如果返回成功信息，说明OSS配置正确。

## 注意事项

1. **安全性**：不要将AccessKey信息提交到代码仓库中
2. **权限**：确保AccessKey具有OSS读写权限
3. **地域**：确保endpoint和base-url的地域保持一致
4. **存储桶**：确保存储桶已创建且可访问

## 常见错误

1. **OSS客户端未初始化**：检查配置是否正确加载
2. **权限不足**：检查AccessKey权限
3. **网络连接**：检查网络连接和防火墙设置
4. **CORS错误**：检查跨域配置

配置完成后，重启应用即可正常使用微信头像上传功能。

