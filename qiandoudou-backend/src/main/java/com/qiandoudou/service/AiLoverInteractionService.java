package com.qiandoudou.service;

/**
 * AI情侣互动服务接口
 */
public interface AiLoverInteractionService {
    
    /**
     * 处理AI情侣钱包的流水动态互动
     * 当AI情侣钱包有新的流水记录时，自动点赞和评论
     * 
     * @param transactionId 交易ID
     */
    void processAiLoverWalletInteraction(Long transactionId);
    
    /**
     * 检查钱包是否为AI情侣钱包
     * 
     * @param walletId 钱包ID
     * @return 是否为AI情侣钱包
     */
    boolean isAiLoverWallet(Long walletId);
    
    /**
     * 获取AI情侣的用户ID（用于点赞和评论）
     * 
     * @param walletId 钱包ID
     * @return AI情侣的用户ID
     */
    Long getAiLoverUserId(Long walletId);
    
    /**
     * 生成AI情侣评论的语音URL
     * 
     * @param commentText 评论文本
     * @param aiPartnerId AI伴侣ID
     * @return 语音URL
     */
    String generateCommentVoice(String commentText, Long aiPartnerId);
}
