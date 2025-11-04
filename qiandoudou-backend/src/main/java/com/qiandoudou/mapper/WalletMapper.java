package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 钱包 Mapper 接口
 */
@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    /**
     * 获取用户钱包列表（包含AI伴侣信息、梦想钱包字段和社交统计数据）
     * 返回所有钱包，过滤逻辑在Service层根据onlyPublic参数进行
     */
    @Select("SELECT w.id, w.user_id, w.name, CAST(w.type AS UNSIGNED) as type, w.balance, " +
            "w.background_image as backgroundImage, w.ai_partner_id, w.is_public, " +
            "w.create_time, w.update_time, w.deleted, " +
            "w.dream_type as dreamType, w.dream_target_amount as dreamTargetAmount, " +
            "w.dream_progress as dreamProgress, w.dream_item_name as dreamItemName, " +
            "w.dream_item_image as dreamItemImage, w.dream_destination as dreamDestination, " +
            "ap.name as ai_partner_name, ap.avatar as ai_partner_avatar, " +
            "COALESCE(fans_count.count, 0) as fansCount, " +
            "COALESCE(likes_count.count, 0) as likesCount, " +
            "COALESCE(views_count.count, 0) as viewsCount " +
            "FROM wallets w " +
            "LEFT JOIN ai_partners ap ON w.ai_partner_id = ap.id " +
            "LEFT JOIN (SELECT wallet_id, COUNT(*) as count FROM user_follows WHERE deleted = 0 GROUP BY wallet_id) fans_count ON w.id = fans_count.wallet_id " +
            "LEFT JOIN (SELECT wallet_id, COUNT(*) as count FROM (SELECT t.wallet_id FROM transactions t INNER JOIN post_likes pl ON t.id = pl.post_id WHERE t.deleted = 0 AND pl.deleted = 0 GROUP BY t.wallet_id, pl.id) likes_data GROUP BY wallet_id) likes_count ON w.id = likes_count.wallet_id " +
            "LEFT JOIN (SELECT wallet_id, COUNT(*) as count FROM wallet_views WHERE deleted = 0 GROUP BY wallet_id) views_count ON w.id = views_count.wallet_id " +
            "WHERE w.user_id = #{userId} AND w.deleted = 0 " +
            "GROUP BY w.id " +
            "ORDER BY w.create_time DESC")
    List<Map<String, Object>> getUserWalletsWithPartner(@Param("userId") Long userId);

    /**
     * 获取公开钱包列表及其最新交易记录（用于兜圈圈）- 分页版本
     */
    @Select("SELECT w.id, w.user_id, w.name, CAST(w.type AS UNSIGNED) as type, w.balance, " +
            "w.background_image as backgroundImage, w.create_time, " +
            "u.nickname as owner_nickname, u.avatar as owner_avatar, " +
            "ap.name as ai_partner_name, ap.avatar as ai_partner_avatar, " +
            "(" +
            "  SELECT JSON_ARRAYAGG(" +
            "    JSON_OBJECT(" +
            "      'id', t.id, " +
            "      'description', t.description, " +
            "      'amount', t.amount, " +
            "      'type', t.type, " +
            "      'create_time', t.create_time, " +
            "      'note', t.note" +
            "    )" +
            "  ) " +
            "  FROM transactions t " +
            "  WHERE t.wallet_id = w.id AND t.deleted = 0 " +
            "  ORDER BY t.create_time DESC " +
            "  LIMIT 2" +
            ") as recent_transactions " +
            "FROM wallets w " +
            "LEFT JOIN users u ON w.user_id = u.id " +
            "LEFT JOIN ai_partners ap ON w.ai_partner_id = ap.id " +
            "WHERE w.is_public = 1 AND w.deleted = 0 " +
            "ORDER BY w.update_time DESC " +
            "LIMIT #{size} OFFSET #{offset}")
    List<Map<String, Object>> getPublicWalletsWithRecentTransactions(@Param("offset") int offset, @Param("size") int size);

    /**
     * 获取公开钱包总数
     */
    @Select("SELECT COUNT(*) FROM wallets w WHERE w.is_public = 1 AND w.deleted = 0")
    Long getPublicWalletsCount();

    /**
     * 获取钱包所有者ID
     */
    @Select("SELECT user_id FROM wallets WHERE id = #{walletId} AND deleted = 0")
    Long getWalletOwnerId(@Param("walletId") Long walletId);

    /**
     * 获取用户关注的钱包列表（包含社交统计数据）
     */
    @Select("SELECT w.id, w.user_id, w.name, w.type, w.balance, " +
            "w.background_image as backgroundImage, w.create_time, " +
            "u.nickname as owner_nickname, u.avatar as owner_avatar, " +
            "COALESCE(fans_count.count, 0) as followers_count, " +
            "COALESCE(likes_count.count, 0) as likes_count, " +
            "COALESCE(views_count.count, 0) as views_count, " +
            "(" +
            "  SELECT COUNT(*) FROM user_follows uf2 " +
            "  WHERE uf2.wallet_id = w.id AND uf2.deleted = 0" +
            ") as participantCount " +
            "FROM user_follows uf " +
            "INNER JOIN wallets w ON uf.wallet_id = w.id " +
            "LEFT JOIN users u ON w.user_id = u.id " +
            "LEFT JOIN (SELECT wallet_id, COUNT(*) as count FROM user_follows WHERE deleted = 0 GROUP BY wallet_id) fans_count ON w.id = fans_count.wallet_id " +
            "LEFT JOIN (SELECT wallet_id, COUNT(*) as count FROM (SELECT t.wallet_id FROM transactions t INNER JOIN post_likes pl ON t.id = pl.post_id WHERE t.deleted = 0 AND pl.deleted = 0 GROUP BY t.wallet_id, pl.id) likes_data GROUP BY wallet_id) likes_count ON w.id = likes_count.wallet_id " +
            "LEFT JOIN (SELECT wallet_id, COUNT(*) as count FROM wallet_views WHERE deleted = 0 GROUP BY wallet_id) views_count ON w.id = views_count.wallet_id " +
            "WHERE uf.follower_id = #{userId} AND uf.deleted = 0 AND w.deleted = 0 AND w.is_public = 1 " +
            "GROUP BY w.id " +
            "ORDER BY uf.create_time DESC")
    List<Map<String, Object>> getUserFollowedWallets(@Param("userId") Long userId);
}
