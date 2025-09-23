package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.DreamItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 梦想商品配置Mapper接口
 */
@Mapper
public interface DreamItemMapper extends BaseMapper<DreamItem> {

    /**
     * 根据分类获取商品列表
     */
    List<DreamItem> selectByCategory(@Param("category") Integer category);

    /**
     * 搜索商品
     */
    List<DreamItem> searchItems(@Param("keyword") String keyword, @Param("category") Integer category);

    /**
     * 获取启用的商品列表
     */
    List<DreamItem> selectActiveItems(@Param("category") Integer category);
}
