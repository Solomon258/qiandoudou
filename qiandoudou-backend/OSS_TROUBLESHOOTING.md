# OSS 上传问题排查指南

## 问题现象
```
图片上传失败: Error: 图片上传失败: 文件上传到OSS失败: null
```

### 6. 常见错误码对照

| 错误信息 | 可能原因 | 解决方案 |
|---------|---------|---------|
| `OSS客户端未初始化` | 配置错误 | 检查配置文件 |
| `AccessDenied` | 权限不足 | 检查 AccessKey 权限 |
| `NoSuchBucket` | Bucket 不存在 | 创建或检查 Bucket |
| `InvalidAccessKeyId` | AccessKey 错误 | 更新 AccessKey |
| `SignatureDoesNotMatch` | AccessSecret 错误 | 更新 AccessSecret |

### 7. 联系信息
如果问题仍未解决，请提供：
1. 后端启动完整日志
2. OSS 测试接口返回结果
3. 阿里云控制台 bucket 状态截图
