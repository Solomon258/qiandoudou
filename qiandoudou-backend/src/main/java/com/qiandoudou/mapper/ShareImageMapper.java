package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.ShareImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 分享图片 Mapper 接口
 */
@Mapper
public interface ShareImageMapper extends BaseMapper<ShareImage> {

    /**
     * 根据类型和目标ID获取分享图片
     */
    ShareImage selectByTypeAndTarget(@Param("type") String type, @Param("targetId") Long targetId);

    /**
     * 根据类型获取通用分享图片
     */
    ShareImage selectByType(@Param("type") String type);
}
