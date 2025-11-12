package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiandoudou.entity.Transaction;
import com.qiandoudou.entity.Wallet;
import com.qiandoudou.mapper.WalletMapper;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.DreamWalletService;
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
import java.util.HashMap;
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
    private DreamWalletService dreamWalletService;

    @Autowired
    private AiService aiService;

    @Override
    public List<Map<String, Object>> getUserWallets(Long userId, Boolean onlyPublic) {
        List<Map<String, Object>> wallets = baseMapper.getUserWalletsWithPartner(userId);

        // 如果只需要公开的钱包，进行过滤
        if (onlyPublic != null && onlyPublic) {
            wallets = wallets.stream()
                    .filter(wallet -> {
                        Object isPublic = wallet.get("is_public");
                        // 兼容数字1、字符串'1'和布尔值true
                        return isPublic == null ? false :
                               (isPublic.equals(1) || isPublic.equals("1") || isPublic.equals(true));
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        return wallets;
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
            } else if (type == 3) {
                // 搭子钱包（默认背景图，实际应该在BuddyService中设置）
                backgroundImage = "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dazi/dazizan_beijingtu.jpg";
            }
        }
        
        wallet.setBackgroundImage(backgroundImage);
        wallet.setAiPartnerId(aiPartnerId);
        wallet.setIsPublic(0); // 修改：默认为私密状态，由用户自主选择是否公开

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

        // 使用原子操作增加钱包余额，避免并发竞态条件
        // 这确保即使多个线程同时执行，余额也不会丢失
        int updatedRows = baseMapper.incrementBalance(walletId, amount);
        if (updatedRows <= 0) {
            throw new RuntimeException("钱包余额增加失败，钱包可能不存在");
        }

        // 重新获取钱包最新余额，用于后续操作
        Wallet updatedWallet = getById(walletId);
        BigDecimal newBalance = updatedWallet != null ? updatedWallet.getBalance() : wallet.getBalance().add(amount);

        logger.info("钱包余额原子增加成功，钱包ID: {}, 增加金额: {}, 新余额: {}", walletId, amount, newBalance);

        // 如果是梦想钱包，更新进度
        // 修复：type=4 是梦想攒钱(购物)，type=5 是梦想攒钱(旅行)，type=3 是搭子钱包
        if (wallet.getType() == 4 || wallet.getType() == 5) { // 4-梦想攒钱(购物)，5-梦想攒钱(旅行)
            try {
                boolean reachedMilestone = dreamWalletService.updateDreamProgress(
                    walletId, newBalance, amount, 1); // 1-转入
                if (reachedMilestone) {
                    logger.info("梦想钱包达到新里程碑，钱包ID: {}", walletId);
                }
            } catch (Exception e) {
                logger.error("更新梦想钱包进度失败，钱包ID: {}", walletId, e);
            }
        }

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

        // 使用原子操作减少钱包余额，避免并发竞态条件
        // 这确保即使多个线程同时执行，余额也不会出现问题
        int updatedRows = baseMapper.incrementBalance(walletId, amount.negate());
        if (updatedRows <= 0) {
            throw new RuntimeException("钱包余额减少失败，钱包可能不存在");
        }

        // 重新获取钱包最新余额，用于后续操作
        Wallet updatedWallet = getById(walletId);
        BigDecimal newBalance = updatedWallet != null ? updatedWallet.getBalance() : wallet.getBalance().subtract(amount);

        logger.info("钱包余额原子减少成功，钱包ID: {}, 减少金额: {}, 新余额: {}", walletId, amount, newBalance);

        // 如果是梦想钱包，更新进度
        // 修复：type=4 是梦想攒钱(购物)，type=5 是梦想攒钱(旅行)，type=3 是搭子钱包
        if (wallet.getType() == 4 || wallet.getType() == 5) { // 4-梦想攒钱(购物)，5-梦想攒钱(旅行)
            try {
                boolean reachedMilestone = dreamWalletService.updateDreamProgress(
                    walletId, newBalance, amount, 2); // 2-转出
                if (reachedMilestone) {
                    logger.info("梦想钱包达到新里程碑，钱包ID: {}", walletId);
                }
            } catch (Exception e) {
                logger.error("更新梦想钱包进度失败，钱包ID: {}", walletId, e);
            }
        }

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

        // 使用原子操作增加钱包余额，避免并发竞态条件
        int updatedRows = baseMapper.incrementBalance(walletId, amount);
        if (updatedRows <= 0) {
            throw new RuntimeException("钱包余额增加失败，钱包可能不存在");
        }

        // 重新获取钱包最新余额，用于后续操作
        Wallet updatedWallet = getById(walletId);
        BigDecimal newBalance = updatedWallet != null ? updatedWallet.getBalance() : wallet.getBalance().add(amount);

        logger.info("钱包余额原子增加成功，钱包ID: {}, 增加金额: {}, 新余额: {}", walletId, amount, newBalance);

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
    public Map<String, Object> getPublicWallets(Integer page, Integer size) {
        // 计算偏移量
        int offset = (page - 1) * size;
        
        // 获取分页数据
        List<Map<String, Object>> wallets = baseMapper.getPublicWalletsWithRecentTransactions(offset, size);
        
        // 获取总数
        Long total = baseMapper.getPublicWalletsCount();
        
        // 计算是否还有更多数据
        boolean hasMore = (long) offset + size < total;
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", wallets);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("hasMore", hasMore);
        
        return result;
    }

    @Override
    public void setWalletPublic(Long walletId, Integer isPublic) {
        logger.info("开始设置钱包公开状态 - walletId: {}, isPublic: {}", walletId, isPublic);
        
        Wallet wallet = getById(walletId);
        if (wallet == null) {
            logger.error("钱包不存在 - walletId: {}", walletId);
            throw new RuntimeException("钱包不存在");
        }
        
        Integer oldStatus = wallet.getIsPublic();
        logger.info("钱包原状态 - walletId: {}, 原is_public: {}, 新is_public: {}", walletId, oldStatus, isPublic);
        
        wallet.setIsPublic(isPublic);
        boolean updateResult = updateById(wallet);
        
        logger.info("钱包状态更新结果 - walletId: {}, updateResult: {}", walletId, updateResult);
        
        // 验证更新结果
        Wallet updatedWallet = getById(walletId);
        if (updatedWallet != null) {
            logger.info("验证更新结果 - walletId: {}, 当前is_public: {}", walletId, updatedWallet.getIsPublic());
        }
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

        // 使用原子操作增加钱包余额，避免并发竞态条件
        int updatedRows = baseMapper.incrementBalance(walletId, amount);
        if (updatedRows <= 0) {
            throw new RuntimeException("钱包余额增加失败，钱包可能不存在");
        }

        // 重新获取钱包最新余额，用于后续操作
        Wallet updatedWallet = getById(walletId);
        BigDecimal newBalance = updatedWallet != null ? updatedWallet.getBalance() : wallet.getBalance().add(amount);

        logger.info("钱包余额原子增加成功，钱包ID: {}, 增加金额: {}, 新余额: {}", walletId, amount, newBalance);

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

        // 获取当前余额
        BigDecimal originalBalance = wallet.getBalance();

        // 计算需要调整的差额
        BigDecimal difference = calculatedBalance.subtract(originalBalance);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            // 使用原子操作更新余额
            int updatedRows = baseMapper.incrementBalance(walletId, difference);
            if (updatedRows <= 0) {
                throw new RuntimeException("钱包余额重新计算失败，钱包可能不存在");
            }
            logger.info("钱包余额原子调整成功，钱包ID: {}, 调整金额: {}, 原余额: {}, 新余额: {}",
                       walletId, difference, originalBalance, calculatedBalance);
        } else {
            logger.info("钱包余额无需调整，钱包ID: {}, 当前余额: {}", walletId, originalBalance);
        }
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

    @Override
    @Transactional
    public void transferToWallet(Long fromWalletId, Long toWalletId, BigDecimal amount, String description, String imageUrl, String note) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("转账金额必须大于0");
        }

        if (fromWalletId.equals(toWalletId)) {
            throw new RuntimeException("不能转账到同一个钱包");
        }

        Wallet fromWallet = getById(fromWalletId);
        if (fromWallet == null) {
            throw new RuntimeException("源钱包不存在");
        }

        Wallet toWallet = getById(toWalletId);
        if (toWallet == null) {
            throw new RuntimeException("目标钱包不存在");
        }

        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("源钱包余额不足");
        }

        // 验证两个钱包属于同一个用户
        if (!fromWallet.getUserId().equals(toWallet.getUserId())) {
            throw new RuntimeException("只能转账到自己的钱包");
        }

        // 使用原子操作更新源钱包余额（减少）
        int fromUpdatedRows = baseMapper.incrementBalance(fromWalletId, amount.negate());
        if (fromUpdatedRows <= 0) {
            throw new RuntimeException("源钱包余额减少失败，钱包可能不存在");
        }

        // 使用原子操作更新目标钱包余额（增加）
        int toUpdatedRows = baseMapper.incrementBalance(toWalletId, amount);
        if (toUpdatedRows <= 0) {
            throw new RuntimeException("目标钱包余额增加失败，钱包可能不存在");
        }

        // 重新获取最新余额用于交易记录
        Wallet updatedFromWallet = getById(fromWalletId);
        Wallet updatedToWallet = getById(toWalletId);
        BigDecimal fromNewBalance = updatedFromWallet != null ? updatedFromWallet.getBalance() : fromWallet.getBalance().subtract(amount);
        BigDecimal toNewBalance = updatedToWallet != null ? updatedToWallet.getBalance() : toWallet.getBalance().add(amount);

        // 为源钱包创建转出交易记录
        Transaction fromTransaction = new Transaction();
        fromTransaction.setWalletId(fromWalletId);
        fromTransaction.setUserId(fromWallet.getUserId());
        fromTransaction.setType(2); // 转出
        fromTransaction.setAmount(amount);
        fromTransaction.setBalanceAfter(fromNewBalance);
        fromTransaction.setDescription(description + " (转账到：" + toWallet.getName() + ")");
        fromTransaction.setImageUrl(imageUrl);
        fromTransaction.setNote(note);
        fromTransaction.setCreateTime(java.time.LocalDateTime.now());
        transactionService.save(fromTransaction);

        // 为目标钱包创建转入交易记录
        Transaction toTransaction = new Transaction();
        toTransaction.setWalletId(toWalletId);
        toTransaction.setUserId(toWallet.getUserId());
        toTransaction.setType(1); // 转入
        toTransaction.setAmount(amount);
        toTransaction.setBalanceAfter(toNewBalance);
        toTransaction.setDescription(description + " (来自：" + fromWallet.getName() + ")");
        toTransaction.setImageUrl(imageUrl);
        toTransaction.setNote(note);
        toTransaction.setCreateTime(java.time.LocalDateTime.now());
        transactionService.save(toTransaction);

        // 发布事件触发AI互动
        try {
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, fromTransaction.getId()));
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, toTransaction.getId()));
            logger.debug("发布钱包转账事件，从钱包ID: {}, 到钱包ID: {}", fromWalletId, toWalletId);
        } catch (Exception e) {
            logger.error("发布钱包转账事件失败，从钱包ID: {}, 到钱包ID: {}", fromWalletId, toWalletId, e);
        }

        logger.info("钱包转账成功，从钱包ID: {}, 到钱包ID: {}, 转账金额: {}", fromWalletId, toWalletId, amount);
    }

    @Override
    @Transactional
    public void incrementWalletBalance(Long walletId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("增加金额必须大于0");
        }

        // 使用原子操作增加钱包余额，避免并发竞态条件
        // 这个方法使用数据库的 UPDATE ... SET balance = balance + ? 语句
        // 确保即使多个线程同时执行，余额也不会丢失
        int updatedRows = baseMapper.incrementBalance(walletId, amount);

        if (updatedRows <= 0) {
            throw new RuntimeException("钱包余额增加失败，钱包可能不存在");
        }

        logger.debug("钱包余额原子增加成功，钱包ID: {}, 增加金额: {}", walletId, amount);
    }
}
