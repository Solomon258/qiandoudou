package com.qiandoudou.service;

import java.io.File;

/**
 * 阿里云OSS文件上传服务接口
 */
public interface OssService {
    
    /**
     * 上传音频文件到OSS
     * @param audioFile 音频文件
     * @param fileName 文件名
     * @return OSS中的文件URL
     */
    String uploadAudioFile(File audioFile, String fileName);
    
    /**
     * 上传视频文件到OSS
     * @param videoFile 视频文件
     * @param fileName 文件名
     * @return OSS中的文件URL
     */
    String uploadVideoFile(File videoFile, String fileName);
    
    /**
     * 上传图片文件到OSS
     * @param imageFile 图片文件
     * @param fileName 文件名
     * @return OSS中的文件URL
     */
    String uploadImageFile(File imageFile, String fileName);
    
    /**
     * 上传用户图片文件到OSS用户专用目录
     * @param imageFile 图片文件
     * @param fileName 文件名
     * @return OSS中的文件URL
     */
    String uploadUserImageFile(File imageFile, String fileName);
    
    /**
     * 上传字节数组到OSS
     * @param fileData 文件数据
     * @param fileName 文件名
     * @param fileType 文件类型 (audio, video, image, user_image)
     * @return OSS中的文件URL
     */
    String uploadFile(byte[] fileData, String fileName, String fileType);
    
    /**
     * 上传用户图片字节数组到OSS用户专用目录
     * @param fileData 图片文件数据
     * @param fileName 文件名
     * @return OSS中的文件URL
     */
    String uploadUserImageData(byte[] fileData, String fileName);
    
    /**
     * 删除OSS中的文件
     * @param objectKey 文件在OSS中的key
     * @return 是否删除成功
     */
    boolean deleteFile(String objectKey);
}
