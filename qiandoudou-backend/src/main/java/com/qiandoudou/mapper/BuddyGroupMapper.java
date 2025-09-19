package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.BuddyGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 搭子圈子 Mapper 接口
 */
@Mapper
public interface BuddyGroupMapper extends BaseMapper<BuddyGroup> {

    /**
     * 获取所有可用的搭子圈子
     */
    @Select("SELECT * FROM buddy_groups WHERE is_active = 1 AND deleted = 0 ORDER BY sort_order ASC")
    List<BuddyGroup> getActiveBuddyGroups();

    /**
     * 获取圈子详情（包含成员信息）
     */
    @Select("SELECT bg.*, " +
            "(SELECT COUNT(*) FROM buddy_characters bc WHERE bc.character_group_id = bg.id AND bc.is_active = 1 AND bc.deleted = 0) as actual_member_count " +
            "FROM buddy_groups bg " +
            "WHERE bg.id = #{groupId} AND bg.is_active = 1 AND bg.deleted = 0")
    Map<String, Object> getBuddyGroupDetail(Long groupId);
}
