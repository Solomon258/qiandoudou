package com.qiandoudou.service.impl;

import com.qiandoudou.entity.BuddyCharacter;
import com.qiandoudou.entity.Transaction;
import com.qiandoudou.entity.Wallet;
import com.qiandoudou.mapper.BuddyCharacterMapper;
import com.qiandoudou.mapper.WalletBuddyMapper;
import com.qiandoudou.service.BuddyAutoCommentService;
import com.qiandoudou.service.BuddyService;
import com.qiandoudou.service.TransactionService;
import com.qiandoudou.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * 搭子自动评论服务实现类
 */
@Service
public class BuddyAutoCommentServiceImpl implements BuddyAutoCommentService {

    private static final Logger logger = LoggerFactory.getLogger(BuddyAutoCommentServiceImpl.class);

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BuddyService buddyService;

    @Autowired
    private BuddyCharacterMapper buddyCharacterMapper;

    private final Random random = new Random();

    @Override
    @Async
    public List<Map<String, Object>> generateBuddyAutoComments(Long transactionId) {
        List<Map<String, Object>> comments = new ArrayList<>();
        
        try {
            logger.info("=== 开始生成搭子自动评论 ===");
            logger.info("交易ID: {}", transactionId);

            // 1. 获取交易信息
            Transaction transaction = transactionService.getById(transactionId);
            if (transaction == null) {
                logger.warn("交易不存在，交易ID: {}", transactionId);
                return comments;
            }

            // 2. 获取钱包信息
            Wallet wallet = walletService.getById(transaction.getWalletId());
            if (wallet == null || wallet.getType() != 3) {
                logger.info("非搭子钱包，跳过搭子评论生成，钱包类型: {}", wallet != null ? wallet.getType() : "null");
                return comments;
            }

            // 3. 获取钱包的搭子角色
            List<BuddyCharacter> buddyCharacters = buddyCharacterMapper.getBuddyCharactersByWalletId(wallet.getId());
            if (buddyCharacters.isEmpty()) {
                logger.warn("钱包没有搭子角色，钱包ID: {}", wallet.getId());
                return comments;
            }

            logger.info("找到 {} 个搭子角色", buddyCharacters.size());

            // 4. 构建交易描述
            String transactionDescription = buildTransactionDescription(transaction);
            logger.info("交易描述: {}", transactionDescription);

            // 5. 随机选择2-4个搭子进行评论（不是全部）
            List<BuddyCharacter> selectedBuddies = selectRandomBuddies(buddyCharacters);
            logger.info("随机选择 {} 个搭子进行评论", selectedBuddies.size());

            // 6. 异步生成每个搭子的评论
            List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
            for (BuddyCharacter buddy : selectedBuddies) {
                CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        // 随机延迟1-10秒，模拟真实互动
                        Thread.sleep((random.nextInt(10) + 1) * 1000L);
                        
                        return buddyService.generateBuddyComment(
                            transactionId, 
                            buddy.getId(), 
                            transactionDescription, 
                            transaction.getImageUrl()
                        );
                    } catch (Exception e) {
                        logger.error("搭子评论生成失败，搭子: {}", buddy.getName(), e);
                        return null;
                    }
                });
                futures.add(future);
            }

            // 7. 等待所有评论生成完成
            for (CompletableFuture<Map<String, Object>> future : futures) {
                try {
                    Map<String, Object> comment = future.get();
                    if (comment != null) {
                        comments.add(comment);
                        logger.info("搭子评论生成成功: {}", comment.get("buddyCharacterName"));
                    }
                } catch (Exception e) {
                    logger.error("获取搭子评论结果失败", e);
                }
            }

            logger.info("=== 搭子自动评论生成完成，共生成 {} 条评论 ===", comments.size());

        } catch (Exception e) {
            logger.error("生成搭子自动评论异常，交易ID: {}", transactionId, e);
        }

        return comments;
    }

    @Override
    public boolean isBuddyWallet(Long walletId) {
        try {
            Wallet wallet = walletService.getById(walletId);
            return wallet != null && wallet.getType() == 3;
        } catch (Exception e) {
            logger.error("检查搭子钱包失败，钱包ID: {}", walletId, e);
            return false;
        }
    }

    /**
     * 构建交易描述文本
     */
    private String buildTransactionDescription(Transaction transaction) {
        StringBuilder desc = new StringBuilder();
        
        // 交易类型描述
        switch (transaction.getType()) {
            case 1:
                desc.append("转入了 ").append(transaction.getAmount()).append(" 元");
                break;
            case 2:
                desc.append("转出了 ").append(transaction.getAmount()).append(" 元");
                break;
            case 3:
                desc.append("通过剧本攒了 ").append(transaction.getAmount()).append(" 元");
                break;
            default:
                desc.append("进行了 ").append(transaction.getAmount()).append(" 元的交易");
        }

        // 添加描述信息
        if (transaction.getDescription() != null && !transaction.getDescription().trim().isEmpty()) {
            desc.append("，备注：").append(transaction.getDescription());
        }

        // 添加余额信息
        desc.append("，当前余额：").append(transaction.getBalanceAfter()).append(" 元");

        return desc.toString();
    }

    /**
     * 随机选择搭子进行评论
     */
    private List<BuddyCharacter> selectRandomBuddies(List<BuddyCharacter> allBuddies) {
        List<BuddyCharacter> selected = new ArrayList<>();
        
        // 如果搭子数量少于等于4个，全部选择
        if (allBuddies.size() <= 4) {
            selected.addAll(allBuddies);
        } else {
            // 随机选择2-4个搭子
            int selectCount = Math.min(random.nextInt(3) + 2, allBuddies.size()); // 2-4个
            List<BuddyCharacter> shuffled = new ArrayList<>(allBuddies);
            java.util.Collections.shuffle(shuffled);
            selected.addAll(shuffled.subList(0, selectCount));
        }

        return selected;
    }
}
