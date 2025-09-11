package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.entity.ShareImage;
import com.qiandoudou.service.ShareImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 分享图片控制器
 */
@RestController
@RequestMapping("/share")
@Slf4j
@CrossOrigin(origins = "*") // 添加跨域支持
public class ShareImageController {

    @Autowired
    private ShareImageService shareImageService;

    /**
     * 获取钱兜兜分享图片
     */
    @GetMapping("/wallet")
    public Result<ShareImage> getWalletShareImage() {
        log.info("收到获取钱兜兜分享图片请求");
        try {
            ShareImage shareImage = shareImageService.getWalletShareImage();
            log.info("获取到分享图片: {}", shareImage);
            if (shareImage == null) {
                log.warn("未找到钱兜兜分享图片");
                return Result.error("未找到分享图片");
            }
            return Result.success(shareImage);
        } catch (Exception e) {
            log.error("获取钱兜兜分享图片失败", e);
            return Result.error("获取分享图片失败");
        }
    }

    /**
     * 获取剧本分享图片
     */
    @GetMapping("/script/{scriptId}")
    public Result<ShareImage> getScriptShareImage(@PathVariable Long scriptId) {
        log.info("收到获取剧本分享图片请求，scriptId: {}", scriptId);
        try {
            ShareImage shareImage = shareImageService.getScriptShareImage(scriptId);
            log.info("获取到剧本分享图片: {}", shareImage);
            if (shareImage == null) {
                log.warn("未找到剧本ID: {} 的分享图片", scriptId);
                return Result.error("未找到该剧本的分享图片");
            }
            return Result.success(shareImage);
        } catch (Exception e) {
            log.error("获取剧本分享图片失败", e);
            return Result.error("获取分享图片失败");
        }
    }

    /**
     * 根据类型和目标ID获取分享图片
     */
    @GetMapping("/{type}")
    public Result<ShareImage> getShareImage(
            @PathVariable String type,
            @RequestParam(required = false) Long targetId) {
        log.info("获取分享图片，type: {}, targetId: {}", type, targetId);
        try {
            ShareImage shareImage = shareImageService.getShareImage(type, targetId);
            if (shareImage == null) {
                return Result.error("未找到分享图片");
            }
            return Result.success(shareImage);
        } catch (Exception e) {
            log.error("获取分享图片失败", e);
            return Result.error("获取分享图片失败");
        }
    }
}
