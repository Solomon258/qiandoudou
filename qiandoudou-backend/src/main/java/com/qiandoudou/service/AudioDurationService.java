package com.qiandoudou.service;

/**
 * 音频时长解析服务接口
 */
public interface AudioDurationService {

    /**
     * 异步解析音频文件时长并更新到数据库
     * @param commentId 评论ID
     * @param voiceUrl 语音URL
     */
    void parseAndUpdateDurationAsync(Long commentId, String voiceUrl);

    /**
     * 从音频URL解析时长
     * @param voiceUrl 语音URL
     * @return 时长字符串，如"12s"
     */
    String parseAudioDurationFromUrl(String voiceUrl);

    /**
     * 从音频字节数组解析时长
     * @param audioData 音频数据
     * @param fileName 文件名（用于判断格式）
     * @return 时长字符串，如"12s"
     */
    String parseAudioDurationFromBytes(byte[] audioData, String fileName);
}
