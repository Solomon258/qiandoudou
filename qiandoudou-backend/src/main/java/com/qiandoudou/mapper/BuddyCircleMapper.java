package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.BuddyCircle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 搭子圈子 Mapper 接口
 */
@Mapper
public interface BuddyCircleMapper extends BaseMapper<BuddyCircle> {

    /**
     * 获取启用的圈子列表，按排序权重排序
     */
    @Select("SELECT * FROM buddy_circles WHERE is_active = 1 AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<BuddyCircle> getActiveCircles();
}
