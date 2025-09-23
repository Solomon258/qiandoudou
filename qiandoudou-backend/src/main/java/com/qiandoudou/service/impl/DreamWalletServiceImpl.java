package com.qiandoudou.service.impl;

import com.qiandoudou.entity.*;
import com.qiandoudou.mapper.*;
import com.qiandoudou.service.DreamWalletService;
import com.qiandoudou.service.impl.ByteDanceImageToTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 梦想钱包服务实现类
 */
@Slf4j
@Service
public class DreamWalletServiceImpl implements DreamWalletService {

    @Autowired
    private DreamItemMapper dreamItemMapper;

    @Autowired
    private DreamRewardMapper dreamRewardMapper;

    @Autowired
    private DreamShareRecordMapper dreamShareRecordMapper;

    @Autowired
    private DreamProgressHistoryMapper dreamProgressHistoryMapper;

    @Autowired
    private WalletMapper walletMapper;


    @Autowired
    private ByteDanceImageToTextService byteDanceImageToTextService;

    @Override
    public List<DreamItem> getDreamItems(Integer category) {
        return dreamItemMapper.selectByCategory(category);
    }

    @Override
    public List<DreamItem> searchDreamItems(String keyword, Integer category) {
        return dreamItemMapper.searchItems(keyword, category);
    }

