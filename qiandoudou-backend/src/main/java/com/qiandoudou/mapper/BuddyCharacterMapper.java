package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.BuddyCharacter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 搭子角色 Mapper 接口
 */
@Mapper
public interface BuddyCharacterMapper extends BaseMapper<BuddyCharacter> {

    /**
     * 获取所有可用的自由选择搭子角色
     */
    @Select("SELECT * FROM buddy_characters WHERE character_group_id IS NULL AND is_active = 1 AND deleted = 0 ORDER BY sort_order ASC")
    List<BuddyCharacter> getFreeBuddyCharacters();

    /**
     * 根据圈子ID获取角色列表
     */
    @Select("SELECT * FROM buddy_characters WHERE character_group_id = #{groupId} AND is_active = 1 AND deleted = 0 ORDER BY sort_order ASC")
    List<BuddyCharacter> getBuddyCharactersByGroupId(@Param("groupId") Long groupId);

    /**
     * 根据钱包ID获取搭子角色信息
     */
    @Select("SELECT bc.* FROM buddy_characters bc " +
            "INNER JOIN wallet_buddies wb ON bc.id = wb.buddy_character_id " +
            "WHERE wb.wallet_id = #{walletId} AND wb.is_active = 1 AND wb.deleted = 0 " +
            "AND bc.is_active = 1 AND bc.deleted = 0 " +
            "ORDER BY wb.create_time ASC")
    List<BuddyCharacter> getBuddyCharactersByWalletId(@Param("walletId") Long walletId);

    /**
     * 获取搭子角色的详细信息（包含标签解析）
     */
    @Select("SELECT id, name, nickname, avatar, full_image, gender, personality, description, " +
            "tags, voice_sample_url, voice_duration, character_group_id, sort_order " +
            "FROM buddy_characters WHERE id = #{characterId} AND is_active = 1 AND deleted = 0")
    Map<String, Object> getBuddyCharacterDetail(@Param("characterId") Long characterId);
}
