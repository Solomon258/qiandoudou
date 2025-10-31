package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.DreamImageConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 梦想攒图片配置Mapper
 */
@Mapper
public interface DreamImageConfigMapper extends BaseMapper<DreamImageConfig> {

    /**
     * 根据梦想类型和商品名称查询图片配置
     *
     * @param dreamType 梦想类型：1-购物，2-旅游
     * @param itemName 商品或目的地名称
     * @return 梦想图片配置
     */
    DreamImageConfig selectByTypeAndName(@Param("dreamType") Integer dreamType, @Param("itemName") String itemName);
}
