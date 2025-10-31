package com.qiandoudou.service.impl;

import com.qiandoudou.entity.*;
import com.qiandoudou.mapper.*;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.DreamWalletService;
import com.qiandoudou.service.DreamImageConfigService;
import com.qiandoudou.service.TransactionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

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
    private TransactionService transactionService;

    @Autowired
    private AiService aiService;

    @Autowired
    private DreamImageConfigService dreamImageConfigService;

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
            wallet.setType(dreamType == 1 ? 4 : 5); // 4-梦想攒钱(购物)，5-梦想攒钱(旅行)
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setIsPublic(0);
            
            // 设置梦想相关字段
            wallet.setDreamType(dreamType);
            wallet.setDreamTargetAmount(targetAmount);
            wallet.setDreamProgress(0);
            wallet.setDreamLastMilestone(0);

            if (dreamType == 1) {
                // 购物类型
                wallet.setDreamItemName(dreamItem.getName());  // ⭐ 使用 name（英文标识符）而不是 displayName

                // 从 dream_image_config 获取初始进度图片（第1张，0-20%）
                List<String> progressImages = dreamImageConfigService.getProgressImages(dreamType, dreamItem.getName());
                if (progressImages != null && !progressImages.isEmpty()) {
                    wallet.setDreamItemImage(progressImages.get(0));  // 使用第1张进度图片
                } else {
                    // 降级方案：使用 dream_items 的图标
                    wallet.setDreamItemImage(dreamItem.getImageUrl());
                }
            } else {
                // 旅行类型
                wallet.setDreamDestination(dreamItem.getName());  // ⭐ 使用 name（英文标识符）而不是 displayName
                wallet.setDreamDays(days);

                // ⭐ 旅行钱包直接使用 dream_items 表中的 image_url 作为背景图
                // 不存储进度图片，进度图片由前端根据进度动态获取
                wallet.setDreamItemImage(dreamItem.getImageUrl());

                // 先设置默认行程文本
                wallet.setDreamItinerary("AI正在为您生成详细的旅行行程，请稍后刷新查看...");
            }

            // 保存钱包
            walletMapper.insert(wallet);

            // 为钱包创建默认奖励
            dreamRewardMapper.createDefaultRewards(wallet.getId());

            log.info("梦想钱包创建成功，钱包ID: {}, 类型: {}, 目标金额: {}",
                    wallet.getId(), dreamType, targetAmount);

            final Wallet finalWallet = wallet;

            // 如果是旅行类型，使用CompletableFuture异步生成行程
            if (dreamType == 2) {
                final Long walletId = wallet.getId();
                final String destination = dreamItem.getDisplayName();
                final Integer travelDays = days;
                final BigDecimal budgetAmount = targetAmount;

                // 使用CompletableFuture异步执行，不阻塞当前线程
                CompletableFuture.runAsync(() -> {
                    try {
                        // 等待100ms确保事务已提交
                        Thread.sleep(100);
                        asyncGenerateTravelItinerary(walletId, destination, travelDays, budgetAmount);
                    } catch (Exception e) {
                        log.error("异步任务启动失败: {}", e.getMessage(), e);
                    }
                });
            }

            return finalWallet;

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

    /**
     * 异步生成旅行行程
     */
    @Async
    public void asyncGenerateTravelItinerary(Long walletId, String destination, Integer days, BigDecimal budget) {
        log.info("==================== 开始异步生成旅行行程 ====================");
        log.info("钱包ID: {}, 目的地: {}, 天数: {}, 预算: {}", walletId, destination, days, budget);

        try {
            // 调用同步生成方法
            String itinerary = generateTravelItinerary(destination, days, budget);

            // 从行程文本中提取预算金额
            BigDecimal extractedBudget = extractBudgetFromItinerary(itinerary);

            // 更新钱包的行程字段和目标金额
            Wallet wallet = walletMapper.selectById(walletId);
            if (wallet != null) {
                wallet.setDreamItinerary(itinerary);

                // 如果成功提取到预算，更新目标金额
                if (extractedBudget != null && extractedBudget.compareTo(BigDecimal.ZERO) > 0) {
                    wallet.setDreamTargetAmount(extractedBudget);
                    log.info("从行程中提取预算并更新目标金额，钱包ID: {}, 预算: {}", walletId, extractedBudget);
                }

                walletMapper.updateById(wallet);
                log.info("钱包行程已更新，钱包ID: {}", walletId);
            } else {
                log.warn("钱包不存在，无法更新行程，钱包ID: {}", walletId);
            }

            log.info("==================== 异步生成旅行行程完成 ====================");
        } catch (Exception e) {
            log.error("==================== 异步生成旅行行程失败 ====================");
            log.error("钱包ID: {}, 异常: {}", walletId, e.getMessage(), e);

            // 失败时更新为友好提示
            try {
                Wallet wallet = walletMapper.selectById(walletId);
                if (wallet != null) {
                    wallet.setDreamItinerary("行程生成失败，请稍后重试或联系客服");
                    walletMapper.updateById(wallet);
                }
            } catch (Exception updateEx) {
                log.error("更新失败提示时出错: {}", updateEx.getMessage());
            }
        }
    }

    @Override
    public String generateTravelItinerary(String destination, Integer days, BigDecimal budget) {
        log.info("==================== 开始生成旅行行程 ====================");
        log.info("参数 - 目的地: {}, 天数: {}, 预算: {}", destination, days, budget);

        try {
            // 构建AI提示词
            String prompt = String.format(
                "请为%s生成简洁的%d日旅游攻略，总字数不超过500字。\n\n" +
                "旅行信息：\n" +
                "- 目的地：%s\n" +
                "- 天数：%d天\n\n" +
                "生成要求：\n" +
                "1. 用 Markdown 格式，主标题用 # （一个井号），每天用 #### （四个井号）\n" +
                "2. 主标题格式：# %s%d日旅游攻略\n" +
                "3. 每日标题格式：#### Day1: 景点名称\n" +
                "4. 每天只列出2-3个主要景点和1个特色餐饮\n" +
                "5. 简明扼要，每天50-80字\n" +
                "6. 用 - 开头列出要点\n" +
                "7. 最后添加预算总计：**总计**：人均约XXXX-XXXX元\n\n" +
                "内容要点：景点、美食、交通、预算，精而不杂。总字数必须在500字以内。",
                destination, days, destination, days, destination, days
            );

            log.info("AI提示词已构建，长度: {} 字符", prompt.length());
            log.info("准备调用 aiService.generateAiText()...");

            // 调用AI服务生成行程
            String aiGeneratedItinerary = aiService.generateAiText(prompt);

            log.info("aiService.generateAiText() 调用完成");
            log.info("返回结果是否为null: {}", aiGeneratedItinerary == null);

            if (aiGeneratedItinerary == null || aiGeneratedItinerary.trim().isEmpty()) {
                log.warn("AI生成的行程为空，使用默认模板");
                String defaultItinerary = getDefaultItinerary(destination, days, budget);
                log.info("==================== 行程生成完成（使用默认模板） ====================");
                return defaultItinerary;
            }

            log.info("AI行程生成成功，长度: {} 字符", aiGeneratedItinerary.length());
            log.info("行程内容前100字: {}", aiGeneratedItinerary.substring(0, Math.min(100, aiGeneratedItinerary.length())));
            log.info("==================== 行程生成完成（AI生成） ====================");
            return aiGeneratedItinerary;

        } catch (Exception e) {
            log.error("==================== 生成旅行行程失败 ====================");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常消息: {}", e.getMessage());
            log.error("完整堆栈:", e);
            String defaultItinerary = getDefaultItinerary(destination, days, budget);
            log.info("使用默认模板返回");
            return defaultItinerary;
        }
    }

    /**
     * 获取默认行程模板（当AI生成失败时使用）
     */
    private String getDefaultItinerary(String destination, Integer days, BigDecimal budget) {
        return String.format("【%s %d天旅行行程】\n\n" +
            "第1天：抵达%s，入住酒店，游览市中心\n" +
            "第2天：参观主要景点，品尝当地美食\n" +
            "第3天：购物和休闲，准备返程\n\n" +
            "预算：%s元\n" +
            "注：详细行程正在生成中，请稍后刷新查看完整安排。",
            destination, days, destination, budget.toString());
    }

    @Override
    public String generateShareImage(Long walletId, Integer progress) {
        Wallet wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            return null;
        }

        // 根据梦想类型获取分享图片
        if (wallet.getDreamType() == 1) {
            // 购物类型 - 获取该商品的分享图片
            String itemName = wallet.getDreamItemName();
            if (itemName != null) {
                String shareImage = dreamImageConfigService.getShareImage(1, itemName);
                if (shareImage != null) {
                    log.info("获取购物分享图片成功，商品: {}，URL: {}", itemName, shareImage);
                    return shareImage;
                }
            }
        } else {
            // 旅游类型 - 获取该目的地的分享图片
            String destination = wallet.getDreamDestination();
            if (destination != null) {
                String shareImage = dreamImageConfigService.getShareImage(2, destination);
                if (shareImage != null) {
                    log.info("获取旅游分享图片成功，目的地: {}，URL: {}", destination, shareImage);
                    return shareImage;
                }
            }
        }

        // 降级方案：返回当前阶段图片
        log.warn("未找到分享图片配置，使用阶段图片作为降级方案");
        return getCurrentStageImage(wallet, progress);
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

    @Override
    public Map<String, Object> getDreamWalletRecords(Long walletId, Integer page, Integer pageSize) {
        // 验证钱包是否存在
        Wallet wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        // 创建分页对象
        Page<Transaction> pageObj = new Page<>(page, pageSize);
        
        // 创建查询条件
        QueryWrapper<Transaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("wallet_id", walletId)
                   .eq("deleted", 0)
                   .orderByDesc("create_time");

        // 分页查询交易记录
        Page<Transaction> transactionPage = transactionService.page(pageObj, queryWrapper);
        
        // 格式化交易记录，添加类型标识
        List<Map<String, Object>> formattedRecords = transactionPage.getRecords().stream()
                .map(transaction -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("id", transaction.getId());
                    record.put("amount", transaction.getAmount());
                    record.put("createTime", transaction.getCreateTime());
                    record.put("description", transaction.getDescription());
                    record.put("imageUrl", transaction.getImageUrl());
                    record.put("note", transaction.getNote());
                    record.put("balanceAfter", transaction.getBalanceAfter());
                    
                    // 判断交易类型和是否为自动存入
                    boolean isAutoDeposit = transaction.getAiPartnerId() != null;
                    String typeText;
                    String type;
                    
                    if (transaction.getType() == 2) {
                        // 转出
                        typeText = "主动为梦想支出";
                        type = "out";
                    } else if (isAutoDeposit) {
                        // 自动存入（有AI伴侣ID的）
                        typeText = "自动为梦想存入";
                        type = "in";
                    } else {
                        // 主动存入
                        typeText = "主动为梦想存入";
                        type = "in";
                    }
                    
                    record.put("typeText", typeText);
                    record.put("type", type);
                    record.put("autoDeposit", isAutoDeposit);
                    record.put("aiPartnerId", transaction.getAiPartnerId());
                    record.put("aiPartnerName", transaction.getAiPartnerName());
                    
                    return record;
                })
                .collect(Collectors.toList());

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("records", formattedRecords);
        result.put("total", transactionPage.getTotal());
        result.put("pages", transactionPage.getPages());
        result.put("current", transactionPage.getCurrent());
        result.put("size", transactionPage.getSize());
        result.put("hasMore", transactionPage.getCurrent() < transactionPage.getPages());

        log.info("获取梦想钱包交易记录，钱包ID: {}, 页码: {}, 记录数: {}", 
                walletId, page, formattedRecords.size());

        return result;
    }

    /**
     * 获取当前阶段对应的图片
     */
    private String getCurrentStageImage(Wallet wallet, Integer progress) {
        if (wallet.getDreamType() == 1) {
            // 购物类型 - 从数据库配置中获取进度图片
            String itemName = wallet.getDreamItemName();
            if (itemName != null) {
                List<String> progressImages = dreamImageConfigService.getProgressImages(1, itemName);
                if (progressImages != null && !progressImages.isEmpty()) {
                    // 根据进度百分比选择对应的图片（0-20%返回第1张，20-40%返回第2张，以此类推）
                    if (progress >= 80) return progressImages.get(4);
                    if (progress >= 60) return progressImages.get(3);
                    if (progress >= 40) return progressImages.get(2);
                    if (progress >= 20) return progressImages.get(1);
                    return progressImages.get(0);
                }
            }
            // 降级方案：返回商品图片或空值
            return wallet.getDreamItemImage();
        } else {
            // ⭐ 旅行类型 - 直接返回dreamItemImage（背景图），不使用进度图片
            // 旅行钱包不需要根据进度显示不同的图片，始终显示目的地背景图
            return wallet.getDreamItemImage();
        }
    }

    /**
     * 从旅行行程文本中提取预算金额
     * 支持格式：人均约3000-4500元、3000-4500元、约3000元等
     */
    private BigDecimal extractBudgetFromItinerary(String itinerary) {
        if (itinerary == null || itinerary.isEmpty()) {
            log.warn("行程文本为空，无法提取预算");
            return null;
        }

        try {
            // 正则表达式匹配预算范围：人均约3000-4500元、3000-4500元等
            // 匹配模式：数字-数字元 或 约数字元
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:人均约?|约)?\\s*(\\d+)\\s*[-~—至到]\\s*(\\d+)\\s*元|(?:人均约?|约)\\s*(\\d+)\\s*元"
            );
            java.util.regex.Matcher matcher = pattern.matcher(itinerary);

            BigDecimal extractedBudget = null;

            // 找到最后一个匹配的预算（通常总计在最后）
            while (matcher.find()) {
                if (matcher.group(1) != null && matcher.group(2) != null) {
                    // 范围格式：3000-4500
                    int minBudget = Integer.parseInt(matcher.group(1));
                    int maxBudget = Integer.parseInt(matcher.group(2));
                    // 计算平均值
                    extractedBudget = new BigDecimal((minBudget + maxBudget) / 2.0)
                        .setScale(2, RoundingMode.HALF_UP);
                    log.info("提取到预算范围: {}-{}元，计算平均值: {}", minBudget, maxBudget, extractedBudget);
                } else if (matcher.group(3) != null) {
                    // 单个数字格式：约3000
                    int budget = Integer.parseInt(matcher.group(3));
                    extractedBudget = new BigDecimal(budget);
                    log.info("提取到预算: {}元", extractedBudget);
                }
            }

            if (extractedBudget == null) {
                log.warn("未能从行程中提取到预算信息");
            }

            return extractedBudget;

        } catch (Exception e) {
            log.error("提取预算时出错: {}", e.getMessage(), e);
            return null;
        }
    }
}
