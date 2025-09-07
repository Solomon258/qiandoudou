package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.WalletView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 钱包浏览记录 Mapper 接口
 */
@Mapper
public interface WalletViewMapper extends BaseMapper<WalletView> {

    /**
     * 获取钱包的浏览数量
     */
    @Select("SELECT COUNT(*) FROM wallet_views " +
            "WHERE wallet_id = #{walletId} AND deleted = 0")
    Integer getWalletViewCount(@Param("walletId") Long walletId);

    /**
     * 检查用户是否已浏览过某个钱包
     */
    @Select("SELECT COUNT(*) FROM wallet_views " +
            "WHERE user_id = #{userId} AND wallet_id = #{walletId} AND deleted = 0")
    Integer checkUserViewed(@Param("userId") Long userId, @Param("walletId") Long walletId);
}
