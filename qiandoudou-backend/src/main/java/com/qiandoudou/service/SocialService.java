package com.qiandoudou.service;

import java.util.Map;
import java.util.List;

/**
 * 社交功能服务接口
 */
public interface SocialService {
    
    /**
     * 获取用户社交统计数据
     */
    Map<String, Object> getUserSocialStats(Long userId);
    
    /**
     * 关注钱包
     */
    void followWallet(Long userId, Long walletId);
    
    /**
     * 取消关注钱包
     */
    void unfollowWallet(Long userId, Long walletId);
    
    /**
     * 点赞交易记录
     */
    void likeTransaction(Long userId, Long transactionId);
    
    /**
     * 取消点赞交易记录
     */
    void unlikeTransaction(Long userId, Long transactionId);
    
    /**
     * 评论交易记录
     */
    Map<String, Object> commentTransaction(Long userId, Long transactionId, String content);

    /**
     * AI伴侣评论交易记录
     */
    Map<String, Object> aiCommentTransaction(Long transactionId, String content, Long aiPartnerId, String voiceUrl);
    
    /**
     * 获取交易评论列表
     */
    List<Map<String, Object>> getTransactionComments(Long transactionId);
    
    /**
     * 获取用户互动消息
     */
    List<Map<String, Object>> getUserInteractionMessages(Long userId, Integer page, Integer pageSize);
    
    /**
     * 获取用户未读消息数量
     */
    Integer getUnreadMessageCount(Long userId);
    
    /**
     * 标记消息为已读
     */
    void markMessagesAsRead(Long userId);
    
    /**
     * 检查用户关注状态
     */
    Boolean checkUserFollowStatus(Long currentUserId, Long targetUserId);

    /**
     * 获取钱包社交统计数据
     */
    Map<String, Object> getWalletSocialStats(Long walletId);

    /**
     * 记录钱包浏览
     */
    void recordWalletView(Long userId, Long walletId);
    
    /**
     * 获取交易的社交数据（点赞数、评论数、用户是否已点赞）
     */
    Map<String, Object> getTransactionSocialData(Long transactionId, Long userId);
}