    @Override
    @Transactional
    public Wallet createDreamWallet(Long userId, Integer dreamType, String walletName, 
                                  BigDecimal targetAmount, Long itemId, Integer days) {
        try {
            // 获取商品信息
            DreamItem dreamItem = dreamItemMapper.selectById(itemId);
            if (dreamItem == null) {
                throw new RuntimeException("商品不存在");
            }

            // 创建钱包
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setName(walletName);
            wallet.setType(dreamType == 1 ? 3 : 4); // 3-梦想攒钱(购物)，4-梦想攒钱(旅行)
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setIsPublic(0);
            
            // 设置梦想相关字段
            wallet.setDreamType(dreamType);
            wallet.setDreamTargetAmount(targetAmount);
            wallet.setDreamProgress(0);
            wallet.setDreamLastMilestone(0);

            if (dreamType == 1) {
                // 购物类型
                wallet.setDreamItemName(dreamItem.getDisplayName());
                wallet.setDreamItemImage(dreamItem.getImageUrl());
            } else {
                // 旅行类型
                wallet.setDreamDestination(dreamItem.getDisplayName());
                wallet.setDreamDays(days);
                wallet.setDreamItemImage(dreamItem.getImageUrl());
                
                // 生成旅行行程
                try {
                    String itinerary = generateTravelItinerary(dreamItem.getDisplayName(), days, targetAmount);
                    wallet.setDreamItinerary(itinerary);
                } catch (Exception e) {
                    log.warn("生成旅行行程失败: {}", e.getMessage());
                    wallet.setDreamItinerary("行程生成中，请稍后查看...");
                }
            }

            // 保存钱包
            walletMapper.insert(wallet);

            // 为钱包创建默认奖励
            dreamRewardMapper.createDefaultRewards(wallet.getId());

            log.info("梦想钱包创建成功，钱包ID: {}, 类型: {}, 目标金额: {}", 
                    wallet.getId(), dreamType, targetAmount);

            return wallet;

        } catch (Exception e) {
            log.error("创建梦想钱包失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建梦想钱包失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDreamWalletDetail(Long walletId) {
        Wallet wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("wallet", wallet);

        // 获取奖励列表
        List<DreamReward> rewards = dreamRewardMapper.selectByWalletId(walletId);
        result.put("rewards", rewards);

        // 获取进度历史
        List<DreamProgressHistory> progressHistory = dreamProgressHistoryMapper.selectByWalletId(walletId);
        result.put("progressHistory", progressHistory);

        // 计算进度信息
        Integer progress = calculateProgress(wallet.getBalance(), wallet.getDreamTargetAmount());
        result.put("currentProgress", progress);
        result.put("remainingAmount", wallet.getDreamTargetAmount().subtract(wallet.getBalance()));

        // 获取当前阶段图片
        String stageImage = getCurrentStageImage(wallet, progress);
        result.put("currentStageImage", stageImage);

        return result;
    }

    @Override
    @Transactional
    public boolean updateDreamProgress(Long walletId, BigDecimal newBalance, 
                                     BigDecimal triggerAmount, Integer triggerType) {
        Wallet wallet = walletMapper.selectById(walletId);
        if (wallet == null || wallet.getDreamTargetAmount() == null) {
            return false;
        }

        // 计算新进度
        Integer oldProgress = wallet.getDreamProgress();
        Integer newProgress = calculateProgress(newBalance, wallet.getDreamTargetAmount());
        
        // 更新钱包进度
        wallet.setDreamProgress(newProgress);
        
        // 检查是否达到新的里程碑
        Integer newMilestone = getProgressMilestone(newProgress);
        Integer oldMilestone = wallet.getDreamLastMilestone();
        boolean reachedNewMilestone = newMilestone > oldMilestone;
        
        if (reachedNewMilestone) {
            wallet.setDreamLastMilestone(newMilestone);
        }
        
        walletMapper.updateById(wallet);

        // 记录进度历史
        DreamProgressHistory history = new DreamProgressHistory();
        history.setWalletId(walletId);
        history.setOldProgress(oldProgress);
        history.setNewProgress(newProgress);
        history.setTriggerAmount(triggerAmount);
        history.setTriggerType(triggerType);
        if (reachedNewMilestone) {
            history.setMilestoneReached(newMilestone);
        }
        dreamProgressHistoryMapper.insert(history);

        // 如果达到新里程碑，解锁奖励
        if (reachedNewMilestone) {
            checkAndUnlockRewards(walletId, newProgress);
        }

        log.info("梦想钱包进度更新，钱包ID: {}, 进度: {}% -> {}%, 里程碑: {}", 
                walletId, oldProgress, newProgress, reachedNewMilestone ? newMilestone : "无");

        return reachedNewMilestone;
    }

    @Override
    public List<DreamReward> getDreamRewards(Long walletId) {
        return dreamRewardMapper.selectByWalletId(walletId);
    }

    @Override
    public String generateTravelItinerary(String destination, Integer days, BigDecimal budget) {
        try {
            String prompt = String.format(
                "为%s%d天的旅行生成详细行程安排，预算%s元，包括必游景点、住宿推荐、特色美食、交通方式、每日费用预估等，要求实用性强，符合中国游客习惯。请按天数分别列出每天的安排。",
                destination, days, budget.toString()
            );
            
            return byteDanceImageToTextService.generateTextFromPrompt(prompt);
        } catch (Exception e) {
            log.error("生成旅行行程失败: {}", e.getMessage(), e);
            return String.format("【%s %d天旅行行程】\n\n由于系统繁忙，行程正在生成中，请稍后刷新查看详细安排。\n\n预算：%s元", 
                               destination, days, budget.toString());
        }
    }

    @Override
    public String generateShareImage(Long walletId, Integer progress) {
        // TODO: 实现分享图片生成逻辑
        // 这里先返回默认图片，后续可以集成图片合成服务
        Wallet wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            return null;
        }

        String stageImage = getCurrentStageImage(wallet, progress);
        return stageImage;
    }

    @Override
    @Transactional
    public void recordShare(Long walletId, Long userId, Integer shareType, 
                          Integer progress, String imageUrl, String shareText) {
        DreamShareRecord record = new DreamShareRecord();
        record.setWalletId(walletId);
        record.setUserId(userId);
        record.setShareType(shareType);
        record.setShareProgress(progress);
        record.setShareImageUrl(imageUrl);
        record.setShareText(shareText);
        record.setSharePlatform("wechat");
        
        dreamShareRecordMapper.insert(record);
        
        log.info("记录梦想分享，钱包ID: {}, 用户ID: {}, 进度: {}%", walletId, userId, progress);
    }

    @Override
    @Transactional
    public List<DreamReward> checkAndUnlockRewards(Long walletId, Integer progress) {
        List<DreamReward> unlockableRewards = dreamRewardMapper.selectUnlockableRewards(walletId, progress);
        
        if (!unlockableRewards.isEmpty()) {
            List<Long> rewardIds = unlockableRewards.stream()
                    .map(DreamReward::getId)
                    .collect(Collectors.toList());
            
            dreamRewardMapper.batchUnlockRewards(rewardIds);
            
            log.info("解锁梦想奖励，钱包ID: {}, 奖励数量: {}", walletId, unlockableRewards.size());
        }
        
        return unlockableRewards;
    }

    @Override
    public Integer calculateProgress(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        BigDecimal progress = currentAmount.divide(targetAmount, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        int result = progress.intValue();
        return Math.min(result, 100); // 最大不超过100%
    }

    @Override
    public Integer getProgressMilestone(Integer progress) {
        if (progress >= 100) return 100;
        if (progress >= 80) return 80;
        if (progress >= 60) return 60;
        if (progress >= 40) return 40;
        if (progress >= 20) return 20;
        return 0;
    }

    /**
     * 获取当前阶段对应的图片
     */
    private String getCurrentStageImage(Wallet wallet, Integer progress) {
        if (wallet.getDreamType() == 1) {
            // 购物类型 - 汽车图片
            if (progress >= 80) return "/static/icon/car/汽车阶段5@3x.png";
            if (progress >= 60) return "/static/icon/car/汽车阶段4@3x.png";
            if (progress >= 40) return "/static/icon/car/汽车阶段3@3x.png";
            if (progress >= 20) return "/static/icon/car/汽车阶段2@3x.png";
            return "/static/icon/car/汽车阶段1@3x.png";
        } else {
            // 旅行类型 - 目的地图片
            return wallet.getDreamItemImage();
        }
    }
}
