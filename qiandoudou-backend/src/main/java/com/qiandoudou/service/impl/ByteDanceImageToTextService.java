package com.qiandoudou.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;

/**
 * 字节跳动图生文服务实现类
 * 使用RestTemplate调用API，并添加图片格式验证和转换
 */
@Service
public class ByteDanceImageToTextService {

    private static final Logger logger = LoggerFactory.getLogger(ByteDanceImageToTextService.class);

    @Value("${ai.bytedance.api-key}")
    private String apiKey;

    @Value("${ai.bytedance.model}")
    private String model;

    @Value("${ai.bytedance.enabled:true}")
    private boolean enabled;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 验证和转换图片格式
     * @param imageBase64 原始base64图片数据
     * @return 处理后的base64图片数据
     */
    private String validateAndConvertImage(String imageBase64) throws IOException {
        // 去掉可能的data:image前缀
        String cleanBase64 = imageBase64;
        if (imageBase64.startsWith("data:image/")) {
            int commaIndex = imageBase64.indexOf(',');
            if (commaIndex != -1) {
                cleanBase64 = imageBase64.substring(commaIndex + 1);
            }
        }

        // 解码base64为字节数组
        byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
        
        // 检测图片格式
        String formatName = getImageFormat(imageBytes);
        logger.info("检测到图片格式: {}", formatName);

        // 如果是支持的格式，直接返回
        if ("jpeg".equalsIgnoreCase(formatName) || "jpg".equalsIgnoreCase(formatName) || "png".equalsIgnoreCase(formatName)) {
            return cleanBase64;
        }

        // 如果格式不支持，转换为JPEG
        logger.info("图片格式不支持，正在转换为JPEG格式");
        try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("无法读取图片，可能是格式不支持");
            }

            // 转换为JPEG
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                // 如果原图有透明通道，创建白色背景
                if (image.getColorModel().hasAlpha()) {
                    BufferedImage jpegImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                    jpegImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
                    ImageIO.write(jpegImage, "jpg", outputStream);
                } else {
                    ImageIO.write(image, "jpg", outputStream);
                }
                
                byte[] convertedBytes = outputStream.toByteArray();
                String convertedBase64 = Base64.getEncoder().encodeToString(convertedBytes);
                logger.info("图片格式转换完成");
                return convertedBase64;
            }
        }
    }

    /**
     * 获取图片格式 - 基于文件头魔数检测
     */
    private String getImageFormat(byte[] imageBytes) {
        if (imageBytes.length < 4) {
            return "unknown";
        }
        
        // JPEG格式检测 (FF D8 FF)
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 && imageBytes[2] == (byte) 0xFF) {
            return "jpeg";
        }
        
        // PNG格式检测 (89 50 4E 47)
        if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == 0x50 && imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {
            return "png";
        }
        
        // GIF格式检测 (47 49 46 38)
        if (imageBytes[0] == 0x47 && imageBytes[1] == 0x49 && imageBytes[2] == 0x46 && imageBytes[3] == 0x38) {
            return "gif";
        }
        
        // WEBP格式检测 (52 49 46 46 ... 57 45 42 50)
        if (imageBytes.length >= 12 && 
            imageBytes[0] == 0x52 && imageBytes[1] == 0x49 && imageBytes[2] == 0x46 && imageBytes[3] == 0x46 &&
            imageBytes[8] == 0x57 && imageBytes[9] == 0x45 && imageBytes[10] == 0x42 && imageBytes[11] == 0x50) {
            return "webp";
        }
        
        // BMP格式检测 (42 4D)
        if (imageBytes[0] == 0x42 && imageBytes[1] == 0x4D) {
            return "bmp";
        }
        
        return "unknown";
    }

    /**
     * 使用字节跳动API生成文字描述
     * @param imageBase64 图片的base64编码字符串
     * @param prompt 提示词
     * @return 生成的文字描述
     */
    public String generateTextFromImage(String imageBase64, String prompt) {
        if (!enabled) {
            throw new RuntimeException("字节跳动API未启用");
        }

        try {
            logger.info("开始调用字节跳动图生文API");

            // 验证和转换图片格式
            String processedBase64 = validateAndConvertImage(imageBase64);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 构建消息内容
            List<Map<String, Object>> contentParts = new ArrayList<>();
            
            // 图片部分 - 使用处理后的base64数据
            String base64Data = "data:image/jpeg;base64," + processedBase64;
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            Map<String, String> imageUrl = new HashMap<>();
            imageUrl.put("url", base64Data);
            imageContent.put("image_url", imageUrl);
            contentParts.add(imageContent);

            // 文本部分 - 添加中文限制
            String finalPrompt = prompt != null ? prompt : "一段话描述这张图，20字以内";
            finalPrompt += "。要求：只使用中文，不要生成任何英文单词、字母或英文表达，用中文表达亲昵如\"亲爱的\"、\"宝贝\"、\"么么哒\"等。";
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", finalPrompt);
            contentParts.add(textContent);

            // 构建消息
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", contentParts);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 调用API
            String apiUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageResult = (Map<String, Object>) choice.get("message");
                    String result = (String) messageResult.get("content");
                    
                    logger.info("字节跳动API调用成功，返回结果: {}", result);
                    return result;
                } else {
                    throw new RuntimeException("字节跳动API返回空结果");
                }
            } else {
                throw new RuntimeException("字节跳动API调用失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("字节跳动API调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("字节跳动API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据文本生成AI文案（不需要图片）
     * @param prompt 提示词
     * @return 生成的文字描述
     */
    public String generateTextFromPrompt(String prompt) {
        if (!enabled) {
            logger.warn("字节跳动API未启用，enabled={}", enabled);
            throw new RuntimeException("字节跳动API未启用");
        }

        logger.info("字节跳动API配置检查 - enabled: {}, apiKey存在: {}, model: {}", 
                   enabled, (apiKey != null && !apiKey.trim().isEmpty()), model);

        try {
            logger.info("开始调用字节跳动纯文本生成API，输入prompt: {}", prompt);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 构建消息内容（只有文本，没有图片）
            List<Map<String, Object>> contentParts = new ArrayList<>();
            
            // 文本部分 - 添加中文限制
            String finalPrompt = prompt != null ? prompt : "生成一段优美的文案";
            finalPrompt += "。要求：只使用中文，不要生成任何英文单词、字母或英文表达，用中文表达亲昵如\"亲爱的\"、\"宝贝\"、\"么么哒\"等。";
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", finalPrompt);
            contentParts.add(textContent);

            // 构建消息
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", contentParts);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 调用API
            String apiUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
            logger.info("正在调用字节跳动API: {}", apiUrl);
            logger.info("请求体: {}", requestBody);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // 解析响应
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message_response = (Map<String, Object>) choice.get("message");
                    String content = (String) message_response.get("content");
                    
                    logger.info("字节跳动纯文本生成API调用成功，生成内容: {}", content);
                    return content.trim();
                }
                
                logger.warn("字节跳动API返回的choices为空");
                throw new RuntimeException("API返回数据格式异常");
            } else {
                logger.error("字节跳动纯文本生成API调用失败，状态码: {}", response.getStatusCode());
                throw new RuntimeException("字节跳动API调用失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("字节跳动纯文本生成API调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("字节跳动纯文本生成API调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * 检查字节跳动API是否可用
     */
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }
}
