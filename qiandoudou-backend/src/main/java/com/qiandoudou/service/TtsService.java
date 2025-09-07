package com.qiandoudou.service;

/**
 * TTS语音合成服务接口
 */
public interface TtsService {
    
    /**
     * 生成语音文件并上传到静态资源库
     * @param text 要转换的文本
     * @param voiceType 声音类型（配音女-奇妙栩等）
     * @return 语音文件在静态资源库中的URL
     */
    String generateVoiceAndUpload(String text, String voiceType);
    
    /**
     * 调用TTS API生成语音
     * @param text 要转换的文本
     * @param voiceType 声音类型
     * @return 语音文件的字节数组
     */
    byte[] generateVoice(String text, String voiceType);
}
