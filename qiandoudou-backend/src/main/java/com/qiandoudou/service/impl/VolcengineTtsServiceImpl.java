package com.qiandoudou.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiandoudou.service.CharacterVoiceMappingService;
import com.qiandoudou.service.OssService;
import com.qiandoudou.service.VolcengineTtsService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 字节跳动/火山引擎TTS服务实现类 - HTTP方式
 */
@Service
public class VolcengineTtsServiceImpl implements VolcengineTtsService {

    private static final Logger logger = LoggerFactory.getLogger(VolcengineTtsServiceImpl.class);

    // 字节跳动/火山引擎TTS HTTP端点
    private static final String VOLCENGINE_TTS_ENDPOINT = "https://openspeech.bytedance.com/api/v1/tts";
    
    // 字节跳动/火山引擎TTS配置
    @Value("${ai.bytedance.tts.app-id:}")
    private String appId;
    
    @Value("${ai.bytedance.tts.access-token:}")
    private String accessToken;
    
    @Value("${ai.bytedance.tts.cluster:volcano_tts}")
    private String cluster;
    
    @Value("${ai.bytedance.tts.enabled:false}")
    private boolean enabled;

    @Autowired
    private OssService ossService;

    @Autowired
    private CharacterVoiceMappingService characterVoiceMappingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 默认火山引擎音色（当数据库中未找到时使用）
    private static final String DEFAULT_VOLCENGINE_VOICE = "zh_female_wanwanxiaohe_moon_bigtts";

    @Override
    public byte[] generateVoice(String text, String voiceType) throws Exception {
        if (!enabled) {
            throw new RuntimeException("火山引擎TTS服务未启用");
        }

        if (appId == null || appId.trim().isEmpty() || accessToken == null || accessToken.trim().isEmpty()) {
            throw new RuntimeException("火山引擎TTS配置不完整");
        }

        logger.info("=== 火山引擎TTS服务内部处理开始 ===");
        logger.info("火山引擎TTS - 服务配置: appId={}, cluster={}, endpoint={}", appId, cluster, VOLCENGINE_TTS_ENDPOINT);
        logger.info("火山引擎TTS - 输入文本: {}", text.length() > 50 ? text.substring(0, 50) + "..." : text);
        logger.info("火山引擎TTS - 输入音色类型: {}", voiceType);

        // 获取火山引擎音色
        String volcengineVoice = getVolcengineVoice(voiceType);
        logger.info("火山引擎TTS - 最终使用音色: {}", volcengineVoice);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        try {
            // 构建请求
            HttpPost httpPost = new HttpPost(VOLCENGINE_TTS_ENDPOINT);
            httpPost.setHeader("Content-Type", "application/json");
            // 火山引擎TTS的认证格式：Bearer;{access_token}（注意是分号）
            httpPost.setHeader("Authorization", "Bearer;" + accessToken);
            
            // 构建请求体
            Map<String, Object> request = new HashMap<>();
            
            // 应用配置
            Map<String, Object> app = new HashMap<>();
            app.put("appid", appId);
            app.put("token", accessToken);
            app.put("cluster", cluster);
            request.put("app", app);
            
            // 用户配置
            Map<String, Object> user = new HashMap<>();
            user.put("uid", UUID.randomUUID().toString());
            request.put("user", user);
            
            // 音频配置
            Map<String, Object> audio = new HashMap<>();
            audio.put("voice_type", volcengineVoice);
            audio.put("encoding", "mp3");
            request.put("audio", audio);
            
            // 请求配置
            Map<String, Object> requestConfig = new HashMap<>();
            requestConfig.put("reqid", UUID.randomUUID().toString());
            requestConfig.put("text", text);
            requestConfig.put("text_type", "plain");
            requestConfig.put("operation", "query");  // 添加缺失的operation参数
            
            request.put("request", requestConfig);

            String jsonRequest = objectMapper.writeValueAsString(request);
            logger.info("火山引擎TTS - HTTP请求体: {}", jsonRequest);
            logger.info("火山引擎TTS - 请求头: Authorization=Bearer;{}, Content-Type=application/json", 
                       accessToken.length() > 10 ? accessToken.substring(0, 10) + "..." : accessToken);
            
            StringEntity entity = new StringEntity(jsonRequest, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            
            // 发送请求
            logger.info("火山引擎TTS - 开始发送HTTP请求到: {}", VOLCENGINE_TTS_ENDPOINT);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.info("火山引擎TTS - HTTP响应状态码: {}", statusCode);
                
                if (statusCode == 200) {
                    HttpEntity responseEntity = response.getEntity();
                    
                    // 检查响应内容类型
                    String contentType = responseEntity.getContentType() != null ? 
                                       responseEntity.getContentType().getValue() : "unknown";
                    logger.info("火山引擎TTS - 响应Content-Type: {}", contentType);
                    
                    if (contentType.contains("audio/") || contentType.contains("application/octet-stream")) {
                        // 直接返回音频数据
                        byte[] audioData = EntityUtils.toByteArray(responseEntity);
                        logger.info("=== 火山引擎TTS - 直接获取音频数据成功 ===");
                        logger.info("火山引擎TTS - 音频数据大小: {} bytes", audioData.length);
                        return audioData;
                    } else {
                        // 解析JSON响应，包含Base64编码的音频数据
                        String responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                        // 避免打印过长的Base64数据，只显示响应结构
                        String logResponseBody = responseBody.length() > 500 ? 
                            responseBody.substring(0, 500) + "...(Base64音频数据已省略)" : responseBody;
                        logger.info("火山引擎TTS - JSON响应体: {}", logResponseBody);
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                        
                        // 检查是否有错误（火山引擎TTS的成功码是3000，不是0）
                        Object codeObj = responseMap.get("code");
                        int code = codeObj instanceof Integer ? (Integer) codeObj : Integer.parseInt(codeObj.toString());
                        if (code != 3000) {  // 3000是火山引擎TTS的成功码
                            String errorMsg = (String) responseMap.get("message");
                            logger.error("火山引擎TTS - API返回错误: code={}, message={}", code, errorMsg);
                            throw new RuntimeException("火山引擎TTS API错误: " + errorMsg);
                        }
                        
                        logger.info("火山引擎TTS - API调用成功: code={}, message={}", code, responseMap.get("message"));
                        
                        // 火山引擎TTS返回Base64编码的音频数据
                        if (responseMap.containsKey("data")) {
                            String base64AudioData = (String) responseMap.get("data");
                            if (base64AudioData != null && !base64AudioData.trim().isEmpty()) {
                                logger.info("火山引擎TTS - 获取到Base64音频数据，长度: {}", base64AudioData.length());
                                
                                // 解码Base64音频数据
                                try {
                                    byte[] audioData = Base64.getDecoder().decode(base64AudioData);
                                    logger.info("火山引擎TTS - Base64解码成功，音频数据大小: {} bytes", audioData.length);
                                    return audioData;
                                } catch (Exception e) {
                                    logger.error("火山引擎TTS - Base64解码失败: {}", e.getMessage());
                                    throw new RuntimeException("火山引擎TTS Base64解码失败: " + e.getMessage());
                                }
                            }
                        }
                        
                        logger.error("火山引擎TTS - 响应格式异常，未找到音频数据或URL");
                        throw new RuntimeException("火山引擎TTS响应格式异常");
                    }
                } else {
                    String errorBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("=== 火山引擎TTS - HTTP请求失败 ===");
                    logger.error("火山引擎TTS - 状态码: {}", statusCode);
                    logger.error("火山引擎TTS - 错误响应: {}", errorBody);
                    throw new RuntimeException("火山引擎TTS API调用失败，状态码: " + statusCode + ", 错误: " + errorBody);
                }
            }
            
        } catch (IOException e) {
            logger.error("火山引擎TTS API调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("火山引擎TTS API调用异常: " + e.getMessage(), e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.warn("关闭HTTP客户端时出错: {}", e.getMessage());
            }
        }
    }

