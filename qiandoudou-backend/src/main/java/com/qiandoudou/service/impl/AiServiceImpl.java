package com.qiandoudou.service.impl;

import com.qiandoudou.entity.AiPartner;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.AiPartnerService;
import com.qiandoudou.service.ImageToTextService;
import com.qiandoudou.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;

/**
 * AI服务实现类
 * 注意：这里只是预留接口，实际项目中需要集成真实的AI服务
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceImpl.class);

    @Value("${ai.api.base-url}")
    private String aiApiBaseUrl;

    @Value("${ai.api.api-key}")
    private String aiApiKey;

    @Autowired
    private AiPartnerService aiPartnerService;

    @Autowired
    private ImageToTextService imageToTextService;

    @Autowired
    private TtsService ttsService;

    @Override
    public String generateScriptContent(String theme, String previousContent, String userChoice) {
        // TODO: 集成OpenAI或其他AI服务生成剧本内容
        // 这里返回模拟内容
        return "根据你的选择：" + userChoice + "，故事继续发展...（这里是AI生成的剧本内容，实际项目中需要调用真实的AI API）";
    }

    @Override
    public String generateScriptImage(String content) {
        // TODO: 集成DALL-E或其他AI图片生成服务
        // 这里返回模拟图片URL
        return "/img/scripts/generated_" + System.currentTimeMillis() + ".jpg";
    }

    @Override
    public String generatePartnerComment(Long partnerId, String postContent) {
        try {
            AiPartner partner = aiPartnerService.getById(partnerId);
            if (partner == null) {
                return "亲爱的，你今天的表现真不错！";
            }

            // 根据AI伴侣的性格构建提示词
            String personality = partner.getPersonality();
            String name = partner.getName();
            
            String prompt = buildPromptForPartnerComment(name, personality, postContent);
            
            // 使用AI生成文案
            logger.info("开始调用AI生成文案，提示词: {}", prompt);
            String aiGeneratedText = generateAiText(prompt);
            logger.info("AI生成的原始文案: {}", aiGeneratedText);
            
            // 确保文案以伴侣名字开头
            if (!aiGeneratedText.startsWith(name + "：") && !aiGeneratedText.startsWith(name + ":")) {
                aiGeneratedText = name + "：" + aiGeneratedText;
            }
            
            logger.info("最终生成的文案: {}", aiGeneratedText);
            return aiGeneratedText;
            
        } catch (Exception e) {
            logger.error("AI文案生成失败: {}", e.getMessage(), e);
            // 如果AI生成失败，回退到原有逻辑
            return generateFallbackComment(partnerId);
        }
    }

    /**
     * 构建AI伴侣评论的提示词
     */
    private String buildPromptForPartnerComment(String name, String personality, String postContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个名叫").append(name).append("的AI伴侣，性格特点是").append(personality).append("。");
        prompt.append("你的伴侣刚刚进行了一次储蓄行为：").append(postContent).append("。");
        prompt.append("请以").append(name).append("的身份，用").append(personality).append("的语气，");
        prompt.append("对伴侣的储蓄行为给出一句温馨的评论或鼓励。");
        prompt.append("要求：1）50字以内；2）语气要符合性格特点；3）内容要与储蓄相关；4）要体现情侣间的亲密关系。");
        prompt.append("请直接返回评论内容，不要加任何前缀或后缀说明。");
        
        return prompt.toString();
    }

    /**
     * 生成回退评论（当AI生成失败时使用）
     */
    private String generateFallbackComment(Long partnerId) {
        try {
            AiPartner partner = aiPartnerService.getById(partnerId);
            if (partner == null) {
                return getRandomFallbackMessage("亲爱的");
            }

            String personality = partner.getPersonality();
            String name = partner.getName();

            if (personality.contains("温柔")) {
                String[] messages = {
                    "亲爱的，看到你这样努力储蓄，我真的很开心呢～继续加油哦！💕",
                    "宝贝，你的每一次储蓄都让我感到骄傲，我们一起向目标努力吧～",
                    "亲爱的，你又存钱了呢，真是个勤劳的小蜜蜂～我爱你！",
                    "看到你这么用心理财，我的心都要化了～你真棒！💕",
                    "宝贝，你的储蓄习惯真让人欣慰，我们的未来会更美好的～"
                };
                return name + "：" + getRandomMessage(messages);
            } else if (personality.contains("幽默")) {
                String[] messages = {
                    "哇，又存钱了！看来我们离财务自由又近了一步，今晚庆祝一下？😄",
                    "储蓄小能手上线了！这样下去我们很快就能买个小岛了～😂",
                    "恭喜你又完成了一次'把钱藏起来'的任务！奖励你一个拥抱～",
                    "看来今天的你是'省钱小天才'模式啊，厉害厉害！👏",
                    "又存钱啦？你这是要承包整个银行吗？哈哈～"
                };
                return name + "：" + getRandomMessage(messages);
            } else if (personality.contains("可爱")) {
                String[] messages = {
                    "好棒好棒！你是最厉害的储蓄小能手！✨",
                    "哇～又存钱钱了！你真是个小财迷呢，好可爱～💕",
                    "储蓄星人又在行动了！你真的超级棒棒哒！🌟",
                    "小金库又有新成员啦～你真是理财小达人呢！",
                    "哇塞！你又存钱了耶～我要给你点一万个赞！👍"
                };
                return name + "：" + getRandomMessage(messages);
            } else {
                String[] messages = {
                    "很好的理财决策，这样的习惯值得坚持。",
                    "理性的储蓄规划，你的财务管理能力很出色。",
                    "持续的储蓄行为体现了你的自律性，值得称赞。",
                    "良好的储蓄习惯是财富积累的基础，继续保持。",
                    "你的储蓄计划执行得很好，这是成功的重要一步。"
                };
                return name + "：" + getRandomMessage(messages);
            }
        } catch (Exception e) {
            return getRandomFallbackMessage("亲爱的");
        }
    }

    /**
     * 从消息数组中随机选择一条
     */
    private String getRandomMessage(String[] messages) {
        int index = (int) (Math.random() * messages.length);
        return messages[index];
    }

    /**
     * 获取随机的默认消息
     */
    private String getRandomFallbackMessage(String name) {
        String[] messages = {
            "你今天的表现真不错！",
            "看到你这么努力存钱，我很开心！",
            "你的储蓄习惯真棒，继续保持！",
            "又存钱了呢，你真是个理财小能手！",
            "每一次储蓄都是向梦想迈进的一步！"
        };
        return name + "，" + getRandomMessage(messages);
    }

    @Override
    public String generatePartnerVoice(Long partnerId, String text) {
        try {
            // 获取AI伴侣信息，确定声音类型
            AiPartner partner = aiPartnerService.getById(partnerId);
            String voiceType = "奇妙栩"; // 默认声音类型
            
            if (partner != null) {
                // 根据AI伴侣的性格选择合适的声音类型
                String personality = partner.getPersonality();
                if (personality.contains("温柔")) {
                    voiceType = "温柔女声";
                } else if (personality.contains("甜美") || personality.contains("可爱")) {
                    voiceType = "甜美女声";
                } else if (personality.contains("成熟")) {
                    voiceType = "成熟男声";
                } else if (personality.contains("阳光")) {
                    voiceType = "阳光男声";
                }
                // 默认使用奇妙栩
            }
            
            // 清理文本，去掉名字前缀
            String cleanText = text;
            if (partner != null) {
                String name = partner.getName();
                if (cleanText.startsWith(name + "：")) {
                    cleanText = cleanText.substring((name + "：").length()).trim();
                } else if (cleanText.startsWith(name + ":")) {
                    cleanText = cleanText.substring((name + ":").length()).trim();
                }
            }
            
            // 调用TTS服务生成语音并上传
            return ttsService.generateVoiceAndUpload(cleanText, voiceType);
            
        } catch (Exception e) {
            // 如果TTS生成失败，返回null或空字符串，表示没有语音
            return null;
        }
    }

    @Override
    public String generateTextFromImage(String imageUrl) {
        try {
            // 1. 先将图片URL转换为base64
            String imageBase64 = convertImageUrlToBase64(imageUrl);
            
            // 2. 调用图生文API
            String prompt = "你是一个朋友圈文案助手，根据图片生成朋友圈的文案，少于100字，不要生成其他内容,不要思考太久";
            return imageToTextService.generateTextFromImage(imageBase64, prompt);
        } catch (Exception e) {
            // 如果调用失败，返回默认描述
            return "这是一张很棒的图片，记录了美好的时刻！";
        }
    }

    /**
     * 将图片URL转换为base64编码
     */
    private String convertImageUrlToBase64(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            try (InputStream inputStream = url.openStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] imageBytes = outputStream.toByteArray();
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("图片转换为base64失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateTextFromImageBase64(String imageBase64, String prompt) {
        return imageToTextService.generateTextFromImage(imageBase64, prompt);
    }

    @Override
    public String generateAiText(String prompt) {
        return imageToTextService.generateTextFromPrompt(prompt);
    }
}
