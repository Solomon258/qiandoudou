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

    // 声音类型映射
    private static final Map<String, String> VOICE_TYPE_MAP = new HashMap<>();
    
    static {
        VOICE_TYPE_MAP.put("奇妙栩", "wav/peiyin/配音女-奇妙栩/1.wav");
        VOICE_TYPE_MAP.put("温柔女声", "wav/peiyin/配音女-温柔/1.wav");
        VOICE_TYPE_MAP.put("甜美女声", "wav/peiyin/配音女-甜美/1.wav");
        VOICE_TYPE_MAP.put("成熟男声", "wav/peiyin/配音男-成熟/1.wav");
        VOICE_TYPE_MAP.put("阳光男声", "wav/peiyin/配音男-阳光/1.wav");
        // 默认使用奇妙栩
        VOICE_TYPE_MAP.put("default", "wav/peiyin/配音女-奇妙栩/1.wav");
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
            // 构建请求
            HttpPost httpPost = new HttpPost(ttsApiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            
            // 获取声音路径
            String audioPath = VOICE_TYPE_MAP.getOrDefault(voiceType, VOICE_TYPE_MAP.get("default"));
            
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
}
