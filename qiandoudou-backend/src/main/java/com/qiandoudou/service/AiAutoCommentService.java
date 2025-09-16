package com.qiandoudou.service;

import java.util.Map;

/**
 * AI自动评论服务接口
 */
public interface AiAutoCommentService {

    /**
     * 为交易生成AI自动评论
     * @param transactionId 交易ID
     * @param walletId 钱包ID
     * @param userId 用户ID
     * @return AI评论数据（包含内容、语音URL等）
     */
    Map<String, Object> generateAutoComment(Long transactionId, Long walletId, Long userId);
}
