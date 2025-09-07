package com.qiandoudou.service;

/**
 * AI服务接口
 */
public interface AiService {

    /**
     * 生成剧本章节内容
     */
    String generateScriptContent(String theme, String previousContent, String userChoice);

    /**
     * 生成剧本章节图片
     */
    String generateScriptImage(String content);

    /**
     * 生成AI伴侣评论
     */
    String generatePartnerComment(Long partnerId, String postContent);

    /**
     * 生成AI伴侣评论（基于交易信息）
     */
    String generatePartnerComment(String transactionType, String description, Double amount);

    /**
     * 生成AI伴侣语音
     */
    String generatePartnerVoice(Long partnerId, String text);

    /**
     * 根据图片生成文字描述
     */
    String generateTextFromImage(String imageUrl);

    /**
     * 根据图片base64生成文字描述
     */
    String generateTextFromImageBase64(String imageBase64, String prompt);
    
    /**
     * 生成AI文案（不需要图片）
     */
    String generateAiText(String prompt);
}
