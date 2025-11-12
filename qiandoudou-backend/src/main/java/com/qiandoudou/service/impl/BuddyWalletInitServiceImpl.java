package com.qiandoudou.service.impl;

import com.qiandoudou.entity.BuddyCharacter;
import com.qiandoudou.entity.Transaction;
import com.qiandoudou.entity.Wallet;
import com.qiandoudou.mapper.BuddyCharacterMapper;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.BuddyWalletInitService;
import com.qiandoudou.service.TransactionService;
import com.qiandoudou.service.WalletService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 搭子钱包初始化服务实现类
 */
@Service
public class BuddyWalletInitServiceImpl implements BuddyWalletInitService {

    private static final Logger logger = LoggerFactory.getLogger(BuddyWalletInitServiceImpl.class);

    @Autowired
    private WalletService walletService;

    @Autowired
    private BuddyCharacterMapper buddyCharacterMapper;

    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private AiService aiService;

    @Override
    public void initializeBuddyWalletSync(Long walletId, List<Long> buddyCharacterIds) {
        try {
            logger.info("开始同步初始化搭子钱包，钱包ID: {}, 搭子数量: {}", walletId, buddyCharacterIds.size());

            // 等待确保钱包创建事务已提交
            Thread.sleep(1000);

            // 验证钱包是否存在
            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                logger.error("钱包不存在，无法初始化，钱包ID: {}", walletId);
                return;
            }

            logger.info("钱包验证成功，当前余额: {}, 开始为每个搭子创建初始转账", wallet.getBalance());

            for (Long buddyCharacterId : buddyCharacterIds) {
                try {
                    BuddyCharacter buddyCharacter = buddyCharacterMapper.selectById(buddyCharacterId);
                    if (buddyCharacter == null) {
                        logger.warn("搭子角色不存在，跳过，ID: {}", buddyCharacterId);
                        continue;
                    }

                    // 调用AI生成个性化欢迎消息
                    String welcomeMessage = generateBuddyWelcomeMessage(buddyCharacter, wallet.getName());

                    logger.info("开始为搭子 {} 创建初始转账，当前钱包余额: {}", buddyCharacter.getName(), wallet.getBalance());

                    // 直接创建带有搭子信息的交易记录
                    createBuddyInitialTransaction(wallet, buddyCharacter, welcomeMessage);

                    logger.info("搭子 {} 初始转账成功", buddyCharacter.getName());

                    // 添加间隔，避免并发问题
                    Thread.sleep(500);

                } catch (Exception e) {
                    logger.error("搭子初始转账失败，搭子ID: {}, 错误: ", buddyCharacterId, e);
                }
            }

            logger.info("搭子钱包同步初始化完成，钱包ID: {}", walletId);

        } catch (Exception e) {
            logger.error("搭子钱包同步初始化失败，钱包ID: {}", walletId, e);
        }
    }

    @Override
    @Async
    public void initializeBuddyWalletAsync(Long walletId, List<Long> buddyCharacterIds) {
        // 使用分布式锁避免与用户转账冲突
        String lockKey = "wallet_init_" + walletId;

        try {
            logger.info("开始异步初始化搭子钱包，钱包ID: {}, 搭子数量: {}", walletId, buddyCharacterIds.size());

            // 等待更长时间确保钱包创建和用户转账事务都已提交
            Thread.sleep(5000);

            // 验证钱包是否存在
            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                logger.error("钱包不存在，无法初始化，钱包ID: {}", walletId);
                return;
            }
            
            logger.info("钱包验证成功，开始为每个搭子创建初始转账");
            
            for (Long buddyCharacterId : buddyCharacterIds) {
                try {
                    BuddyCharacter buddyCharacter = buddyCharacterMapper.selectById(buddyCharacterId);
                    if (buddyCharacter == null) {
                        logger.warn("搭子角色不存在，跳过，ID: {}", buddyCharacterId);
                        continue;
                    }

                    // 使用已获取的钱包信息生成个性化欢迎消息
                    String welcomeMessage = generateBuddyWelcomeMessage(buddyCharacter, wallet.getName());
                    
                    logger.info("开始为搭子 {} 创建初始转账", buddyCharacter.getName());
                    
                    // 直接创建带有搭子信息的交易记录
                    createBuddyInitialTransaction(wallet, buddyCharacter, welcomeMessage);
                    
                    logger.info("搭子 {} 初始转账成功", buddyCharacter.getName());
                    
                    // 添加间隔，避免并发问题
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    logger.error("搭子初始转账失败，搭子ID: {}, 错误: ", buddyCharacterId, e);
                }
            }
            
            logger.info("搭子钱包异步初始化完成，钱包ID: {}", walletId);
            
        } catch (Exception e) {
            logger.error("搭子钱包异步初始化失败，钱包ID: {}", walletId, e);
        }
    }

    /**
     * 为搭子创建初始转账记录
     * 统一使用 walletService.transferIn() 方法，确保交易记录正确创建
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createBuddyInitialTransaction(Wallet wallet, BuddyCharacter buddyCharacter, String welcomeMessage) {
        try {
            BigDecimal initialAmount = new BigDecimal("0.01");

            logger.info("开始为搭子 {} 创建初始转账，钱包ID: {}, 金额: {}", buddyCharacter.getName(), wallet.getId(), initialAmount);

            // 生成搭子语音
            String voiceUrl = null;
            try {
                logger.info("开始为搭子 {} 生成语音，消息内容: {}", buddyCharacter.getName(), welcomeMessage);
                voiceUrl = aiService.generatePartnerVoiceByCharacterName(buddyCharacter.getName(), welcomeMessage);
                if (voiceUrl != null) {
                    logger.info("搭子语音生成成功，URL: {}", voiceUrl);
                } else {
                    logger.warn("搭子语音生成失败，返回null");
                }
            } catch (Exception e) {
                logger.warn("搭子语音生成异常: {}", e.getMessage());
            }

            // 获取当前钱包最新余额，用于设置交易记录
            Wallet currentWallet = walletService.getById(wallet.getId());
            BigDecimal currentBalance = currentWallet != null ? currentWallet.getBalance() : wallet.getBalance();
            BigDecimal newBalance = currentBalance.add(initialAmount);

            logger.info("当��钱包余额: {}, 准备为搭子 {} 增加: {}, 新余额: {}", currentBalance, buddyCharacter.getName(), initialAmount, newBalance);

            // 直接创建交易记录，避免使用transferIn方法可能产生的并发问题
            Transaction transaction = new Transaction();
            transaction.setWalletId(wallet.getId());
            transaction.setUserId(wallet.getUserId());
            transaction.setType(1); // 转入类型
            transaction.setAmount(initialAmount);
            transaction.setBalanceAfter(newBalance);
            transaction.setDescription("搭子初始转账");
            transaction.setNote(welcomeMessage);

            // 设置搭子相关字段
            transaction.setAiPartnerName(buddyCharacter.getName());
            transaction.setAiPartnerAvatar(buddyCharacter.getAvatar());
            transaction.setAiMessage(welcomeMessage);

            // 设置语音URL和时长
            if (voiceUrl != null) {
                transaction.setVoiceUrl(voiceUrl);
                transaction.setVoiceDuration("12s"); // 预估时长
            }

            transaction.setCreateTime(java.time.LocalDateTime.now());
            transactionService.save(transaction);

            logger.info("搭子交易记录创建成功，交易ID: {}, 搭子: {}, 金额: {}, 余额: {}",
                transaction.getId(), buddyCharacter.getName(), initialAmount, newBalance);

            // 最后更新钱包余额，确保原子操作
            walletService.incrementWalletBalance(wallet.getId(), initialAmount);

            logger.info("搭子 {} 初始转账成功，使用统一转账接口", buddyCharacter.getName());

        } catch (Exception e) {
            logger.error("创建搭子初始转账记录失败，搭子: {}", buddyCharacter.getName(), e);
            throw e;
        }
    }
    
    /**
     * 生成搭子欢迎消息
     */
    private String generateBuddyWelcomeMessage(BuddyCharacter buddyCharacter, String walletName) {
        logger.info("开始生成搭子欢迎消息，搭子: {}, 钱包: {}", buddyCharacter.getName(), walletName);
        logger.info("搭子角色详情 - ID: {}, 名称: {}, 性格: {}", buddyCharacter.getId(), buddyCharacter.getName(), buddyCharacter.getPersonality());
        
        // 检查personality字段
        if (buddyCharacter.getPersonality() == null || buddyCharacter.getPersonality().trim().isEmpty()) {
            logger.warn("搭子角色 {} 的personality字段为空，无法生成个性化消息", buddyCharacter.getName());
            String defaultMessage = String.format("大家好！我是%s，很高兴加入这个搭子攒钱计划！让我们一起努力存钱吧！💪", buddyCharacter.getName());
            logger.info("使用默认欢迎消息: {}", defaultMessage);
            return defaultMessage;
        }
        
        try {
            // 构建欢迎提示词
            String prompt = String.format(
                "你是%s，%s。现在加入了一个新的搭子攒钱钱包'%s'，请生成一条欢迎加入的消息，表达你对这次搭子攒钱计划的期待和支持。消息要体现你的性格特点，控制在50字以内。",
                buddyCharacter.getName(),
                buddyCharacter.getPersonality(),
                walletName
            );

            logger.info("AI提示词: {}", prompt);
            logger.info("准备调用AI服务: aiService = {}", aiService != null ? "已注入" : "未注入");

            // 调用AI生成服务 - 使用文本生成方法
            String welcomeMessage = aiService.generateAiText(prompt);
            logger.info("AI生成结果: {}", welcomeMessage);
            
            if (welcomeMessage != null && !welcomeMessage.trim().isEmpty()) {
                logger.info("AI生成搭子欢迎消息成功: {}", welcomeMessage.trim());
                return welcomeMessage.trim();
            } else {
                logger.warn("AI返回了空消息，使用默认消息");
            }

        } catch (Exception e) {
            logger.warn("AI生成搭子欢迎消息失败: {}", e.getMessage(), e);
        }

        // 如果AI生成失败，返回默认消息
        String defaultMessage = String.format("大家好！我是%s，很高兴加入这个搭子攒钱计划！让我们一起努力存钱吧！💪", buddyCharacter.getName());
        logger.info("使用默认欢迎消息: {}", defaultMessage);
        return defaultMessage;
    }
}
