package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

/**
 * 动态点赞 Mapper 接口
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    /**
     * 检查用户是否已点赞某个交易
     */
    @Select("SELECT COUNT(*) FROM post_likes " +
            "WHERE post_id = #{transactionId} AND user_id = #{userId} AND deleted = 0")
    Integer checkUserLiked(@Param("transactionId") Long transactionId, @Param("userId") Long userId);

    /**
     * 获取交易的点赞数量
     */
    @Select("SELECT COUNT(*) FROM post_likes " +
            "WHERE post_id = #{transactionId} AND deleted = 0")
    Integer getTransactionLikeCount(@Param("transactionId") Long transactionId);

    /**
     * 获取用户的总获赞数
     */
    @Select("SELECT COUNT(*) FROM post_likes pl " +
            "INNER JOIN transactions t ON pl.post_id = t.id " +
            "WHERE t.user_id = #{userId} AND pl.deleted = 0 AND t.deleted = 0")
    Integer getUserTotalLikes(@Param("userId") Long userId);

    /**
     * 获取钱包的总获赞数
     */
    @Select("SELECT COUNT(*) FROM post_likes pl " +
            "INNER JOIN transactions t ON pl.post_id = t.id " +
            "WHERE t.wallet_id = #{walletId} AND pl.deleted = 0 AND t.deleted = 0")
    Integer getWalletTotalLikes(@Param("walletId") Long walletId);

    /**
     * 删除点赞记录（软删除）
     */
    @Delete("UPDATE post_likes SET deleted = 1 " +
            "WHERE post_id = #{transactionId} AND user_id = #{userId}")
    void removeLike(@Param("transactionId") Long transactionId, @Param("userId") Long userId);

    /**
     * 安全插入点赞记录，避免重复插入
     * 使用 INSERT IGNORE 忽略重复插入错误
     */
    @org.apache.ibatis.annotations.Insert("INSERT IGNORE INTO post_likes (id, post_id, user_id, is_ai_like, create_time, deleted) " +
            "VALUES (#{id}, #{postId}, #{userId}, #{isAiLike}, #{createTime}, 0)")
    int insertLikeSafely(PostLike postLike);
    
    /**
     * 原子性地插入或激活点赞记录
     */
    @org.apache.ibatis.annotations.Insert("INSERT INTO post_likes (id, post_id, user_id, is_ai_like, create_time, deleted) " +
            "VALUES (#{id}, #{postId}, #{userId}, #{isAiLike}, #{createTime}, 0) " +
            "ON DUPLICATE KEY UPDATE deleted = 0, create_time = VALUES(create_time)")
    int upsertLike(PostLike postLike);
}
