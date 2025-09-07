# 阿里云OSS配置说明

## 概述
本项目已将文件上传功能从静态资源服务器迁移到阿里云OSS（对象存储服务）。

## 配置步骤

### 1. 获取阿里云OSS访问凭证
1. 登录阿里云控制台
2. 进入访问控制RAM页面
3. 创建用户并获取AccessKey ID和AccessKey Secret
4. 为用户授予OSS相关权限

### 2. 修改配置文件
在 `application.yml` 中修改以下配置：

```yaml
aliyun:
  oss:
    endpoint: https://oss-cn-guangzhou.aliyuncs.com
    access-key-id: 你的AccessKey_ID  # 替换为实际值
    access-key-secret: 你的AccessKey_Secret  # 替换为实际值
    bucket-name: qiandoudou
    base-url: https://qiandoudou.oss-cn-guangzhou.aliyuncs.com
    paths:
      audio: res/audio
      video: res/video
      image: res/image
```

### 3. 确保OSS Bucket配置
- Bucket名称: `qiandoudou`
- 区域: 华南1（广州）
- 读写权限: 公共读（用于前端访问文件）

### 4. 文件目录结构
上传的文件将按以下结构存储：
- 音频文件: `/res/audio/`
- 视频文件: `/res/video/`
- 图片文件: `/res/image/`

### 5. 访问URL格式
上传成功后，文件的访问URL格式为：
```
https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/{类型}/{文件名}
```

例如：
```
https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/audio/abc123def456.wav
```

## 代码变更说明

### 新增文件
- `OssService.java` - OSS服务接口
- `OssServiceImpl.java` - OSS服务实现类
- `OssProperties.java` - OSS配置属性类

### 修改文件
- `pom.xml` - 添加阿里云OSS SDK依赖
- `application.yml` - 添加OSS配置
- `TtsServiceImpl.java` - 修改为使用OSS服务

### 依赖变更
新增依赖：
```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>
```

## 注意事项
1. 请确保AccessKey ID和AccessKey Secret的安全性，不要提交到版本控制系统
2. 建议使用环境变量或配置中心来管理敏感配置
3. OSS的读写权限设置需要根据实际需求调整
4. 定期检查OSS的费用使用情况

## 测试验证
启动应用后，当AI伴侣发送动态时，生成的语音文件将自动上传到OSS，并返回OSS的访问URL。
