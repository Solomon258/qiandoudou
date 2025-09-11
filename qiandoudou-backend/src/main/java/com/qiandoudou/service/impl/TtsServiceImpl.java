package com.qiandoudou.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiandoudou.service.CharacterVoiceMappingService;
import com.qiandoudou.service.OssService;
import com.qiandoudou.service.TtsService;
import com.qiandoudou.service.VolcengineTtsService;
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

    @Autowired
    private CharacterVoiceMappingService characterVoiceMappingService;

    @Autowired
    private VolcengineTtsService volcengineTtsService;

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
            
            // 1. 优先尝试火山引擎TTS
            byte[] voiceData = null;
            String fileName = null;
            String usedTtsService = "";
            
            if (volcengineTtsService.isAvailable()) {
                try {
                    logger.info("=== 开始调用火山引擎TTS API ===");
                    logger.info("火山引擎TTS - 输入参数: 文本长度={}, 音色类型={}", text.length(), voiceType);
                    
                    long startTime = System.currentTimeMillis();
                    voiceData = volcengineTtsService.generateVoice(text, voiceType);
                    long endTime = System.currentTimeMillis();
                    
                    fileName = "volcengine_tts_" + System.currentTimeMillis() + ".mp3";
                    usedTtsService = "火山引擎TTS";
                    
                    logger.info("=== 火山引擎TTS API 调用成功 ===");
                    logger.info("火山引擎TTS - 响应数据: 音频大小={}bytes, 耗时={}ms", voiceData.length, (endTime - startTime));
                    logger.info("火山引擎TTS - 生成文件名: {}", fileName);
                } catch (Exception e) {
                    logger.error("=== 火山引擎TTS API 调用失败 ===");
                    logger.error("火山引擎TTS - 错误信息: {}", e.getMessage());
                    logger.warn("火山引擎TTS调用失败，准备降级到原有TTS接口A", e);
                    voiceData = null; // 重置，准备使用原有接口
                }
            } else {
                logger.info("=== 火山引擎TTS服务不可用，直接使用原有TTS接口A ===");
                logger.info("火山引擎TTS - 服务状态: 未启用或配置不完整");
            }
            
            // 2. 如果火山引擎失败，降级到原有TTS接口
            if (voiceData == null) {
                try {
                    logger.info("=== 开始调用原有TTS接口A ===");
                    logger.info("原有TTS接口A - 输入参数: 文本长度={}, 音色类型={}", text.length(), voiceType);
                    
                    long startTime = System.currentTimeMillis();
                    voiceData = generateVoice(text, voiceType);
                    long endTime = System.currentTimeMillis();
                    
                    fileName = "tts_" + System.currentTimeMillis() + ".wav";
                    usedTtsService = "原有TTS接口A";
                    
                    logger.info("=== 原有TTS接口A 调用成功 ===");
                    logger.info("原有TTS接口A - 响应数据: 音频大小={}bytes, 耗时={}ms", voiceData.length, (endTime - startTime));
                    logger.info("原有TTS接口A - 生成文件名: {}", fileName);
                } catch (Exception e) {
                    logger.error("=== 原有TTS接口A 调用失败 ===");
                    logger.error("原有TTS接口A - 错误信息: {}", e.getMessage());
                    throw e; // 重新抛出异常
                }
            }
            
            // 3. 上传到阿里云OSS
            logger.info("=== 开始上传到阿里云OSS ===");
            logger.info("OSS上传 - 文件名: {}, 音频大小: {}bytes", fileName, voiceData.length);
            
            long uploadStartTime = System.currentTimeMillis();
            String voiceUrl = ossService.uploadFile(voiceData, fileName, "audio");
            long uploadEndTime = System.currentTimeMillis();
            
            logger.info("=== 语音生成和上传完成 ===");
            logger.info("最终结果 - 使用TTS服务: {}", usedTtsService);
            logger.info("最终结果 - 音频URL: {}", voiceUrl);
            logger.info("最终结果 - OSS上传耗时: {}ms", (uploadEndTime - uploadStartTime));
            
            return voiceUrl;
            
        } catch (Exception e) {
            logger.error("语音生成和上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("语音生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateVoice(String text, String voiceType) {
        logger.info("=== 原有TTS接口A - 内部处理开始 ===");
        logger.info("原有TTS接口A - 服务配置: apiUrl={}", ttsApiUrl);
        logger.info("原有TTS接口A - 输入文本: {}", text.length() > 50 ? text.substring(0, 50) + "..." : text);
        logger.info("原有TTS接口A - 输入音色类型: {}", voiceType);
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        try {
            // 获取声音路径，支持降级
            String audioPath = getAudioPathWithFallback(voiceType);
            logger.info("原有TTS接口A - 最终使用音频路径: {}", audioPath);
            
            // 构建请求
            HttpPost httpPost = new HttpPost(ttsApiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            
            // 构建请求体
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("text", text);
            requestData.put("audio_paths", Arrays.asList(audioPath));
            
            String jsonRequest = objectMapper.writeValueAsString(requestData);
            logger.info("原有TTS接口A - HTTP请求体: {}", jsonRequest);
            logger.info("原有TTS接口A - 请求头: Content-Type=application/json");
            
            StringEntity entity = new StringEntity(jsonRequest, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            
            // 发送请求
            logger.info("原有TTS接口A - 开始发送HTTP请求到: {}", ttsApiUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.info("原有TTS接口A - HTTP响应状态码: {}", statusCode);
                
                if (statusCode == 200) {
                    HttpEntity responseEntity = response.getEntity();
                    String contentType = responseEntity.getContentType() != null ? 
                                       responseEntity.getContentType().getValue() : "unknown";
                    logger.info("原有TTS接口A - 响应Content-Type: {}", contentType);
                    
                    byte[] voiceData = EntityUtils.toByteArray(responseEntity);
                    
                    logger.info("=== 原有TTS接口A - 调用成功 ===");
                    logger.info("原有TTS接口A - 音频数据大小: {} bytes", voiceData.length);
                    return voiceData;
                } else {
                    String errorBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("=== 原有TTS接口A - HTTP请求失败 ===");
                    logger.error("原有TTS接口A - 状态码: {}", statusCode);
                    logger.error("原有TTS接口A - 错误响应: {}", errorBody);
                    
                    // 如果是文件不存在错误，尝试降级处理
                    if (errorBody.contains("No such file or directory")) {
                        logger.warn("原有TTS接口A - 音频文件不存在，尝试使用备用路径");
                        return generateVoiceWithFallback(text, httpClient);
                    }
                    
                    throw new RuntimeException("原有TTS接口A调用失败，状态码: " + statusCode);
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
                    logger.warn("备用路径 {} 调用失败，状态码: {}, 响应: {}", fallbackPath, statusCode, errorBody);
                    }
                }
                
            } catch (Exception e) {
                logger.warn("备用路径 {} 调用异常: {}", fallbackPath, e.getMessage());
            }
        }
        
        // 所有备用路径都失败了
        throw new RuntimeException("所有音频路径都无法使用，TTS服务不可用");
    }

    @Override
    public String generateVoiceByCharacterName(String text, String characterName) {
        try {
            logger.info("根据人物名称生成语音，人物名称: {}, 文本长度: {}", characterName, text.length());
            
            // 1. 优先尝试火山引擎TTS（使用人物名称作为音色类型）
            byte[] voiceData = null;
            String fileName = null;
            String usedTtsService = "";
            
            if (volcengineTtsService.isAvailable()) {
                try {
                    logger.info("=== 开始调用火山引擎TTS API（基于人物名称） ===");
                    logger.info("火山引擎TTS - 输入参数: 人物名称={}, 文本长度={}", characterName, text.length());
                    
                    // 从数据库获取火山引擎音色信息
                    String volcengineVoiceType = characterVoiceMappingService.getVolcengineVoiceTypeByCharacterName(characterName);
                    String volcengineCharacterName = characterVoiceMappingService.getVolcengineCharacterNameByCharacterName(characterName);
                    logger.info("火山引擎TTS - 数据库映射: 前端人名={} → 火山引擎人名={}, 音色类型={}", 
                               characterName, volcengineCharacterName, volcengineVoiceType);
                    
                    long startTime = System.currentTimeMillis();
                    voiceData = volcengineTtsService.generateVoice(text, characterName);
                    long endTime = System.currentTimeMillis();
                    
                    fileName = "volcengine_tts_" + characterName + "_" + System.currentTimeMillis() + ".mp3";
                    usedTtsService = "火山引擎TTS";
                    
                    logger.info("=== 火山引擎TTS API 调用成功（基于人物名称） ===");
                    logger.info("火山引擎TTS - 响应数据: 音频大小={}bytes, 耗时={}ms", voiceData.length, (endTime - startTime));
                    logger.info("火山引擎TTS - 生成文件名: {}", fileName);
                } catch (Exception e) {
                    logger.error("=== 火山引擎TTS API 调用失败（基于人物名称） ===");
                    logger.error("火山引擎TTS - 错误信息: {}", e.getMessage());
                    logger.warn("火山引擎TTS调用失败，准备降级到原有TTS接口A", e);
                    voiceData = null; // 重置，准备使用原有接口
                }
            } else {
                logger.info("=== 火山引擎TTS服务不可用，直接使用原有TTS接口A（基于人物名称） ===");
                logger.info("火山引擎TTS - 服务状态: 未启用或配置不完整");
            }
            
            // 2. 如果火山引擎失败，降级到原有TTS接口
            if (voiceData == null) {
                try {
                    logger.info("=== 开始调用原有TTS接口A（基于人物名称） ===");
                    logger.info("原有TTS接口A - 输入参数: 人物名称={}, 文本长度={}", characterName, text.length());
                    
                    // 从数据库获取原TTS接口A的配置信息
                    String originalTtsCharacterName = characterVoiceMappingService.getOriginalTtsCharacterNameByCharacterName(characterName);
                    String originalTtsVoicePath = characterVoiceMappingService.getOriginalTtsVoicePathByCharacterName(characterName);
                    logger.info("原有TTS接口A - 数据库映射: 前端人名={} → 原TTS人名={}, 声音路径={}", 
                               characterName, originalTtsCharacterName, originalTtsVoicePath);
                    
                    long startTime = System.currentTimeMillis();
                    voiceData = generateVoiceWithPath(text, originalTtsVoicePath);
                    long endTime = System.currentTimeMillis();
                    
                    fileName = "tts_" + characterName + "_" + System.currentTimeMillis() + ".wav";
                    usedTtsService = "原有TTS接口A";
                    
                    logger.info("=== 原有TTS接口A 调用成功（基于人物名称） ===");
                    logger.info("原有TTS接口A - 响应数据: 音频大小={}bytes, 耗时={}ms", voiceData.length, (endTime - startTime));
                    logger.info("原有TTS接口A - 生成文件名: {}", fileName);
                } catch (Exception e) {
                    logger.error("=== 原有TTS接口A 调用失败（基于人物名称） ===");
                    logger.error("原有TTS接口A - 错误信息: {}", e.getMessage());
                    throw e; // 重新抛出异常
                }
            }
            
            // 3. 上传到阿里云OSS
            logger.info("=== 开始上传到阿里云OSS（基于人物名称） ===");
            logger.info("OSS上传 - 人物名称: {}, 文件名: {}, 音频大小: {}bytes", characterName, fileName, voiceData.length);
            
            long uploadStartTime = System.currentTimeMillis();
            String voiceUrl = ossService.uploadFile(voiceData, fileName, "audio");
            long uploadEndTime = System.currentTimeMillis();
            
            logger.info("=== 基于人物名称的语音生成和上传完成 ===");
            logger.info("最终结果 - 人物名称: {}", characterName);
            logger.info("最终结果 - 使用TTS服务: {}", usedTtsService);
            logger.info("最终结果 - 音频URL: {}", voiceUrl);
            logger.info("最终结果 - OSS上传耗时: {}ms", (uploadEndTime - uploadStartTime));
            
            return voiceUrl;
            
        } catch (Exception e) {
            logger.error("根据人物名称生成语音失败，人物名称: {}, 错误: {}", characterName, e.getMessage(), e);
            throw new RuntimeException("语音生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用指定声音路径生成语音
     */
    private byte[] generateVoiceWithPath(String text, String voicePath) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        try {
            logger.info("使用指定音频路径生成语音: {}", voicePath);
            
            // 构建请求
            HttpPost httpPost = new HttpPost(ttsApiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            
            // 构建请求体
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("text", text);
            requestData.put("audio_paths", Arrays.asList(voicePath));
            
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
                    
                    // 如果指定路径失败，使用默认路径重试
                    if (errorBody.contains("No such file or directory")) {
                        logger.warn("指定音频文件不存在，使用默认路径重试");
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
}
