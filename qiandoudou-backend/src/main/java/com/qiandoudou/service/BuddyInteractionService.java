package com.qiandoudou.service;

import com.qiandoudou.entity.Transaction;

/**
 * 搭子互动服务接口
 */
public interface BuddyInteractionService {

    /**
     * 处理交易后的搭子互动
     */
    void processTransactionInteraction(Long walletId, Transaction transaction);

    /**
     * 触发钱包创建时的欢迎互动
     */
    void triggerWelcomeInteraction(Long walletId);

    /**
     * 检查角色是否可以进行互动（冷却控制）
     */
    boolean canCharacterInteract(Long characterId, Long walletId, Integer interactionType);

    /**
     * 生成搭子评论内容
     */
    String generateBuddyComment(Transaction transaction, Long characterId);

    /**
     * 生成搭子评论语音
     */
    String generateBuddyCommentVoice(String content, String voiceType);
}
