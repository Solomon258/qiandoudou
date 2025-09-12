package com.qiandoudou.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiandoudou.service.ImageToTextService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 图生文服务实现类
 */
@Service
public class ImageToTextServiceImpl implements ImageToTextService {

    private static final Logger logger = LoggerFactory.getLogger(ImageToTextServiceImpl.class);

    @Value("${ai.image-to-text.api-url:http://113.45.231.186:1236/generate}")
    private String apiUrl;

    @Value("${ai.bytedance.enabled:true}")
    private boolean byteDanceEnabled;

    @Autowired
    private ByteDanceImageToTextService byteDanceImageToTextService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Pattern thinkTagPattern = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);
    // 用于提取JSON中text字段内容的正则表达式
    private final Pattern jsonTextPattern = Pattern.compile("\\{\"text\":\"([^\"]*?)\"\\}");

    @Override
    public String generateTextFromImage(String imageBase64, String prompt) {
        // 1. 优先尝试字节跳动API
        if (byteDanceEnabled && byteDanceImageToTextService.isAvailable()) {
            try {
                logger.info("使用字节跳动API进行图生文");
                String result = byteDanceImageToTextService.generateTextFromImage(imageBase64, prompt);
                logger.info("字节跳动API调用成功");
                return result;
            } catch (Exception e) {
                logger.warn("字节跳动API失败，降级到原有API");
                // 继续执行原有API逻辑
            }
        } else {
            logger.info("字节跳动API未启用或不可用，使用原有API");
        }

        // 2. 降级到原有API
        return callOriginalApi(imageBase64, prompt);
    }

    /**
     * 调用原有的图生文API
     */
    private String callOriginalApi(String imageBase64, String prompt) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 构造请求
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Content-Type", "application/json");

            // 构造请求体 - 添加中文限制
            String finalPrompt = prompt != null ? prompt : "你是一个朋友圈文案助手，根据图片生成朋友圈的文案，少于100字，不要生成其他内容,不要思考太久";
            finalPrompt += "。要求：只使用中文，不要生成任何英文单词、字母或英文表达，用中文表达亲昵如\"亲爱的\"、\"宝贝\"、\"么么哒\"等。";
            Map<String, Object> payload = new HashMap<>();
            payload.put("image", new String[]{imageBase64});
            payload.put("prompt", finalPrompt);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            StringEntity entity = new StringEntity(jsonPayload, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);

            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    
                    // 处理响应
                    String resultText = extractTextFromResponse(responseBody);
                    
                    // 去除 <think></think> 部分
                    resultText = thinkTagPattern.matcher(resultText).replaceAll("");
                    
                    logger.info("图生文API调用成功");
                    
                    return resultText.trim();
                } else {
                    logger.error("图生文API调用失败，状态码: {}", statusCode);
                    throw new RuntimeException("API调用失败，状态码: " + statusCode);
                }
            }
        } catch (IOException e) {
            logger.error("图生文API调用异常: {}", e.getMessage());
            throw new RuntimeException("图生文API调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * 从API响应中提取文本内容
     * 处理格式如: {"text":"\n竹影婆娑，绿裳映幽。风过处，叶动心弦，偷得半日闲。"}
     */
    private String extractTextFromResponse(String responseBody) {
        try {
            // 首先尝试解析为JSON
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            // 如果响应是JSON对象且包含text字段
            if (responseJson.isObject() && responseJson.has("text")) {
                String textContent = responseJson.get("text").asText();
                // 去除开头的换行符和空格
                return textContent.replaceAll("^\\s*\\n\\s*", "").trim();
            }
            
            // 如果是其他格式，尝试使用正则表达式提取
            java.util.regex.Matcher matcher = jsonTextPattern.matcher(responseBody);
            if (matcher.find()) {
                String textContent = matcher.group(1);
                // 去除开头的换行符和空格
                return textContent.replaceAll("^\\s*\\n\\s*", "").trim();
            }
            
            // 如果是纯文本响应
            if (responseJson.isTextual()) {
                return responseJson.asText().trim();
            }
            
            // 其他情况，返回原始响应
            return responseBody.trim();
            
        } catch (Exception e) {
            logger.warn("解析响应JSON失败，尝试直接处理: {}", e.getMessage());
            
            // JSON解析失败，尝试正则表达式直接处理字符串
            java.util.regex.Matcher matcher = jsonTextPattern.matcher(responseBody);
            if (matcher.find()) {
                String textContent = matcher.group(1);
                // 去除开头的换行符和空格
                return textContent.replaceAll("^\\s*\\n\\s*", "").trim();
            }
            
            // 最后返回原始内容
            return responseBody.trim();
        }
    }

    @Override
    public String generateTextFromPrompt(String prompt) {
        // 1. 优先尝试字节跳动API
        if (byteDanceEnabled && byteDanceImageToTextService.isAvailable()) {
            try {
                logger.info("使用字节跳动API进行文本生成");
                String result = byteDanceImageToTextService.generateTextFromPrompt(prompt);
                logger.info("字节跳动API调用成功");
                return result;
            } catch (Exception e) {
                logger.warn("字节跳动API失败，降级到原有API");
                // 继续执行原有API逻辑
            }
        } else {
            logger.info("字节跳动API未启用或不可用，使用原有API");
        }

        // 2. 降级到原有API（不传图片，只传文本）
        return callOriginalApiTextOnly(prompt);
    }

    /**
     * 调用原有的API进行纯文本生成（不需要图片）
     */
    private String callOriginalApiTextOnly(String prompt) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        try {
            // 构建请求
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            
            // 构建请求体（不包含图片）- 添加中文限制
            String finalPrompt = prompt + "。要求：只使用中文，不要生成任何英文单词、字母或英文表达，用中文表达亲昵如\"亲爱的\"、\"宝贝\"、\"么么哒\"等。";
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("prompt", finalPrompt);
            // 不传image字段，只传文本
            
            String jsonRequest = objectMapper.writeValueAsString(requestData);
            
            StringEntity entity = new StringEntity(jsonRequest, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);

            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    
                    // 处理响应
                    String resultText = extractTextFromResponse(responseBody);
                    
                    // 去除 <think></think> 部分
                    resultText = thinkTagPattern.matcher(resultText).replaceAll("");
                    
                    logger.info("文本生成API调用成功");
                    
                    return resultText.trim();
                } else {
                    logger.error("文本生成API调用失败，状态码: {}", statusCode);
                    throw new RuntimeException("API调用失败，状态码: " + statusCode);
                }
            }
        } catch (IOException e) {
            logger.error("文本生成API调用异常: {}", e.getMessage());
            throw new RuntimeException("文本生成API调用异常: " + e.getMessage(), e);
        }
    }
}
