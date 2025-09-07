package com.qiandoudou.service;

/**
 * 图生文服务接口
 */
public interface ImageToTextService {
    
    /**
     * 根据图片生成文字描述
     * @param imageBase64 图片的base64编码字符串
     * @param prompt 提示词
     * @return 生成的文字描述
     */
    String generateTextFromImage(String imageBase64, String prompt);
    
    /**
     * 根据文本生成AI文案（不需要图片）
     * @param prompt 提示词
     * @return 生成的文字描述
     */
    String generateTextFromPrompt(String prompt);
}
