package com.qiandoudou.service.impl;

import com.qiandoudou.entity.Wallet;
import com.qiandoudou.entity.Transaction;
import com.qiandoudou.entity.AiPartner;
import com.qiandoudou.service.AiLoverInteractionService;
import com.qiandoudou.mapper.WalletMapper;
import com.qiandoudou.service.SocialService;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.TtsService;
import com.qiandoudou.service.AiPartnerService;
import com.qiandoudou.mapper.TransactionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI情侣互动服务实现类
 */
@Service
public class AiLoverInteractionServiceImpl implements AiLoverInteractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiLoverInteractionServiceImpl.class);
    
    @Autowired
    private WalletMapper walletMapper;
    
    @Autowired
    private SocialService socialService;
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private TtsService ttsService;
    
    @Autowired
    private AiPartnerService aiPartnerService;
    
    @Autowired
    private TransactionMapper transactionMapper;
    
    @Override
    public void processAiLoverWalletInteraction(Long transactionId) {
        try {
            logger.info("开始处理AI情侣钱包互动，交易ID: {}", transactionId);
            
            // 获取交易信息
            Transaction transaction = transactionMapper.selectById(transactionId);
            if (transaction == null) {
                logger.warn("交易记录不存在: {}", transactionId);
                return;
            }
            
            // 获取钱包信息
            Wallet wallet = walletMapper.selectById(transaction.getWalletId());
            if (wallet == null) {
                logger.warn("钱包不存在: {}", transaction.getWalletId());
                return;
            }
            
            // 检查是否为AI情侣钱包
            if (!isAiLoverWallet(wallet.getId())) {
                logger.debug("非AI情侣钱包，跳过互动处理: {}", wallet.getId());
                return;
            }
            
            // 获取AI情侣的用户ID
            Long aiLoverUserId = getAiLoverUserId(wallet.getId());
            if (aiLoverUserId == null) {
                logger.warn("无法获取AI情侣用户ID，钱包ID: {}", wallet.getId());
                return;
            }
            
            // 自动点赞
            try {
                socialService.likeTransaction(aiLoverUserId, transactionId);
                logger.info("AI情侣自动点赞成功，交易ID: {}", transactionId);
            } catch (Exception e) {
                logger.error("AI情侣自动点赞失败，交易ID: {}", transactionId, e);
            }
            
            // 生成评论内容
            String transactionType = getTransactionTypeText(transaction.getType());
            String description = transaction.getDescription();
            Double amount = transaction.getAmount().doubleValue();
            
            String commentText = aiService.generatePartnerComment(transactionType, description, amount);
            logger.info("AI情侣生成评论: {}", commentText);
            
            // 自动评论
            try {
                Long aiPartnerId = wallet.getAiPartnerId();
                if (aiPartnerId != null) {
                    // 生成评论语音
                    String voiceUrl = generateCommentVoice(commentText, aiPartnerId);
                    
                    // 使用AI专用的评论方法
                    Map<String, Object> commentResult = socialService.aiCommentTransaction(transactionId, commentText, aiPartnerId, voiceUrl);
                    logger.info("AI情侣自动评论成功，交易ID: {}, 评论ID: {}", transactionId, commentResult.get("id"));
                }
                
            } catch (Exception e) {
                logger.error("AI情侣自动评论失败，交易ID: {}", transactionId, e);
            }
            
        } catch (Exception e) {
            logger.error("处理AI情侣钱包互动失败，交易ID: {}", transactionId, e);
        }
    }
    
    @Override
    public boolean isAiLoverWallet(Long walletId) {
        try {
            Wallet wallet = walletMapper.selectById(walletId);
            if (wallet == null) {
                return false;
            }
            
            // AI情侣钱包的特征：
            // 1. 类型为情侣钱包 (type = 2)
            // 2. 有关联的AI伴侣ID
            return wallet.getType() == 2 && wallet.getAiPartnerId() != null;
            
        } catch (Exception e) {
            logger.error("检查AI情侣钱包失败，钱包ID: {}", walletId, e);
            return false;
        }
    }
    
    @Override
    public Long getAiLoverUserId(Long walletId) {
        try {
            Wallet wallet = walletMapper.selectById(walletId);
            if (wallet == null || wallet.getAiPartnerId() == null) {
                return null;
            }
            
            // 这里可以根据AI伴侣ID获取对应的虚拟用户ID
            // 为了简化，我们使用一个固定的AI用户ID
            // 在实际项目中，可以为每个AI伴侣创建一个对应的虚拟用户账号
            
            AiPartner aiPartner = aiPartnerService.getById(wallet.getAiPartnerId());
            if (aiPartner != null) {
                // 使用AI伴侣的ID作为虚拟用户ID（需要确保数据库中存在对应的用户记录）
                // 或者使用一个预设的AI系统用户ID
                return aiPartner.getId();
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("获取AI情侣用户ID失败，钱包ID: {}", walletId, e);
            return null;
        }
    }
    
    @Override
    public String generateCommentVoice(String commentText, Long aiPartnerId) {
        try {
            AiPartner aiPartner = aiPartnerService.getById(aiPartnerId);
            if (aiPartner == null) {
                logger.warn("AI伴侣不存在: {}", aiPartnerId);
                return null;
            }
            
            // 使用TTS服务生成语音
            byte[] voiceData = ttsService.generateVoice(commentText, aiPartner.getVoiceType());
            // 这里应该将语音数据保存到文件或上传到OSS，然后返回URL
            // 暂时返回模拟的URL
            String voiceUrl = "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/voice/ai_comment_" + System.currentTimeMillis() + ".mp3";
            logger.info("AI情侣评论语音生成成功，AI伴侣ID: {}, 语音URL: {}", aiPartnerId, voiceUrl);
            
            return voiceUrl;
            
        } catch (Exception e) {
            logger.error("生成AI情侣评论语音失败，AI伴侣ID: {}, 评论文本: {}", aiPartnerId, commentText, e);
            return null;
        }
    }
    
    /**
     * 获取交易类型文本描述
     */
    private String getTransactionTypeText(Integer type) {
        switch (type) {
            case 1:
                return "转入";
            case 2:
                return "转出";
            case 3:
                return "剧本攒钱";
            case 4:
                return "AI伴侣转账";
            default:
                return "其他";
        }
    }
}
