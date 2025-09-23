package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.DreamProgressHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 梦想进度历史Mapper接口
 */
@Mapper
public interface DreamProgressHistoryMapper extends BaseMapper<DreamProgressHistory> {

    /**
     * 根据钱包ID获取进度历史
     */
    List<DreamProgressHistory> selectByWalletId(@Param("walletId") Long walletId);

    /**
     * 获取钱包的里程碑历史
     */
    List<DreamProgressHistory> selectMilestoneHistory(@Param("walletId") Long walletId);
}
