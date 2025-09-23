package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.DreamReward;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 梦想奖励Mapper接口
 */
@Mapper
public interface DreamRewardMapper extends BaseMapper<DreamReward> {

    /**
     * 根据钱包ID获取奖励列表
     */
    List<DreamReward> selectByWalletId(@Param("walletId") Long walletId);

    /**
     * 根据钱包ID和进度获取可解锁的奖励
     */
    List<DreamReward> selectUnlockableRewards(@Param("walletId") Long walletId, @Param("progress") Integer progress);

    /**
     * 批量解锁奖励
     */
    int batchUnlockRewards(@Param("rewardIds") List<Long> rewardIds);

    /**
     * 为新钱包创建默认奖励（从模板复制）
     */
    int createDefaultRewards(@Param("walletId") Long walletId);
}
