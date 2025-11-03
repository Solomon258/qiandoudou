package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.BuddyInteraction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搭子互动记录 Mapper 接口
 */
@Mapper
public interface BuddyInteractionMapper extends BaseMapper<BuddyInteraction> {

    /**
     * 检查角色在指定时间内是否已经互动过（用于冷却控制）
     */
    @Select("SELECT COUNT(*) FROM buddy_interactions " +
            "WHERE character_id = #{characterId} AND wallet_id = #{walletId} " +
            "AND interaction_type = #{interactionType} " +
            "AND create_time > #{afterTime} AND deleted = 0")
    int countRecentInteractions(@Param("characterId") Long characterId,
                               @Param("walletId") Long walletId,
                               @Param("interactionType") Integer interactionType,
                               @Param("afterTime") LocalDateTime afterTime);

    /**
     * 获取交易的搭子互动记录
     */
    @Select("SELECT * FROM buddy_interactions " +
            "WHERE transaction_id = #{transactionId} AND deleted = 0 " +
            "ORDER BY create_time ASC")
    List<BuddyInteraction> getTransactionInteractions(@Param("transactionId") Long transactionId);
}
