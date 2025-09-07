package com.qiandoudou.service.impl;

import com.qiandoudou.service.StaticResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.*;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

/**
 * 静态资源上传服务实现类
 */
@Service
public class StaticResourceServiceImpl implements StaticResourceService {

    private static final Logger logger = LoggerFactory.getLogger(StaticResourceServiceImpl.class);

    @Value("${static.resource.server.host:8.148.206.18}")
    private String serverHost;

    @Value("${static.resource.server.port:22}")
    private int serverPort;

    @Value("${static.resource.server.username:root}")
    private String username;

    @Value("${static.resource.server.password:Spring110}")
    private String password;

    @Value("${static.resource.server.base-path:/bankapp/res}")
    private String basePath;

    @Value("${static.resource.server.base-url:http://8.148.206.18/bankapp/res}")
    private String baseUrl;

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
        Session session = null;
        ChannelSftp channelSftp = null;
        
        try {
            // 创建JSCH会话
            JSch jsch = new JSch();
            session = jsch.getSession(username, serverHost, serverPort);
            session.setPassword(password);
            
            // 跳过主机密钥检查
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            // 连接
            session.connect();
            logger.info("SSH连接成功: {}@{}", username, serverHost);
            
            // 打开SFTP通道
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            logger.info("SFTP通道打开成功");
            
            // 构建远程路径
            String remotePath = basePath + "/" + fileType + "/";
            
            // 确保远程目录存在
            createRemoteDirectoryIfNotExists(channelSftp, remotePath);
            
            // 生成唯一文件名
            String uniqueFileName = generateUniqueFileName(fileName);
            String remoteFilePath = remotePath + uniqueFileName;
            
            // 上传文件
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData)) {
                channelSftp.put(inputStream, remoteFilePath);
                logger.info("文件上传成功: {}", remoteFilePath);
            }
            
            // 返回文件的HTTP URL
            String fileUrl = baseUrl + "/" + fileType + "/" + uniqueFileName;
            logger.info("文件访问URL: {}", fileUrl);
            
            return fileUrl;
            
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } finally {
            // 关闭连接
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
                logger.info("SFTP通道已关闭");
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
                logger.info("SSH会话已关闭");
            }
        }
    }

    /**
     * 创建远程目录（如果不存在）
     */
    private void createRemoteDirectoryIfNotExists(ChannelSftp channelSftp, String remotePath) {
        try {
            // 分割路径并逐级创建
            String[] pathParts = remotePath.split("/");
            String currentPath = "";
            
            for (String part : pathParts) {
                if (part.isEmpty()) {
                    currentPath += "/";
                    continue;
                }
                
                currentPath += part + "/";
                
                try {
                    channelSftp.stat(currentPath);
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        // 目录不存在，创建它
                        try {
                            channelSftp.mkdir(currentPath);
                            logger.info("创建远程目录: {}", currentPath);
                        } catch (SftpException mkdirEx) {
                            logger.warn("创建目录失败: {} - {}", currentPath, mkdirEx.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("检查/创建远程目录时出错: {}", e.getMessage());
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
