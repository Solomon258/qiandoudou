package com.qiandoudou.service.impl;

import com.qiandoudou.entity.PostComment;
import com.qiandoudou.mapper.PostCommentMapper;
import com.qiandoudou.service.AudioDurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 音频时长解析服务实现类
 */
@Service
public class AudioDurationServiceImpl implements AudioDurationService {

    private static final Logger logger = LoggerFactory.getLogger(AudioDurationServiceImpl.class);

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Override
    @Async("taskExecutor")
    public void parseAndUpdateDurationAsync(Long commentId, String voiceUrl) {
        try {
            logger.info("开始异步解析音频时长，评论ID: {}, 语音URL: {}", commentId, voiceUrl);
            
            // 解析音频时长
            String duration = parseAudioDurationFromUrl(voiceUrl);
            
            if (duration != null) {
                // 更新数据库
                PostComment comment = new PostComment();
                comment.setId(commentId);
                comment.setVoiceDuration(duration);
                
                int updated = postCommentMapper.updateById(comment);
                if (updated > 0) {
                    logger.info("音频时长解析成功并更新到数据库，评论ID: {}, 时长: {}", commentId, duration);
                } else {
                    logger.warn("更新数据库失败，评论ID: {}", commentId);
                }
            } else {
                logger.warn("音频时长解析失败，评论ID: {}", commentId);
            }
            
        } catch (Exception e) {
            logger.error("异步解析音频时长失败，评论ID: {}, 错误: {}", commentId, e.getMessage(), e);
        }
    }

    @Override
    public String parseAudioDurationFromUrl(String voiceUrl) {
        try {
            logger.info("从URL解析音频时长: {}", voiceUrl);
            
            // 下载音频文件
            URL url = new URL(voiceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            if (connection.getResponseCode() == 200) {
                try (InputStream inputStream = connection.getInputStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    
                    // 读取音频数据
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    byte[] audioData = outputStream.toByteArray();
                    
                    // 根据URL判断文件格式
                    String fileName = voiceUrl.substring(voiceUrl.lastIndexOf("/") + 1);
                    
                    return parseAudioDurationFromBytes(audioData, fileName);
                }
            } else {
                logger.warn("下载音频文件失败，状态码: {}", connection.getResponseCode());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("从URL解析音频时长失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String parseAudioDurationFromBytes(byte[] audioData, String fileName) {
        try {
            if (fileName.toLowerCase().endsWith(".wav")) {
                return parseWavDuration(audioData);
            } else if (fileName.toLowerCase().endsWith(".mp3")) {
                return parseMp3Duration(audioData);
            } else {
                // 未知格式，尝试WAV解析
                logger.info("未知音频格式，尝试WAV解析: {}", fileName);
                return parseWavDuration(audioData);
            }
        } catch (Exception e) {
            logger.error("解析音频时长失败，文件: {}, 错误: {}", fileName, e.getMessage());
            // 降级到估算
            return estimateDurationBySize(audioData, fileName);
        }
    }

    /**
     * 解析WAV文件时长
     */
    private String parseWavDuration(byte[] audioData) throws UnsupportedAudioFileException, IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bais);
        
        AudioFormat format = audioInputStream.getFormat();
        long frames = audioInputStream.getFrameLength();
        double durationInSeconds = (frames + 0.0) / format.getFrameRate();
        
        audioInputStream.close();
        bais.close();
        
        int duration = (int) Math.ceil(durationInSeconds);
        logger.info("WAV文件时长解析成功: {}秒", duration);
        return duration + "s";
    }

    /**
     * 解析MP3文件时长（简单实现，基于文件大小估算）
     */
    private String parseMp3Duration(byte[] audioData) {
        try {
            // MP3解析比较复杂，这里使用简单的估算方法
            // 假设平均比特率为128kbps
            int bitrate = 128000; // bps
            double durationInSeconds = (audioData.length * 8.0) / bitrate;
            
            int duration = (int) Math.ceil(durationInSeconds);
            logger.info("MP3文件时长估算: {}秒", duration);
            return duration + "s";
            
        } catch (Exception e) {
            logger.error("MP3文件时长估算失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 基于文件大小估算时长
     */
    private String estimateDurationBySize(byte[] audioData, String fileName) {
        try {
            // 根据文件格式设置不同的比特率
            int bitrate = 128000; // 默认128kbps
            
            if (fileName.toLowerCase().endsWith(".wav")) {
                bitrate = 256000; // WAV通常比特率更高
            }
            
            double durationInSeconds = (audioData.length * 8.0) / bitrate;
            int duration = Math.max(1, (int) Math.ceil(durationInSeconds)); // 至少1秒
            
            logger.info("基于文件大小估算时长: {}秒", duration);
            return duration + "s";
            
        } catch (Exception e) {
            logger.error("基于文件大小估算时长失败: {}", e.getMessage());
            return "10s"; // 默认值
        }
    }
}
