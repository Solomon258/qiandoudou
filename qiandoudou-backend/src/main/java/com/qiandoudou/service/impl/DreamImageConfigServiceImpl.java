package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiandoudou.entity.DreamImageConfig;
import com.qiandoudou.mapper.DreamImageConfigMapper;
import com.qiandoudou.service.DreamImageConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 梦想攒图片配置服务实现
 */
@Slf4j
@Service
public class DreamImageConfigServiceImpl extends ServiceImpl<DreamImageConfigMapper, DreamImageConfig> implements DreamImageConfigService {

    @Override
    public DreamImageConfig getByTypeAndName(Integer dreamType, String itemName) {
        if (dreamType == null || itemName == null) {
            log.warn("梦想类型或商品名称为空");
            return null;
        }

        QueryWrapper<DreamImageConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dream_type", dreamType)
                .eq("item_name", itemName)
                .eq("is_active", 1)
                .eq("deleted", 0);

        return this.getOne(queryWrapper);
    }

    @Override
    public List<DreamImageConfig> getActiveByType(Integer dreamType) {
        if (dreamType == null) {
            log.warn("梦想类型为空");
            return new ArrayList<>();
        }

        QueryWrapper<DreamImageConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dream_type", dreamType)
                .eq("is_active", 1)
                .eq("deleted", 0)
                .orderByAsc("item_name");

        return this.list(queryWrapper);
    }

    @Override
    public List<String> getProgressImages(Integer dreamType, String itemName) {
        List<String> progressImages = new ArrayList<>();

        DreamImageConfig config = this.getByTypeAndName(dreamType, itemName);
        if (config == null || config.getProgressImageBaseUrl() == null) {
            log.warn("未找到梦想类型{}、商品名称{}的配置，或进度图片URL为空", dreamType, itemName);
            return progressImages;
        }

        String baseUrl = config.getProgressImageBaseUrl();

        // 生成6张进度图片的URL，分别对应0-20%、20-40%、40-60%、60-80%、80-100%、100%
        // 支持三种格式：
        // 1. 包含 {{partX}} 模板变量的：https://oss.../image/dream/goods/car/{{partX}}.png
        // 2. 包含 {progress} 占位符的（兼容旧格式）：/static/icon/progress/progress-camera-{progress}.png
        // 3. 以 / 结尾的目录（兼容旧格式）：/static/icon/progress/progress-camera/
        for (int i = 1; i <= 6; i++) {
            String imageUrl;

            if (baseUrl.contains("{{partX}}")) {
                // 格式1：替换 {{partX}} 模板变量为实际进度编号（part1, part2, ..., part6）
                imageUrl = baseUrl.replace("{{partX}}", "part" + i);
                log.debug("使用{{partX}}模板格式，第{}张图片: {}", i, imageUrl);
            } else if (baseUrl.contains("{progress}")) {
                // 格式2：替换 {progress} 占位符为实际进度编号（1-6）
                imageUrl = baseUrl.replace("{progress}", String.valueOf(i));
                log.debug("使用{{progress}}占位符格式，第{}张图片: {}", i, imageUrl);
            } else {
                // 格式3：拼接目录和文件名
                // 去掉末尾的斜杠如果有的话
                String url = baseUrl;
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }
                imageUrl = url + "/part" + i + ".png";
                log.debug("使用目录拼接格式，第{}张图片: {}", i, imageUrl);
            }

            progressImages.add(imageUrl);
        }

        log.debug("生成进度图片列表，梦想类型: {}，商品名称: {}，图片列表: {}", dreamType, itemName, progressImages);
        return progressImages;
    }

    @Override
    public String getShareImage(Integer dreamType, String itemName) {
        DreamImageConfig config = this.getByTypeAndName(dreamType, itemName);
        if (config == null || config.getShareImageUrl() == null) {
            log.warn("未找到梦想类型{}、商品名称{}的分享图片配置", dreamType, itemName);
            return null;
        }

        log.debug("获取分享图片，梦想类型: {}，商品名称: {}，URL: {}", dreamType, itemName, config.getShareImageUrl());
        return config.getShareImageUrl();
    }
}
