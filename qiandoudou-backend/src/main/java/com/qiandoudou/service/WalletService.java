package com.qiandoudou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiandoudou.entity.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 钱包服务接口
 */
public interface WalletService extends IService<Wallet> {

    /**
     * 获取用户钱包列表
     */
    List<Map<String, Object>> getUserWallets(Long userId);

    /**
     * 创建钱包
     */
    Wallet createWallet(Long userId, String name, Integer type, String backgroundImage, Long aiPartnerId);

    /**
     * 转入资金
     */
    void transferIn(Long walletId, BigDecimal amount, String description, String imageUrl, String note);

    /**
     * 转出资金
     */
    void transferOut(Long walletId, BigDecimal amount, String description, String imageUrl, String note);

    /**
     * 剧本攒钱
     */
    void scriptSaving(Long walletId, BigDecimal amount, String description, Long scriptChapterId);

    /**
     * 获取公开钱包列表（用于兜圈圈）
     */
    List<Map<String, Object>> getPublicWallets();

    /**
     * 设置钱包公开状态
     */
    void setWalletPublic(Long walletId, Integer isPublic);

    /**
     * AI伴侣自动转账
     */
    void aiPartnerTransfer(Long walletId, Long aiPartnerId, BigDecimal amount, String message, String aiPartnerName, String aiPartnerAvatar);

    /**
     * 重新计算钱包余额（基于交易记录）
     */
    void recalculateWalletBalance(Long walletId);

    /**
     * 修复所有钱包余额
     */
    void fixAllWalletBalances();
}
