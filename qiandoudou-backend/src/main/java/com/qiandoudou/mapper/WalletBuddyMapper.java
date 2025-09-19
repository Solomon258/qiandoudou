package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.WalletBuddy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 钱包搭子关系 Mapper 接口
 */
@Mapper
public interface WalletBuddyMapper extends BaseMapper<WalletBuddy> {

    /**
     * 根据钱包ID获取搭子关系
     */
    @Select("SELECT * FROM wallet_buddies WHERE wallet_id = #{walletId} AND is_active = 1 AND deleted = 0")
    List<WalletBuddy> getWalletBuddies(@Param("walletId") Long walletId);

    /**
     * 检查钱包中是否已存在某个搭子
     */
    @Select("SELECT COUNT(*) FROM wallet_buddies WHERE wallet_id = #{walletId} AND buddy_character_id = #{buddyCharacterId} AND is_active = 1 AND deleted = 0")
    Integer checkBuddyExists(@Param("walletId") Long walletId, @Param("buddyCharacterId") Long buddyCharacterId);

    /**
     * 获取钱包的搭子数量
     */
    @Select("SELECT COUNT(*) FROM wallet_buddies WHERE wallet_id = #{walletId} AND is_active = 1 AND deleted = 0")
    Integer getWalletBuddyCount(@Param("walletId") Long walletId);
}
