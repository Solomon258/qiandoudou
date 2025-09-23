package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.DreamShareRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 梦想分享记录Mapper接口
 */
@Mapper
public interface DreamShareRecordMapper extends BaseMapper<DreamShareRecord> {

    /**
     * 根据钱包ID获取分享记录
     */
    List<DreamShareRecord> selectByWalletId(@Param("walletId") Long walletId);

    /**
     * 根据用户ID获取分享记录
     */
    List<DreamShareRecord> selectByUserId(@Param("userId") Long userId);

    /**
     * 获取用户分享次数统计
     */
    Integer countUserShares(@Param("userId") Long userId);
}
