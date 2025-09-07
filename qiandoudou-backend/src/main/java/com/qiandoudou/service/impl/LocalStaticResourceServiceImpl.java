package com.qiandoudou.service.impl;

import com.qiandoudou.service.StaticResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地静态资源上传服务实现类
 * 用于替代远程SFTP服务器，将文件存储到本地并通过Spring Boot提供静态文件服务
 */
@Service("localStaticResourceService")
public class LocalStaticResourceServiceImpl implements StaticResourceService {

    private static final Logger logger = LoggerFactory.getLogger(LocalStaticResourceServiceImpl.class);

    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    // 本地存储基础路径
    private final String localBasePath;
    private final String baseUrl;

    public LocalStaticResourceServiceImpl() {
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        this.localBasePath = projectRoot + File.separator + "uploads";
        // 构建基础URL（去掉context-path中的/api）
        this.baseUrl = "http://localhost:" + (serverPort != null ? serverPort : "8080") + "/api";
        
        // 确保上传目录存在
        createDirectoryIfNotExists(localBasePath);
        createDirectoryIfNotExists(localBasePath + File.separator + "audio");
        createDirectoryIfNotExists(localBasePath + File.separator + "video");
        createDirectoryIfNotExists(localBasePath + File.separator + "image");
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
    public String uploadFile(byte[] fileData, String fileName, String fileType) {
        try {
            logger.info("开始上传文件到本地存储，文件名: {}, 类型: {}, 数据大小: {} bytes", 
                       fileName, fileType, fileData.length);
            
            // 构建本地存储路径
            String typeDirectory = localBasePath + File.separator + fileType;
            createDirectoryIfNotExists(typeDirectory);
            
            // 生成唯一文件名
            String uniqueFileName = generateUniqueFileName(fileName);
            String localFilePath = typeDirectory + File.separator + uniqueFileName;
            
            // 写入文件
            Path filePath = Paths.get(localFilePath);
            Files.write(filePath, fileData);
            
            logger.info("文件保存成功: {}", localFilePath);
            
            // 构建访问URL
            String fileUrl = baseUrl + "/" + fileType + "/" + uniqueFileName;
            logger.info("文件访问URL: {}", fileUrl);
            
            return fileUrl;
            
        } catch (Exception e) {
            logger.error("本地文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("本地文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建目录（如果不存在）
     */
    private void createDirectoryIfNotExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("创建本地目录: {}", directoryPath);
            }
        } catch (IOException e) {
            logger.warn("创建本地目录失败: {} - {}", directoryPath, e.getMessage());
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + extension;
    }
}

