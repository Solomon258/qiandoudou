package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiandoudou.entity.ShareImage;
import com.qiandoudou.mapper.ShareImageMapper;
import com.qiandoudou.service.ShareImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分享图片服务实现类
 */
@Service
@Slf4j
public class ShareImageServiceImpl implements ShareImageService {

    @Autowired
    private ShareImageMapper shareImageMapper;

    @Override
    public ShareImage getWalletShareImage() {
        log.info("获取钱兜兜分享图片");
        
        // 查询钱兜兜通用分享图片
        QueryWrapper<ShareImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", "wallet")
                   .isNull("target_id")
                   .eq("status", 1)
                   .orderByDesc("create_time")
                   .last("LIMIT 1");
        
        ShareImage shareImage = shareImageMapper.selectOne(queryWrapper);
        
        if (shareImage == null) {
            log.warn("未找到钱兜兜分享图片，创建默认分享图片");
            // 如果没有找到，返回默认的分享图片
            shareImage = new ShareImage();
            shareImage.setType("wallet");
            shareImage.setImageUrl("https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/include_images/share_qiandoudou.png");
            shareImage.setTitle("钱兜兜分享");
            shareImage.setDescription("快来看看我的钱兜兜！");
        }
        
        return shareImage;
    }

    @Override
    public ShareImage getScriptShareImage(Long scriptId) {
        log.info("获取剧本分享图片，scriptId: {}", scriptId);
        
        if (scriptId == null) {
            return null;
        }
        
        // 先查询特定剧本的分享图片
        QueryWrapper<ShareImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", "script")
                   .eq("target_id", scriptId)
                   .eq("status", 1)
                   .orderByDesc("create_time")
                   .last("LIMIT 1");
        
        ShareImage shareImage = shareImageMapper.selectOne(queryWrapper);
        
        if (shareImage == null) {
            log.warn("未找到剧本ID: {} 的分享图片", scriptId);
            // 如果没有找到特定剧本的分享图片，查询通用剧本分享图片
            queryWrapper.clear();
            queryWrapper.eq("type", "script")
                       .isNull("target_id")
                       .eq("status", 1)
                       .orderByDesc("create_time")
                       .last("LIMIT 1");
            
            shareImage = shareImageMapper.selectOne(queryWrapper);
        }
        
        return shareImage;
    }

    @Override
    public ShareImage getShareImage(String type, Long targetId) {
        log.info("获取分享图片，type: {}, targetId: {}", type, targetId);
        
        if ("wallet".equals(type)) {
            return getWalletShareImage();
        } else if ("script".equals(type)) {
            return getScriptShareImage(targetId);
        }
        
        return null;
    }
}