    @Override
    public String generateVoiceAndUpload(String text, String voiceType) throws Exception {
        try {
            logger.info("开始使用火山引擎TTS生成语音并上传，文本长度: {}, 音色类型: {}", text.length(), voiceType);
            
            // 1. 生成语音
            byte[] voiceData = generateVoice(text, voiceType);
            
            // 2. 生成文件名
            String fileName = "volcengine_tts_" + System.currentTimeMillis() + ".mp3";
            
            // 3. 上传到阿里云OSS
            String voiceUrl = ossService.uploadFile(voiceData, fileName, "audio");
            
            logger.info("火山引擎TTS语音生成并上传成功，URL: {}", voiceUrl);
            return voiceUrl;
            
        } catch (Exception e) {
            logger.error("火山引擎TTS语音生成和上传失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled && 
               appId != null && !appId.trim().isEmpty() && 
               accessToken != null && !accessToken.trim().isEmpty();
    }

    /**
     * 获取火山引擎对应的音色 - 从数据库读取
     */
    private String getVolcengineVoice(String voiceType) {
        try {
            // 从数据库获取火山引擎音色配置
            String volcengineVoice = characterVoiceMappingService.getVolcengineVoiceTypeByCharacterName(voiceType);
            if (volcengineVoice != null && !volcengineVoice.trim().isEmpty()) {
                logger.info("从数据库获取到火山引擎音色，音色类型: {}, 火山引擎音色: {}", voiceType, volcengineVoice);
                return volcengineVoice;
            }
        } catch (Exception e) {
            logger.error("从数据库获取火山引擎音色配置失败，音色类型: {}", voiceType, e);
        }
        
        // 如果数据库中没有找到对应的音色，使用默认音色
        logger.warn("未找到音色类型 {} 对应的火山引擎音色，使用默认音色: {}", voiceType, DEFAULT_VOLCENGINE_VOICE);
        return DEFAULT_VOLCENGINE_VOICE;
    }

    /**
     * 从URL下载音频数据
     */
    private byte[] downloadAudioFromUrl(String audioUrl) throws IOException {
        logger.info("开始从URL下载音频数据: {}", audioUrl);
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpGet httpGet = new HttpGet(audioUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    byte[] audioData = EntityUtils.toByteArray(entity);
                    logger.info("音频数据下载成功，大小: {} bytes", audioData.length);
                    return audioData;
                } else {
                    throw new IOException("下载音频数据失败，状态码: " + statusCode);
                }
            }
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.warn("关闭HTTP客户端时出错: {}", e.getMessage());
            }
        }
    }

}

