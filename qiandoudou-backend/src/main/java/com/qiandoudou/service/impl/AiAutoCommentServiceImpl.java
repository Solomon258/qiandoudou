package com.qiandoudou.service.impl;

import com.qiandoudou.entity.AiPartner;
import com.qiandoudou.entity.Transaction;
import com.qiandoudou.entity.Wallet;
import com.qiandoudou.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI自动评论服务实现类
 * 复用现有的AI生成和语音生成逻辑
 */
@Service
public class AiAutoCommentServiceImpl implements AiAutoCommentService {

    private static final Logger logger = LoggerFactory.getLogger(AiAutoCommentServiceImpl.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private AiPartnerService aiPartnerService;

    @Autowired
    private AiService aiService;

    @Autowired
    private SocialService socialService;

    @Override
    public Map<String, Object> generateAutoComment(Long transactionId, Long walletId, Long userId) {
        try {
            logger.info("=== 开始生成AI自动评论 ===");
            logger.info("参数 - 交易ID: {}, 钱包ID: {}, 用户ID: {}", transactionId, walletId, userId);

            // 1. 获取交易信息
            Transaction transaction = transactionService.getById(transactionId);
            if (transaction == null) {
                throw new RuntimeException("交易记录不存在: " + transactionId);
            }

            // 2. 获取钱包信息
            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                throw new RuntimeException("钱包不存在: " + walletId);
            }

            // 3. 获取AI伴侣信息
            AiPartner aiPartner = null;
            if (wallet.getAiPartnerId() != null) {
                aiPartner = aiPartnerService.getById(wallet.getAiPartnerId());
            }
            
            if (aiPartner == null) {
                logger.warn("钱包 {} 没有关联AI伴侣，跳过AI评论生成", walletId);
                return null;
            }

            logger.info("AI伴侣信息 - ID: {}, 名称: {}, 性格: {}", 
                       aiPartner.getId(), aiPartner.getName(), aiPartner.getPersonality());

            // 4. 构建交易描述用于AI生成
            String transactionDescription = buildTransactionDescription(transaction);
            logger.info("交易描述: {}", transactionDescription);

            // 5. 使用AiService生成真正的AI评论内容（支持图片）
            String commentContent;
            if (transaction.getImageUrl() != null && !transaction.getImageUrl().trim().isEmpty()) {
                // 有图片时调用支持图片的方法
                commentContent = ((com.qiandoudou.service.impl.AiServiceImpl) aiService)
                    .generatePartnerCommentWithImage(aiPartner.getId(), transactionDescription, transaction.getImageUrl());
            } else {
                // 无图片时调用原有方法
                commentContent = aiService.generatePartnerComment(aiPartner.getId(), transactionDescription);
            }
            logger.info("AI生成的评论内容完成");

            // 6. 使用现有的语音生成服务
            String voiceUrl = null;
            try {
                voiceUrl = aiService.generatePartnerVoiceByCharacterName(aiPartner.getName(), commentContent);
                if (voiceUrl != null) {
                    logger.info("语音生成成功: {}", voiceUrl);
                } else {
                    logger.warn("语音生成失败，将只保存文字评论");
                }
            } catch (Exception e) {
                logger.warn("语音生成异常，将只保存文字评论: {}", e.getMessage());
            }

            // 7. 使用现有的社交服务保存AI评论记录
            Map<String, Object> comment = socialService.aiCommentTransaction(
                transactionId, commentContent, aiPartner.getId(), voiceUrl);

            // 8. 补充AI伴侣信息
            comment.put("aiPartnerName", aiPartner.getName());
            comment.put("aiPartnerAvatar", aiPartner.getAvatar());
            comment.put("isAiComment", true);

            logger.info("=== AI自动评论生成完成 ===");
            logger.info("评论ID: {}, 语音URL: {}", 
                       comment.get("id"), voiceUrl);

            return comment;

        } catch (Exception e) {
            logger.error("生成AI自动评论失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成AI评论失败: " + e.getMessage());
        }
    }

    /**
     * 构建交易描述，用于AI生成评论
     */
    private String buildTransactionDescription(Transaction transaction) {
        StringBuilder description = new StringBuilder();
        
        // 交易类型
        if (transaction.getType() == 1) {
            description.append("转入了 ");
        } else if (transaction.getType() == 2) {
            description.append("转出了 ");
        } else {
            description.append("进行了一笔 ");
        }
        
        // 金额
        description.append(transaction.getAmount()).append(" 元");
        
        // 交易描述
        if (transaction.getDescription() != null && !transaction.getDescription().trim().isEmpty()) {
            description.append("，备注：").append(transaction.getDescription());
        }
        
        // 交易备注
        if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
            description.append("，说明：").append(transaction.getNote());
        }
        
        return description.toString();
    }

}
