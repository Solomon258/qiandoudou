package com.qiandoudou.service;

import com.qiandoudou.entity.DreamItem;
import com.qiandoudou.entity.DreamReward;
import com.qiandoudou.entity.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 梦想钱包服务接口
 */
public interface DreamWalletService {

    /**
     * 获取梦想商品列表
     * @param category 商品分类：1-购物商品，2-旅行目的地
     * @return 商品列表
     */
    List<DreamItem> getDreamItems(Integer category);

    /**
     * 搜索梦想商品
     * @param keyword 搜索关键词
     * @param category 商品分类
     * @return 商品列表
     */
    List<DreamItem> searchDreamItems(String keyword, Integer category);

    /**
     * 创建梦想钱包
     * @param userId 用户ID
     * @param dreamType 梦想类型：1-购物，2-旅行
     * @param walletName 钱包名称
     * @param targetAmount 目标金额
     * @param itemId 商品ID
     * @param days 旅行天数（旅行类型必填）
     * @return 创建的钱包
     */
    Wallet createDreamWallet(Long userId, Integer dreamType, String walletName, 
                           BigDecimal targetAmount, Long itemId, Integer days);

    /**
     * 获取梦想钱包详情
     * @param walletId 钱包ID
     * @return 钱包详情信息
     */
    Map<String, Object> getDreamWalletDetail(Long walletId);

    /**
     * 更新梦想钱包进度
     * @param walletId 钱包ID
     * @param newBalance 新余额
     * @param triggerAmount 触发金额
     * @param triggerType 触发类型：1-转入，2-转出
     * @return 是否达到新的里程碑
     */
    boolean updateDreamProgress(Long walletId, BigDecimal newBalance, 
                              BigDecimal triggerAmount, Integer triggerType);

    /**
     * 获取梦想钱包奖励列表
     * @param walletId 钱包ID
     * @return 奖励列表
     */
    List<DreamReward> getDreamRewards(Long walletId);

    /**
     * 生成旅行行程
     * @param destination 目的地
     * @param days 天数
     * @param budget 预算
     * @return 行程内容
     */
    String generateTravelItinerary(String destination, Integer days, BigDecimal budget);

    /**
     * 生成分享图片
     * @param walletId 钱包ID
     * @param progress 当前进度
     * @return 分享图片URL
     */
    String generateShareImage(Long walletId, Integer progress);

    /**
     * 记录分享行为
     * @param walletId 钱包ID
     * @param userId 用户ID
     * @param shareType 分享类型
     * @param progress 分享进度
     * @param imageUrl 分享图片URL
     * @param shareText 分享文案
     */
    void recordShare(Long walletId, Long userId, Integer shareType, 
                    Integer progress, String imageUrl, String shareText);

    /**
     * 检查并解锁奖励
     * @param walletId 钱包ID
     * @param progress 当前进度
     * @return 新解锁的奖励列表
     */
    List<DreamReward> checkAndUnlockRewards(Long walletId, Integer progress);

    /**
     * 计算进度百分比
     * @param currentAmount 当前金额
     * @param targetAmount 目标金额
     * @return 进度百分比
     */
    Integer calculateProgress(BigDecimal currentAmount, BigDecimal targetAmount);

    /**
     * 获取进度对应的里程碑
     * @param progress 进度百分比
     * @return 里程碑值（20,40,60,80,100）
     */
    Integer getProgressMilestone(Integer progress);

    /**
     * 获取梦想钱包交易记录
     * @param walletId 钱包ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 交易记录分页结果
     */
    Map<String, Object> getDreamWalletRecords(Long walletId, Integer page, Integer pageSize);
}
