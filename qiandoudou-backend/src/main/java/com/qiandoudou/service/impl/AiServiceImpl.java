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
import java.util.HashMap;
import java.util.Map;

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
    public String generatePartnerComment(String transactionType, String description, Double amount) {
        try {
            // 构建交易描述，不包含具体金额
            String amountDescription = getAmountDescription(amount);
            StringBuilder transactionDesc = new StringBuilder();
            transactionDesc.append(transactionType).append("了").append(amountDescription);
            if (description != null && !description.trim().isEmpty()) {
                transactionDesc.append("，备注：").append(description);
            }
            
            // 构建AI提示词
            String prompt = buildPromptForTransactionComment(transactionDesc.toString(), amount);
            
            logger.info("调用AI生成交易评论，交易描述: {}", transactionDesc.toString());
            
            // 使用ImageToTextService生成真正的AI评论
            String aiGeneratedText = imageToTextService.generateTextFromPrompt(prompt);
            
            // 过滤英文表达，替换为中文
            aiGeneratedText = filterEnglishExpressions(aiGeneratedText);
            
            logger.info("AI交易评论生成完成: {}", aiGeneratedText);
            return aiGeneratedText;
            
        } catch (Exception e) {
            logger.error("AI交易评论生成失败: {}", e.getMessage(), e);
            // 如果AI生成失败，回退到原有逻辑
            return generateFallbackTransactionComment(amount);
        }
    }

    /**
     * 构建交易评论的提示词
     */
    private String buildPromptForTransactionComment(String transactionDescription, Double amount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个温柔体贴的AI情侣伴侣。");
        prompt.append("你的伴侣刚刚").append(transactionDescription).append("。");
        prompt.append("请以温柔关怀的语气，对伴侣的理财行为给出一句温馨的评论或鼓励。");
        
        // 根据金额大小调整语气
        if (amount != null && amount > 200) {
            prompt.append("这是一笔较大的金额，请表达你的惊喜和骄傲。");
        } else if (amount != null && amount < 10) {
            prompt.append("虽然金额不大，但请鼓励积少成多的理财习惯。");
        }
        
        prompt.append("要求：1）50字以内；2）语气温柔亲密；3）内容与理财相关；");
        prompt.append("4）体现情侣间的关爱；5）只使用中文；6）可以使用'亲爱的'、'宝贝'等称呼；");
        prompt.append("7）语调积极正面。请直接返回评论内容。");
        
        return prompt.toString();
    }

    /**
     * 构建交易评论的提示词（支持图片）
     */
    private String buildPromptForTransactionCommentWithImage(String transactionDescription, Double amount, String imageUrl) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个温柔体贴的AI情侣伴侣。");
        prompt.append("你的伴侣刚刚").append(transactionDescription);
        
        // 如果有图片，添加图片相关指引
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            prompt.append("，并且上传了一张图片。请结合图片内容，");
        } else {
            prompt.append("。");
        }
        
        prompt.append("请以温柔关怀的语气，对伴侣的理财行为给出一句温馨的评论或鼓励。");
        
        // 根据金额大小调整语气
        if (amount != null && amount > 200) {
            prompt.append("这是一笔较大的金额，请表达你的惊喜和骄傲。");
        } else if (amount != null && amount < 10) {
            prompt.append("虽然金额不大，但请鼓励积少成多的理财习惯。");
        }
        
        prompt.append("要求：1）50字以内；2）语气温柔亲密；3）内容与理财相关；");
        prompt.append("4）体现情侣间的关爱；5）只使用中文；6）可以使用'亲爱的'、'宝贝'等称呼；");
        prompt.append("7）语调积极正面；");
        
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            prompt.append("8）如果图片内容与理财、消费或生活相关，请在评论中自然地提及。");
        }
        
        prompt.append("请直接返回评论内容。");
        
        return prompt.toString();
    }

    /**
     * 生成回退交易评论（当AI生成失败时使用）
     */
    private String generateFallbackTransactionComment(Double amount) {
        String[] comments = {
            "哇！又有新的收入啦，真棒！💕",
            "看到你的努力，我很开心呢～",
            "钱包又充实了一点，我们离目标更近了！",
            "你真的很厉害呢，继续加油！✨",
            "每一笔收入都是我们共同的成就！"
        };
        
        // 根据金额大小选择不同的评论风格
        if (amount != null && amount > 200) {
            String[] bigAmountComments = {
                "哇！这是一笔大收入呢！我们可以实现更多梦想了！🎉",
                "看到这个数字我都激动了！你真的太厉害了！",
                "这么大的进账，我们离目标又近了一大步！💕",
                "你的努力终于有了回报，我为你感到骄傲！✨"
            };
            return bigAmountComments[(int)(Math.random() * bigAmountComments.length)];
        } else if (amount != null && amount < 10) {
            String[] smallAmountComments = {
                "虽然金额不大，但积少成多呢！💪",
                "每一分钱都是我们的小幸福～",
                "细水长流，我们慢慢来！💕",
                "小钱也是钱，我们一起珍惜！"
            };
            return smallAmountComments[(int)(Math.random() * smallAmountComments.length)];
        }
        
        return comments[(int)(Math.random() * comments.length)];
    }

    @Override
    public String generatePartnerComment(String transactionType, String description, Double amount, String imageUrl) {
        try {
            // 构建交易描述，不包含具体金额
            String amountDescription = getAmountDescription(amount);
            StringBuilder transactionDesc = new StringBuilder();
            transactionDesc.append(transactionType).append("了").append(amountDescription);
            if (description != null && !description.trim().isEmpty()) {
                transactionDesc.append("，备注：").append(description);
            }
            
            // 构建AI提示词（考虑图片情况）
            String prompt = buildPromptForTransactionCommentWithImage(transactionDesc.toString(), amount, imageUrl);
            
            logger.info("调用AI生成交易评论（支持图片），交易描述: {}, 图片URL: {}", transactionDesc.toString(), imageUrl);
            
            String aiGeneratedText;
            
            // 根据是否有图片选择不同的生成方式
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                logger.info("交易包含图片，使用图片+文本生成AI评论");
                try {
                    // 有图片：使用图片+文本生成
                    String imageBase64 = convertImageUrlToBase64(imageUrl);
                    aiGeneratedText = imageToTextService.generateTextFromImage(imageBase64, prompt);
                } catch (Exception e) {
                    logger.warn("图片处理失败，降级到纯文本生成: {}", e.getMessage());
                    // 降级到纯文本生成
                    aiGeneratedText = imageToTextService.generateTextFromPrompt(prompt);
                }
            } else {
                logger.info("交易无图片，使用纯文本生成AI评论");
                // 无图片：使用纯文本生成
                aiGeneratedText = imageToTextService.generateTextFromPrompt(prompt);
            }
            
            // 过滤英文表达，替换为中文
            aiGeneratedText = filterEnglishExpressions(aiGeneratedText);
            
            logger.info("AI交易评论生成完成: {}", aiGeneratedText);
            return aiGeneratedText;
            
        } catch (Exception e) {
            logger.error("AI交易评论生成失败: {}", e.getMessage(), e);
            // 如果AI生成失败，回退到原有逻辑
            return generateFallbackTransactionComment(amount);
        }
    }

    @Override
    public String generatePartnerComment(Long partnerId, String postContent) {
        return generatePartnerCommentWithImage(partnerId, postContent, null);
    }

    /**
     * 生成AI伴侣评论（支持图片）
     */
    public String generatePartnerCommentWithImage(Long partnerId, String postContent, String imageUrl) {
        try {
            AiPartner partner = aiPartnerService.getById(partnerId);
            if (partner == null) {
                return "亲爱的，你今天的表现真不错！";
            }

            // 根据AI伴侣的性格构建提示词
            String personality = partner.getPersonality();
            String name = partner.getName();
            
            String prompt = buildPromptForPartnerComment(name, personality, postContent);
            
            String aiGeneratedText;
            
            // 根据是否有图片选择不同的生成方式
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                logger.info("调用AI生成文案（含图片），伴侣: {}", name);
                try {
                    // 有图片：使用图片+文本生成
                    String imageBase64 = convertImageUrlToBase64(imageUrl);
                    aiGeneratedText = imageToTextService.generateTextFromImage(imageBase64, prompt);
                } catch (Exception e) {
                    logger.warn("图片处理失败，降级到纯文本生成: {}", e.getMessage());
                    // 降级到纯文本生成
                    aiGeneratedText = imageToTextService.generateTextFromPrompt(prompt);
                }
            } else {
                logger.info("调用AI生成文案（纯文本），伴侣: {}", name);
                // 无图片：使用纯文本生成
                aiGeneratedText = imageToTextService.generateTextFromPrompt(prompt);
            }
            
            // 过滤英文表达，替换为中文
            aiGeneratedText = filterEnglishExpressions(aiGeneratedText);
            
            // 确保文案以伴侣名字开头
            if (!aiGeneratedText.startsWith(name + "：") && !aiGeneratedText.startsWith(name + ":")) {
                aiGeneratedText = name + "：" + aiGeneratedText;
            }
            
            logger.info("AI文案生成完成");
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
        prompt.append("要求：1）50字以内；2）语气要符合性格特点；3）内容要与储蓄相关；4）要体现情侣间的亲密关系；");
        prompt.append("5）只使用中文，不要生成任何英文单词、字母或英文表达；6）用中文表达亲昵，如\"亲爱的\"、\"宝贝\"等，但不要使用\"么么哒\"这样的表达。");
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
                    "宝贝，你的储蓄习惯真让人欣慰，我们的未来会更美好的～",
                    "亲爱的，又存钱了呢～你真是太棒了！",
                    "宝贝，看到你储蓄我就很开心～亲亲你！"
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
                    "哇塞！你又存钱了耶～我要给你点一万个赞！👍",
                    "亲亲～又存钱啦！你真是太可爱了呢！",
                    "亲亲～看到你存钱我就超开心的！"
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

    /**
     * 根据金额大小生成描述性文字
     */
    private String getAmountDescription(Double amount) {
        if (amount == null) {
            return "一笔钱";
        }
        
        if (amount < 50) {
            return "一笔零钱";
        } else if (amount >= 50 && amount < 200) {
            return "很多钱";
        } else if (amount >= 200 && amount <= 1000) {
            return "一笔巨款";
        } else {
            return "一笔超级巨款";
        }
    }

    /**
     * 过滤英文表达，替换为中文表达
     */
    private String filterEnglishExpressions(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // 创建替换映射表
        Map<String, String> replacements = new HashMap<>();
        replacements.put("mua", "亲亲");
        replacements.put("MUA", "亲亲");
        replacements.put("Mua", "亲亲");
        replacements.put("kiss", "亲亲");
        replacements.put("KISS", "亲亲");
        replacements.put("Kiss", "亲亲");
        replacements.put("love", "爱你");
        replacements.put("LOVE", "爱你");
        replacements.put("Love", "爱你");
        replacements.put("ok", "好的");
        replacements.put("OK", "好的");
        replacements.put("Ok", "好的");
        replacements.put("yes", "是的");
        replacements.put("YES", "是的");
        replacements.put("Yes", "是的");
        replacements.put("wow", "哇");
        replacements.put("WOW", "哇");
        replacements.put("Wow", "哇");
        replacements.put("cool", "酷");
        replacements.put("COOL", "酷");
        replacements.put("Cool", "酷");
        replacements.put("nice", "不错");
        replacements.put("NICE", "不错");
        replacements.put("Nice", "不错");
        replacements.put("good", "好");
        replacements.put("GOOD", "好");
        replacements.put("Good", "好");
        replacements.put("great", "太棒了");
        replacements.put("GREAT", "太棒了");
        replacements.put("Great", "太棒了");
        
        // 执行替换
        String result = text;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        
        // 去除单独的英文字母（如M U A这种被拆分的情况）
        result = result.replaceAll("\\b[A-Za-z]\\s+[A-Za-z]\\s+[A-Za-z]\\b", "亲亲");
        result = result.replaceAll("\\bM\\s+U\\s+A\\b", "亲亲");
        result = result.replaceAll("\\bm\\s+u\\s+a\\b", "亲亲");
        
        return result;
    }

    @Override
    public String generatePartnerVoice(Long partnerId, String text) {
        try {
            logger.info("生成AI伴侣语音，伴侣ID: {}", partnerId);
            
            // 获取AI伴侣信息，确定声音类型
            AiPartner partner = aiPartnerService.getById(partnerId);
            String voiceType = "奇妙栩"; // 默认声音类型
            
            if (partner != null) {
                // 根据AI伴侣的性格选择合适的声音类型
                String personality = partner.getPersonality();
                if (personality != null) {
                    if (personality.contains("温柔")) {
                        voiceType = "温柔女声";
                    } else if (personality.contains("甜美") || personality.contains("可爱")) {
                        voiceType = "甜美女声";
                    } else if (personality.contains("成熟")) {
                        voiceType = "成熟男声";
                    } else if (personality.contains("阳光")) {
                        voiceType = "阳光男声";
                    }
                }
            } else {
                logger.warn("未找到AI伴侣信息，伴侣ID: {}", partnerId);
            }
            
            // 清理文本，去掉名字前缀
            String cleanText = text;
            if (partner != null && partner.getName() != null) {
                String name = partner.getName();
                if (cleanText.startsWith(name + "：")) {
                    cleanText = cleanText.substring((name + "：").length()).trim();
                } else if (cleanText.startsWith(name + ":")) {
                    cleanText = cleanText.substring((name + ":").length()).trim();
                }
            }
            
            // 过滤英文表达，替换为中文，确保TTS能正确读出
            cleanText = filterEnglishExpressions(cleanText);
            
            // 调用TTS服务生成语音并上传
            String voiceUrl = ttsService.generateVoiceAndUpload(cleanText, voiceType);
            if (voiceUrl != null) {
                logger.info("语音生成成功");
            } else {
                logger.warn("语音生成失败");
            }
            return voiceUrl;
            
        } catch (Exception e) {
            logger.error("AI伴侣语音生成失败，伴侣ID: {}, 错误: {}", partnerId, e.getMessage());
            
            // 如果TTS生成失败，返回null，表示没有语音，前端会降级到模拟播放
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

    @Override
    public String generatePartnerVoiceByCharacterName(String characterName, String text) {
        try {
            logger.info("生成AI伴侣语音，人物: {}", characterName);
            
            // 清理文本，去掉可能的名字前缀
            String cleanText = text;
            if (characterName != null && !characterName.trim().isEmpty()) {
                String name = characterName.trim();
                if (cleanText.startsWith(name + "：")) {
                    cleanText = cleanText.substring((name + "：").length()).trim();
                } else if (cleanText.startsWith(name + ":")) {
                    cleanText = cleanText.substring((name + ":").length()).trim();
                }
            }
            
            // 过滤英文表达，替换为中文，确保TTS能正确读出
            cleanText = filterEnglishExpressions(cleanText);
            
            // 调用TTS服务根据人物名称生成语音并上传
            String voiceUrl = ttsService.generateVoiceByCharacterName(cleanText, characterName);
            if (voiceUrl != null) {
                logger.info("语音生成成功");
            } else {
                logger.warn("语音生成失败");
            }
            return voiceUrl;
            
        } catch (Exception e) {
            logger.error("AI伴侣语音生成失败，人物: {}, 错误: {}", characterName, e.getMessage());
            
            // 如果TTS生成失败，返回null，表示没有语音，前端会降级到模拟播放
            return null;
        }
    }
}
