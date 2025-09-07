package com.qiandoudou.service;

import java.io.File;

/**
 * 静态资源上传服务接口
 */
public interface StaticResourceService {
    
    /**
     * 上传音频文件到静态资源库
     * @param audioFile 音频文件
     * @param fileName 文件名
     * @return 静态资源库中的文件URL
     */
    String uploadAudioFile(File audioFile, String fileName);
    
    /**
     * 上传视频文件到静态资源库
     * @param videoFile 视频文件
     * @param fileName 文件名
     * @return 静态资源库中的文件URL
     */
    String uploadVideoFile(File videoFile, String fileName);
    
    /**
     * 上传图片文件到静态资源库
     * @param imageFile 图片文件
     * @param fileName 文件名
     * @return 静态资源库中的文件URL
     */
    String uploadImageFile(File imageFile, String fileName);
    
    /**
     * 上传字节数组到静态资源库
     * @param fileData 文件数据
     * @param fileName 文件名
     * @param fileType 文件类型 (audio, video, image)
     * @return 静态资源库中的文件URL
     */
    String uploadFile(byte[] fileData, String fileName, String fileType);
}
