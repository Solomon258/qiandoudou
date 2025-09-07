package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;
import java.util.Map;

/**
 * 用户关注 Mapper 接口
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * 检查用户是否已关注某个钱包
     */
    @Select("SELECT COUNT(*) FROM user_follows " +
            "WHERE follower_id = #{followerId} AND wallet_id = #{walletId} AND deleted = 0")
    Integer checkUserFollowed(@Param("followerId") Long followerId, @Param("walletId") Long walletId);

    /**
     * 获取钱包的粉丝数量
     */
    @Select("SELECT COUNT(*) FROM user_follows " +
            "WHERE wallet_id = #{walletId} AND deleted = 0")
    Integer getWalletFansCount(@Param("walletId") Long walletId);

    /**
     * 获取用户关注的钱包数量
     */
    @Select("SELECT COUNT(*) FROM user_follows " +
            "WHERE follower_id = #{userId} AND deleted = 0")
    Integer getUserFollowingCount(@Param("userId") Long userId);

    /**
     * 获取用户的粉丝数量（通过用户的钱包被关注数统计）
     */
    @Select("SELECT COUNT(DISTINCT uf.follower_id) FROM user_follows uf " +
            "INNER JOIN wallets w ON uf.wallet_id = w.id " +
            "WHERE w.user_id = #{userId} AND uf.deleted = 0 AND w.deleted = 0")
    Integer getUserFansCount(@Param("userId") Long userId);

    /**
     * 获取钱包的粉丝列表
     */
    @Select("SELECT uf.*, u.nickname, u.avatar " +
            "FROM user_follows uf " +
            "INNER JOIN users u ON uf.follower_id = u.id " +
            "WHERE uf.wallet_id = #{walletId} AND uf.deleted = 0 " +
            "ORDER BY uf.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<Map<String, Object>> getWalletFans(@Param("walletId") Long walletId, 
                                           @Param("offset") Integer offset, 
                                           @Param("pageSize") Integer pageSize);

    /**
     * 获取用户关注的钱包列表
     */
    @Select("SELECT uf.*, w.name as wallet_name, w.balance, w.background_image " +
            "FROM user_follows uf " +
            "INNER JOIN wallets w ON uf.wallet_id = w.id " +
            "WHERE uf.follower_id = #{userId} AND uf.deleted = 0 AND w.deleted = 0 " +
            "ORDER BY uf.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<Map<String, Object>> getUserFollowing(@Param("userId") Long userId, 
                                              @Param("offset") Integer offset, 
                                              @Param("pageSize") Integer pageSize);

    /**
     * 取消关注钱包（软删除）
     */
    @Delete("UPDATE user_follows SET deleted = 1 " +
            "WHERE follower_id = #{followerId} AND wallet_id = #{walletId}")
    void removeFollow(@Param("followerId") Long followerId, @Param("walletId") Long walletId);
}
