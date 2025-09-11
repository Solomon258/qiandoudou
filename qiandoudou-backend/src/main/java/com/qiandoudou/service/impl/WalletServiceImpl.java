package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiandoudou.entity.Transaction;
import com.qiandoudou.entity.Wallet;
import com.qiandoudou.mapper.WalletMapper;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.TransactionService;
import com.qiandoudou.service.WalletService;
import com.qiandoudou.event.TransactionCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 钱包服务实现类
 */
@Service
public class WalletServiceImpl extends ServiceImpl<WalletMapper, Wallet> implements WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AiService aiService;

    @Override
    public List<Map<String, Object>> getUserWallets(Long userId) {
        return baseMapper.getUserWalletsWithPartner(userId);
    }

    @Override
    public Wallet createWallet(Long userId, String name, Integer type, String backgroundImage, Long aiPartnerId) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setName(name);
        wallet.setType(type);
        wallet.setBalance(BigDecimal.ZERO);
        
        // 根据钱包类型设置默认背景图，如果没有传入背景图的话
        if (backgroundImage == null || backgroundImage.trim().isEmpty()) {
            if (type == 1) {
                // 个人钱包（自己攒钱）
                backgroundImage = "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/view.jpg";
            } else if (type == 2) {
                // 情侣钱包
                backgroundImage = "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/lover.jpg";
            }
        }
        
        wallet.setBackgroundImage(backgroundImage);
        wallet.setAiPartnerId(aiPartnerId);
        wallet.setIsPublic(1); // 修改：默认公开到社交圈

        save(wallet);
        return wallet;
    }

    @Override
    @Transactional
    public void transferIn(Long walletId, BigDecimal amount, String description, String imageUrl, String note) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("转入金额必须大于0");
        }

        Wallet wallet = getById(walletId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        // 更新钱包余额
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        updateById(wallet);

        // 创建交易记录
        Transaction transaction = new Transaction();
        transaction.setWalletId(walletId);
        transaction.setUserId(wallet.getUserId());
        transaction.setType(1); // 转入
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setDescription(description);
        transaction.setImageUrl(imageUrl);
        transaction.setNote(note);
        // 时间字段由MyBatis Plus自动填充，但我们也可以手动设置确保正确
        transaction.setCreateTime(java.time.LocalDateTime.now());
        transactionService.save(transaction);
        
        // 发布交易创建事件，触发AI情侣钱包互动
        try {
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction.getId()));
            logger.debug("发布交易创建事件，交易ID: {}", transaction.getId());
        } catch (Exception e) {
            logger.error("发布交易创建事件失败，交易ID: {}", transaction.getId(), e);
        }
    }

    @Override
    @Transactional
    public void transferOut(Long walletId, BigDecimal amount, String description, String imageUrl, String note) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("转出金额必须大于0");
        }

        Wallet wallet = getById(walletId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }

        // 更新钱包余额
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        updateById(wallet);

        // 创建交易记录
        Transaction transaction = new Transaction();
        transaction.setWalletId(walletId);
        transaction.setUserId(wallet.getUserId());
        transaction.setType(2); // 转出
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setDescription(description);
        transaction.setImageUrl(imageUrl);
        transaction.setNote(note);
        // 时间字段由MyBatis Plus自动填充，但我们也可以手动设置确保正确
        transaction.setCreateTime(java.time.LocalDateTime.now());
        transactionService.save(transaction);
        
        // 发布交易创建事件，触发AI情侣钱包互动
        try {
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction.getId()));
            logger.debug("发布交易创建事件，交易ID: {}", transaction.getId());
        } catch (Exception e) {
            logger.error("发布交易创建事件失败，交易ID: {}", transaction.getId(), e);
        }
    }

    @Override
    @Transactional
    public void scriptSaving(Long walletId, BigDecimal amount, String description, Long scriptChapterId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("攒钱金额必须大于0");
        }

        Wallet wallet = getById(walletId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        // 更新钱包余额
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        updateById(wallet);

        // 创建交易记录
        Transaction transaction = new Transaction();
        transaction.setWalletId(walletId);
        transaction.setUserId(wallet.getUserId());
        transaction.setType(3); // 剧本攒钱
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setDescription(description);
        transaction.setScriptChapterId(scriptChapterId);
        // 时间字段由MyBatis Plus自动填充，但我们也可以手动设置确保正确
        transaction.setCreateTime(java.time.LocalDateTime.now());
        transactionService.save(transaction);
        
        // 发布交易创建事件，触发AI情侣钱包互动
        try {
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction.getId()));
            logger.debug("发布交易创建事件，交易ID: {}", transaction.getId());
        } catch (Exception e) {
            logger.error("发布交易创建事件失败，交易ID: {}", transaction.getId(), e);
        }
    }

    @Override
    public List<Map<String, Object>> getPublicWallets() {
        return baseMapper.getPublicWalletsWithRecentTransactions();
    }

    @Override
    public void setWalletPublic(Long walletId, Integer isPublic) {
        Wallet wallet = getById(walletId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }
        
        wallet.setIsPublic(isPublic);
        updateById(wallet);
    }

    @Override
    @Transactional
    public void aiPartnerTransfer(Long walletId, Long aiPartnerId, BigDecimal amount, String message, String aiPartnerName, String aiPartnerAvatar, String characterName) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("转入金额必须大于0");
        }

        // 获取钱包
        Wallet wallet = getById(walletId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        logger.info("开始处理AI伴侣转账，钱包ID: {}, AI伴侣ID: {}, 金额: {}", walletId, aiPartnerId, amount);

        // 更新钱包余额
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        updateById(wallet);

        // 构建转账描述内容用于AI生成
        String postContent = "转入" + amount + "元到情侣攒钱钱包";
        
        // 使用AI生成伴侣评论
        String aiGeneratedMessage;
        String voiceUrl = null;
        
        try {
            logger.info("开始调用AI服务生成伴侣评论，AI伴侣ID: {}, 内容: {}", aiPartnerId, postContent);
            aiGeneratedMessage = aiService.generatePartnerComment(aiPartnerId, postContent);
            logger.info("AI伴侣评论生成成功: {}", aiGeneratedMessage);
            
            // 生成语音 - 使用人物名称
            logger.info("开始调用TTS服务生成语音，人物名称: {}", characterName);
            if (characterName != null && !characterName.trim().isEmpty()) {
                voiceUrl = aiService.generatePartnerVoiceByCharacterName(characterName, aiGeneratedMessage);
            } else {
                // 如果没有提供人物名称，回退到原来的方法
                voiceUrl = aiService.generatePartnerVoice(aiPartnerId, aiGeneratedMessage);
            }
            if (voiceUrl != null) {
                logger.info("TTS语音生成成功，URL: {}", voiceUrl);
            } else {
                logger.warn("TTS语音生成失败，返回null");
            }
            
        } catch (Exception e) {
            logger.error("AI服务调用失败: {}", e.getMessage(), e);
            // 如果AI生成失败，使用传入的message作为备用
            aiGeneratedMessage = message != null ? message : aiPartnerName + "：给你转了" + amount + "元，继续加油储蓄哦！";
        }

        // 创建AI伴侣交易记录
        Transaction transaction = new Transaction();
        transaction.setWalletId(walletId);
        transaction.setUserId(wallet.getUserId()); // 使用钱包所有者ID
        transaction.setType(1); // 转入类型
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setDescription("AI伴侣转账");
        transaction.setNote(aiGeneratedMessage); // 使用AI生成的消息
        // 设置AI伴侣相关字段
        transaction.setAiPartnerId(aiPartnerId);
        transaction.setAiPartnerName(aiPartnerName);
        transaction.setAiPartnerAvatar(aiPartnerAvatar);
        transaction.setAiMessage(aiGeneratedMessage);
        
        // 设置语音URL和时长
        if (voiceUrl != null) {
            transaction.setVoiceUrl(voiceUrl);
            transaction.setVoiceDuration("10s"); // 预估时长
        } else {
            // 如果没有语音，设置为null
            transaction.setVoiceUrl(null);
            transaction.setVoiceDuration(null);
        }

        transactionService.save(transaction);
        logger.info("AI伴侣转账处理完成，交易记录已保存");
    }

    @Override
    @Transactional
    public void recalculateWalletBalance(Long walletId) {
        logger.info("开始重新计算钱包余额，钱包ID: {}", walletId);
        
        Wallet wallet = getById(walletId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        // 获取该钱包的所有交易记录，按时间排序
        List<Transaction> transactions = transactionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Transaction>()
                .eq("wallet_id", walletId)
                .eq("deleted", 0)
                .orderByAsc("create_time")
        );

        // 重新计算余额
        BigDecimal calculatedBalance = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getType() == 1 || transaction.getType() == 3) {
                // 转入或剧本攒钱
                calculatedBalance = calculatedBalance.add(transaction.getAmount());
            } else if (transaction.getType() == 2) {
                // 转出
                calculatedBalance = calculatedBalance.subtract(transaction.getAmount());
            }
        }

        // 更新钱包余额
        BigDecimal originalBalance = wallet.getBalance();
        wallet.setBalance(calculatedBalance);
        updateById(wallet);

        logger.info("钱包余额重新计算完成，钱包ID: {}, 原余额: {}, 计算后余额: {}, 交易记录数: {}", 
                   walletId, originalBalance, calculatedBalance, transactions.size());
    }

    @Override
    @Transactional
    public void fixAllWalletBalances() {
        logger.info("开始修复所有钱包余额");
        
        List<Wallet> wallets = list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Wallet>()
            .eq("deleted", 0));
        
        int fixedCount = 0;
        for (Wallet wallet : wallets) {
            try {
                recalculateWalletBalance(wallet.getId());
                fixedCount++;
            } catch (Exception e) {
                logger.error("修复钱包余额失败，钱包ID: {}, 错误: {}", wallet.getId(), e.getMessage());
            }
        }
        
        logger.info("所有钱包余额修复完成，共修复 {} 个钱包", fixedCount);
    }
}
