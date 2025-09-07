# 用户图片目录更新说明

## 修改概述

已将所有用户上传的图片统一存储到 OSS 的 `/res/image/user_images/` 目录下。

## 修改内容

### 1. OSS 服务接口扩展 (`OssService.java`)

新增了专门用于用户图片上传的方法：

```java
/**
 * 上传用户图片文件到OSS用户专用目录
 * @param imageFile 图片文件
 * @param fileName 文件名
 * @return OSS中的文件URL
 */
String uploadUserImageFile(File imageFile, String fileName);

/**
 * 上传用户图片字节数组到OSS用户专用目录
 * @param fileData 图片文件数据
 * @param fileName 文件名
 * @return OSS中的文件URL
 */
String uploadUserImageData(byte[] fileData, String fileName);
```

### 2. OSS 服务实现类更新 (`OssServiceImpl.java`)

#### A. 路径映射扩展

在 `getUploadPath()` 方法中新增了 `user_image` 类型：

```java
case "user_image":
    // 用户上传的图片专用目录
    return "res/image/user_images";
```

#### B. 新方法实现

实现了新的用户图片上传方法：

```java
@Override
public String uploadUserImageFile(File imageFile, String fileName) {
    try {
        byte[] fileData = Files.readAllBytes(imageFile.toPath());
        return uploadFile(fileData, fileName, "user_image");
    } catch (IOException e) {
        logger.error("读取用户图片文件失败: {}", e.getMessage(), e);
        throw new RuntimeException("读取用户图片文件失败: " + e.getMessage(), e);
    }
}

@Override
public String uploadUserImageData(byte[] fileData, String fileName) {
    return uploadFile(fileData, fileName, "user_image");
}
```

### 3. 钱包控制器更新 (`WalletController.java`)

#### A. 背景图片上传

钱包背景图片上传现在使用专用的用户图片目录：

```java
// 上传到OSS用户图片目录
String ossUrl = ossService.uploadUserImageData(file.getBytes(), filename);
```

#### B. 测试接口更新

OSS 连接测试接口也使用用户图片目录进行测试：

```java
// 尝试上传一个小的测试文件到用户图片目录
byte[] testData = "test".getBytes();
String testUrl = ossService.uploadFile(testData, "test.txt", "user_image");
```

## 目录结构

### 修改前
```
OSS Bucket: qiandoudou
├── res/
│   ├── audio/          # 音频文件
│   ├── video/          # 视频文件
│   └── image/          # 所有图片文件（混合）
```

### 修改后
```
OSS Bucket: qiandoudou
├── res/
│   ├── audio/          # 音频文件
│   ├── video/          # 视频文件
│   └── image/
│       ├── user_images/    # 用户上传的图片（新增）
│       └── (其他系统图片)   # 系统图片保持原位置
```

## 文件命名规则

用户上传的钱包背景图片文件名格式：
```
wallet_bg_{walletId}_{timestamp}.{extension}
```

例如：
```
wallet_bg_1_1703123456789.jpg
```

## 完整的文件访问URL示例

```
https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/user_images/wallet_bg_1_1703123456789.jpg
```

## 向后兼容性

- ✅ **现有系统图片**：继续使用 `res/image/` 目录
- ✅ **现有API**：`uploadImageFile()` 方法保持不变
- ✅ **新功能**：用户图片使用专用目录和新方法

## 优势

1. **组织性**：用户上传的图片统一管理
2. **安全性**：用户图片与系统图片分离
3. **维护性**：便于批量操作用户图片
4. **扩展性**：为将来的用户图片管理功能打基础
5. **兼容性**：不影响现有功能

## 测试验证

1. **编译测试**：✅ 所有代码编译通过
2. **功能测试**：使用 `test-oss.html` 测试页面验证上传功能
3. **目录验证**：检查 OSS 中的文件是否正确存储到 `res/image/user_images/` 目录

## 后续建议

1. **监控日志**：观察用户图片上传的日志，确保正确存储
2. **清理机制**：考虑实现定期清理未使用图片的功能
3. **权限管理**：可以为用户图片目录设置特定的访问权限
4. **统计功能**：可以统计用户图片的使用情况
