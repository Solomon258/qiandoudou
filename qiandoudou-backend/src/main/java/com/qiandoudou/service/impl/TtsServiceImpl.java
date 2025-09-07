package com.qiandoudou.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiandoudou.service.OssService;
import com.qiandoudou.service.TtsService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * TTS语音合成服务实现类
 */
@Service
public class TtsServiceImpl implements TtsService {

    private static final Logger logger = LoggerFactory.getLogger(TtsServiceImpl.class);

    @Value("${ai.tts.api-url:http://113.45.231.186:1231/tts_url}")
    private String ttsApiUrl;

    @Autowired
    private OssService ossService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 声音类型映射 - 使用更通用的路径，支持降级
    private static final Map<String, String> VOICE_TYPE_MAP = new HashMap<>();
    
    // 备用音频路径列表，按优先级排序 - 基于实际测试结果
    private static final List<String> FALLBACK_AUDIO_PATHS = Arrays.asList(
        "wav/peiyin/配音女-奇妙栩/1.wav"  // 目前唯一可用的路径
    );
    
    static {
        // 基于实际测试，目前只有奇妙栩路径可用，所以所有声音类型都映射到这个路径
        String availablePath = "wav/peiyin/配音女-奇妙栩/1.wav";
        
        // 主要声音类型映射 - 全部使用可用路径
        VOICE_TYPE_MAP.put("奇妙栩", availablePath);
        VOICE_TYPE_MAP.put("温柔女声", availablePath);
        VOICE_TYPE_MAP.put("甜美女声", availablePath);
        VOICE_TYPE_MAP.put("成熟男声", availablePath);
        VOICE_TYPE_MAP.put("阳光男声", availablePath);
        
        // AI伴侣声音类型映射 - 全部使用可用路径
        VOICE_TYPE_MAP.put("female_sweet", availablePath);
        VOICE_TYPE_MAP.put("female_gentle", availablePath);
        VOICE_TYPE_MAP.put("female_intellectual", availablePath);
        VOICE_TYPE_MAP.put("female_lively", availablePath);
        VOICE_TYPE_MAP.put("male_mature", availablePath);
        VOICE_TYPE_MAP.put("male_gentle", availablePath);
        
        // 默认路径
        VOICE_TYPE_MAP.put("default", availablePath);
    }

    @Override
    public String generateVoiceAndUpload(String text, String voiceType) {
        try {
            logger.info("开始生成语音，文本长度: {}, 声音类型: {}", text.length(), voiceType);
            
            // 1. 调用TTS API生成语音
            byte[] voiceData = generateVoice(text, voiceType);
            
            // 2. 生成文件名
            String fileName = "tts_" + System.currentTimeMillis() + ".wav";
            
            // 3. 上传到阿里云OSS
            String voiceUrl = ossService.uploadFile(voiceData, fileName, "audio");
            
            logger.info("语音生成并上传成功，URL: {}", voiceUrl);
            return voiceUrl;
            
        } catch (Exception e) {
            logger.error("语音生成和上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("语音生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateVoice(String text, String voiceType) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        try {
            // 获取声音路径，支持降级
            String audioPath = getAudioPathWithFallback(voiceType);
            logger.info("使用音频路径: {}", audioPath);
            
            // 构建请求
            HttpPost httpPost = new HttpPost(ttsApiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            
            // 构建请求体
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("text", text);
            requestData.put("audio_paths", Arrays.asList(audioPath));
            
            String jsonRequest = objectMapper.writeValueAsString(requestData);
            logger.info("TTS API请求: {}", jsonRequest);
            
            StringEntity entity = new StringEntity(jsonRequest, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            
            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200) {
                    HttpEntity responseEntity = response.getEntity();
                    byte[] voiceData = EntityUtils.toByteArray(responseEntity);
                    
                    logger.info("TTS API调用成功，语音数据大小: {} bytes", voiceData.length);
                    return voiceData;
                } else {
                    String errorBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("TTS API调用失败，状态码: {}, 响应: {}", statusCode, errorBody);
                    
                    // 如果是文件不存在错误，尝试降级处理
                    if (errorBody.contains("No such file or directory")) {
                        logger.warn("音频文件不存在，尝试使用备用路径");
                        return generateVoiceWithFallback(text, httpClient);
                    }
                    
                    throw new RuntimeException("TTS API调用失败，状态码: " + statusCode);
                }
            }
            
        } catch (IOException e) {
            logger.error("TTS API调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("TTS API调用异常: " + e.getMessage(), e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.warn("关闭HTTP客户端时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 获取音频路径，支持降级
     */
    private String getAudioPathWithFallback(String voiceType) {
        // 首先尝试指定的声音类型
        String audioPath = VOICE_TYPE_MAP.get(voiceType);
        if (audioPath != null) {
            return audioPath;
        }
        
        // 如果没有找到，使用默认路径
        logger.warn("未找到声音类型 {}，使用默认路径", voiceType);
        return VOICE_TYPE_MAP.get("default");
    }
    
    /**
     * 使用备用路径生成语音
     */
    private byte[] generateVoiceWithFallback(String text, CloseableHttpClient httpClient) throws IOException {
        for (String fallbackPath : FALLBACK_AUDIO_PATHS) {
            try {
                logger.info("尝试备用音频路径: {}", fallbackPath);
                
                HttpPost httpPost = new HttpPost(ttsApiUrl);
                httpPost.setHeader("Content-Type", "application/json");
                
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("text", text);
                requestData.put("audio_paths", Arrays.asList(fallbackPath));
                
                String jsonRequest = objectMapper.writeValueAsString(requestData);
                StringEntity entity = new StringEntity(jsonRequest, StandardCharsets.UTF_8);
                httpPost.setEntity(entity);
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    
                    if (statusCode == 200) {
                        HttpEntity responseEntity = response.getEntity();
                        byte[] voiceData = EntityUtils.toByteArray(responseEntity);
                        
                        logger.info("备用路径 {} 调用成功，语音数据大小: {} bytes", fallbackPath, voiceData.length);
                        return voiceData;
                    } else {
                        String errorBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        logger.warn("备用路径 {} 调用失败，状态码: {}", fallbackPath, statusCode);
                    }
                }
                
            } catch (Exception e) {
                logger.warn("备用路径 {} 调用异常: {}", fallbackPath, e.getMessage());
            }
        }
        
        // 所有备用路径都失败了
        throw new RuntimeException("所有音频路径都无法使用，TTS服务不可用");
    }
}
