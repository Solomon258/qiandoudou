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
     * @param userId 用户ID
     * @param onlyPublic true 时只返回公开的钱包，false 时返回所有钱包
     */
    List<Map<String, Object>> getUserWallets(Long userId, Boolean onlyPublic);

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
    Map<String, Object> getPublicWallets(Integer page, Integer size);

    /**
     * 设置钱包公开状态
     */
    void setWalletPublic(Long walletId, Integer isPublic);

    /**
     * AI伴侣自动转账
     */
    void aiPartnerTransfer(Long walletId, Long aiPartnerId, BigDecimal amount, String message, String aiPartnerName, String aiPartnerAvatar, String characterName);

    /**
     * 重新计算钱包余额（基于交易记录）
     */
    void recalculateWalletBalance(Long walletId);

    /**
     * 修复所有钱包余额
     */
    void fixAllWalletBalances();

    /**
     * 转账到其他钱包
     */
    void transferToWallet(Long fromWalletId, Long toWalletId, BigDecimal amount, String description, String imageUrl, String note);

    /**
     * 原子操作：增加钱包余额（避免并发问题）
     * 使用数据库层面的原子操作，确保余额加法不会因并发而丢失
     */
    void incrementWalletBalance(Long walletId, BigDecimal amount);
}
