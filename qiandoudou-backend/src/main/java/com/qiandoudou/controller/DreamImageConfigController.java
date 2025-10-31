package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.entity.DreamImageConfig;
import com.qiandoudou.service.DreamImageConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 梦想攒图片配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/dream-image-config")
public class DreamImageConfigController {

    @Autowired
    private DreamImageConfigService dreamImageConfigService;

    /**
     * 根据梦想类型和商品名称获取图片配置
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @param itemName 商品或目的地名称
     * @return 图片配置
     */
    @GetMapping("/get")
    public Result<DreamImageConfig> getImageConfig(
            @RequestParam Integer dreamType,
            @RequestParam String itemName) {
        try {
            DreamImageConfig config = dreamImageConfigService.getByTypeAndName(dreamType, itemName);
            if (config == null) {
                return Result.error("未找到图片配置");
            }
            return Result.success("获取成功", config);
        } catch (Exception e) {
            log.error("获取图片配置失败: {}", e.getMessage(), e);
            return Result.error("获取图片配置失败");
        }
    }

    /**
     * 获取进度图片列表
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @param itemName 商品或目的地名称
     * @return 5张进度图片的URL列表
     */
    @GetMapping("/progress-images")
    public Result<List<String>> getProgressImages(
            @RequestParam Integer dreamType,
            @RequestParam String itemName) {
        try {
            List<String> progressImages = dreamImageConfigService.getProgressImages(dreamType, itemName);
            if (progressImages == null || progressImages.isEmpty()) {
                return Result.error("未找到进度图片");
            }
            return Result.success("获取成功", progressImages);
        } catch (Exception e) {
            log.error("获取进度图片失败: {}", e.getMessage(), e);
            return Result.error("获取进度图片失败");
        }
    }

    /**
     * 获取分享图片URL
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @param itemName 商品或目的地名称
     * @return 分享图片URL
     */
    @GetMapping("/share-image")
    public Result<String> getShareImage(
            @RequestParam Integer dreamType,
            @RequestParam String itemName) {
        try {
            String shareImage = dreamImageConfigService.getShareImage(dreamType, itemName);
            if (shareImage == null) {
                return Result.error("未找到分享图片");
            }
            return Result.success("获取成功", shareImage);
        } catch (Exception e) {
            log.error("获取分享图片失败: {}", e.getMessage(), e);
            return Result.error("获取分享图片失败");
        }
    }

    /**
     * 获取梦想类型的所有启用的图片配置
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @return 图片配置列表
     */
    @GetMapping("/list/{dreamType}")
    public Result<List<DreamImageConfig>> getActiveByType(@PathVariable Integer dreamType) {
        try {
            List<DreamImageConfig> configs = dreamImageConfigService.getActiveByType(dreamType);
            return Result.success("获取成功", configs);
        } catch (Exception e) {
            log.error("获取图片配置列表失败: {}", e.getMessage(), e);
            return Result.error("获取图片配置列表失败");
        }
    }

    /**
     * 获取梦想详情页所需的完整图片信息
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @param itemName 商品或目的地名称
     * @return 包含进度图片和分享图片的完整数据
     */
    @GetMapping("/complete-info")
    public Result<Map<String, Object>> getCompleteImageInfo(
            @RequestParam Integer dreamType,
            @RequestParam String itemName) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取进度图片列表
            List<String> progressImages = dreamImageConfigService.getProgressImages(dreamType, itemName);
            result.put("progressImages", progressImages);

            // 获取分享图片
            String shareImage = dreamImageConfigService.getShareImage(dreamType, itemName);
            result.put("shareImage", shareImage);

            // 获取完整配置信息
            DreamImageConfig config = dreamImageConfigService.getByTypeAndName(dreamType, itemName);
            if (config != null) {
                result.put("config", config);
            }

            return Result.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取完整图片信息失败: {}", e.getMessage(), e);
            return Result.error("获取完整图片信息失败");
        }
    }
}
