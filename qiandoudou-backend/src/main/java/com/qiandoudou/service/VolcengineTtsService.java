package com.qiandoudou.service;

/**
 * 火山引擎TTS服务接口
 */
public interface VolcengineTtsService {
    
    /**
     * 使用火山引擎TTS生成语音
     * @param text 要转换的文本
     * @param voiceType 音色类型
     * @return 语音数据字节数组
     * @throws Exception TTS调用异常
     */
    byte[] generateVoice(String text, String voiceType) throws Exception;
    
    /**
     * 生成语音并上传到OSS
     * @param text 要转换的文本
     * @param voiceType 音色类型
     * @return 语音URL
     * @throws Exception TTS调用异常
     */
    String generateVoiceAndUpload(String text, String voiceType) throws Exception;
    
    /**
     * 检查火山引擎TTS服务是否可用
     * @return 是否可用
     */
    boolean isAvailable();
}
