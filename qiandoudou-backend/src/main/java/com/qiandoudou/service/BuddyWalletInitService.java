package com.qiandoudou.service;

import java.util.List;

/**
 * 搭子钱包初始化服务接口
 */
public interface BuddyWalletInitService {
    
    /**
     * 异步初始化搭子钱包
     */
    void initializeBuddyWalletAsync(Long walletId, List<Long> buddyCharacterIds);
    
    /**
     * 同步初始化搭子钱包
     */
    void initializeBuddyWalletSync(Long walletId, List<Long> buddyCharacterIds);
}
