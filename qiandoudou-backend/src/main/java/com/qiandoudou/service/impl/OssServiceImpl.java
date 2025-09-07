package com.qiandoudou.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.qiandoudou.config.OssProperties;
import com.qiandoudou.service.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

/**
 * 阿里云OSS文件上传服务实现类
 */
@Service
public class OssServiceImpl implements OssService {

    private static final Logger logger = LoggerFactory.getLogger(OssServiceImpl.class);

    @Autowired
    private OssProperties ossProperties;

    private OSS ossClient;

    @PostConstruct
    public void initOssClient() {
        try {
            logger.info("=== 开始初始化OSS客户端 ===");
            logger.info("OSS配置检查:");
            logger.info("  endpoint: {}", ossProperties.getEndpoint());
            logger.info("  accessKeyId: {}", ossProperties.getAccessKeyId() != null ?
                    ossProperties.getAccessKeyId().substring(0, Math.min(8, ossProperties.getAccessKeyId().length())) + "***" : "null");
            logger.info("  accessKeySecret: {}", ossProperties.getAccessKeySecret() != null ? "***" : "null");
            logger.info("  bucketName: {}", ossProperties.getBucketName());
            logger.info("  baseUrl: {}", ossProperties.getBaseUrl());

            // 检查必要的配置项
            if (ossProperties.getEndpoint() == null || ossProperties.getEndpoint().trim().isEmpty()) {
                throw new RuntimeException("OSS endpoint 配置为空");
            }
            if (ossProperties.getAccessKeyId() == null || ossProperties.getAccessKeyId().trim().isEmpty()) {
                throw new RuntimeException("OSS accessKeyId 配置为空");
            }
            if (ossProperties.getAccessKeySecret() == null || ossProperties.getAccessKeySecret().trim().isEmpty()) {
                throw new RuntimeException("OSS accessKeySecret 配置为空");
            }
            if (ossProperties.getBucketName() == null || ossProperties.getBucketName().trim().isEmpty()) {
                throw new RuntimeException("OSS bucketName 配置为空");
            }

            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );

            logger.info("OSS客户端初始化成功！");
            logger.info("=== OSS客户端初始化完成 ===");
        } catch (Exception e) {
            logger.error("OSS客户端初始化失败: {}", e.getMessage(), e);
            // 不抛出异常，让应用能正常启动，但OSS功能不可用
            logger.warn("OSS服务将不可用，请检查配置后重启应用");
        }
    }

    @PreDestroy
    public void destroyOssClient() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
                logger.info("OSS客户端已关闭");
            } catch (Exception e) {
                logger.warn("关闭OSS客户端时出错: {}", e.getMessage());
            }
        }
    }

    @Override
    public String uploadAudioFile(File audioFile, String fileName) {
        try {
            byte[] fileData = Files.readAllBytes(audioFile.toPath());
            return uploadFile(fileData, fileName, "audio");
        } catch (IOException e) {
            logger.error("读取音频文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取音频文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadVideoFile(File videoFile, String fileName) {
        try {
            byte[] fileData = Files.readAllBytes(videoFile.toPath());
            return uploadFile(fileData, fileName, "video");
        } catch (IOException e) {
            logger.error("读取视频文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取视频文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadImageFile(File imageFile, String fileName) {
        try {
            byte[] fileData = Files.readAllBytes(imageFile.toPath());
            return uploadFile(fileData, fileName, "image");
        } catch (IOException e) {
            logger.error("读取图片文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取图片文件失败: " + e.getMessage(), e);
        }
    }

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

    @Override
    public String uploadFile(byte[] fileData, String fileName, String fileType) {
        // 详细的前置检查和日志
        logger.info("=== OSS上传开始 ===");
        logger.info("OSS客户端状态: {}", ossClient != null ? "已初始化" : "未初始化");
        logger.info("OSS配置信息: endpoint={}, bucket={}, baseUrl={}",
                ossProperties.getEndpoint(), ossProperties.getBucketName(), ossProperties.getBaseUrl());

        if (ossClient == null) {
            logger.error("OSS客户端未初始化，无法上传文件");
            throw new RuntimeException("OSS客户端未初始化");
        }

        if (fileData == null || fileData.length == 0) {
            logger.error("文件数据为空，无法上传");
            throw new RuntimeException("文件数据为空");
        }

        try {
            logger.info("开始上传文件到OSS，文件名: {}, 类型: {}, 数据大小: {} bytes",
                    fileName, fileType, fileData.length);

            // 根据文件类型确定上传路径
            String uploadPath = getUploadPath(fileType);
            logger.info("上传路径: {}", uploadPath);

            // 生成唯一文件名
            String uniqueFileName = generateUniqueFileName(fileName);
            String objectKey = uploadPath + "/" + uniqueFileName;
            logger.info("对象键: {}", objectKey);

            // 创建元数据对象
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileData.length);

            // 设置Content-Type，确保文件能正确播放/显示
            if ("audio".equals(fileType)) {
                if (fileName.toLowerCase().endsWith(".wav")) {
                    metadata.setContentType("audio/wav");
                } else if (fileName.toLowerCase().endsWith(".mp3")) {
                    metadata.setContentType("audio/mpeg");
                } else {
                    metadata.setContentType("audio/wav");
                }
            } else if ("video".equals(fileType)) {
                metadata.setContentType("video/mp4");
            } else if ("image".equals(fileType) || "user_image".equals(fileType)) {
                if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                    metadata.setContentType("image/jpeg");
                } else if (fileName.toLowerCase().endsWith(".png")) {
                    metadata.setContentType("image/png");
                } else {
                    metadata.setContentType("image/jpeg");
                }
            }

            // 创建上传请求
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(), objectKey, inputStream, metadata);

            // 执行上传
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            logger.info("文件上传到OSS成功，ETag: {}, ObjectKey: {}", result.getETag(), objectKey);

            // 构建文件访问URL
            String fileUrl = ossProperties.getBaseUrl() + "/" + objectKey;
            logger.info("文件访问URL: {}", fileUrl);

            return fileUrl;

        } catch (Exception e) {
            logger.error("文件上传到OSS失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传到OSS失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String objectKey) {
        if (ossClient == null) {
            throw new RuntimeException("OSS客户端未初始化");
        }

        try {
            ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
            logger.info("文件删除成功，ObjectKey: {}", objectKey);
            return true;
        } catch (Exception e) {
            logger.error("文件删除失败，ObjectKey: {}, 错误: {}", objectKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据文件类型获取上传路径
     */
    private String getUploadPath(String fileType) {
        switch (fileType.toLowerCase()) {
            case "audio":
                return ossProperties.getPaths().getAudio();
            case "video":
                return ossProperties.getPaths().getVideo();
            case "image":
                return ossProperties.getPaths().getImage();
            case "user_image":
                // 用户上传的图片专用目录
                return "res/image/user_images";
            default:
                logger.warn("未知文件类型: {}, 使用默认路径", fileType);
                return "res/other";
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }

        // 获取文件扩展名
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
            extension = originalFileName.substring(lastDotIndex);
        }

        // 生成UUID作为文件名前缀
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + extension;
    }
}
