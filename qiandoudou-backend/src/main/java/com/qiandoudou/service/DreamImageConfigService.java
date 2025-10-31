package com.qiandoudou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiandoudou.entity.DreamImageConfig;

import java.util.List;

/**
 * 梦想攒图片配置服务接口
 */
public interface DreamImageConfigService extends IService<DreamImageConfig> {

    /**
     * 根据梦想类型和商品名称查询图片配置
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @param itemName 商品或目的地名称
     * @return 梦想图片配置
     */
    DreamImageConfig getByTypeAndName(Integer dreamType, String itemName);

    /**
     * 获取梦想类型的所有启用的图片配置
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @return 图片配置列表
     */
    List<DreamImageConfig> getActiveByType(Integer dreamType);

    /**
     * 获取进度图片列表
     * 根据进度百分比返回对应的5张进度图片URL
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @param itemName 商品或目的地名称
     * @return 5张进度图片的URL数组，分别对应20%、40%、60%、80%、100%
     */
    List<String> getProgressImages(Integer dreamType, String itemName);

    /**
     * 获取分享图片URL
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @param itemName 商品或目的地名称
     * @return 分享图片URL
     */
    String getShareImage(Integer dreamType, String itemName);
}
