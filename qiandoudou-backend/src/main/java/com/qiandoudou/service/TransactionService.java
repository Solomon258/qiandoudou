package com.qiandoudou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiandoudou.entity.Transaction;

import java.util.List;
import java.util.Map;

/**
 * 交易记录服务接口
 */
public interface TransactionService extends IService<Transaction> {

    /**
     * 获取钱包交易记录
     */
    List<Transaction> getWalletTransactions(Long walletId);

    /**
     * 获取用户所有交易记录
     */
    List<Transaction> getUserTransactions(Long userId);

    /**
     * 获取钱包指定月份的统计数据
     */
    Map<String, Object> getWalletMonthlyStats(Long walletId, int year, int month);
}
