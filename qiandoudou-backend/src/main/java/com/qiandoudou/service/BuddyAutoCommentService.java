package com.qiandoudou.service;

import java.util.List;
import java.util.Map;

/**
 * 搭子自动评论服务接口
 */
public interface BuddyAutoCommentService {

    /**
     * 为交易生成搭子自动评论
     * @param transactionId 交易ID
     * @return 生成的评论列表
     */
    List<Map<String, Object>> generateBuddyAutoComments(Long transactionId);

    /**
     * 检查钱包是否为搭子钱包
     * @param walletId 钱包ID
     * @return 是否为搭子钱包
     */
    boolean isBuddyWallet(Long walletId);
}
