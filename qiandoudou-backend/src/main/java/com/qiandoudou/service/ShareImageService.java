package com.qiandoudou.service;

import com.qiandoudou.entity.ShareImage;

/**
 * 分享图片服务接口
 */
public interface ShareImageService {

    /**
     * 获取钱兜兜分享图片
     */
    ShareImage getWalletShareImage();

    /**
     * 根据剧本ID获取剧本分享图片
     */
    ShareImage getScriptShareImage(Long scriptId);

    /**
     * 根据类型和目标ID获取分享图片
     */
    ShareImage getShareImage(String type, Long targetId);
}
