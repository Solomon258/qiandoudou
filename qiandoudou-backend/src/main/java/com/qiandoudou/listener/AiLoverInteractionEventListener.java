package com.qiandoudou.listener;

import com.qiandoudou.event.TransactionCreatedEvent;
import com.qiandoudou.service.AiLoverInteractionService;
import com.qiandoudou.service.BuddyAutoCommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * AI互动事件监听器（支持情侣和搭子互动）
 */
@Component
public class AiLoverInteractionEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(AiLoverInteractionEventListener.class);
    
    @Autowired
    private AiLoverInteractionService aiLoverInteractionService;

    @Autowired
    private BuddyAutoCommentService buddyAutoCommentService;
    
    /**
     * 监听交易创建事件，触发AI情侣互动
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        try {
            logger.info("监听到交易创建事件，交易ID: {}", event.getTransactionId());
            
            // 处理AI情侣钱包互动
            logger.info("开始处理AI情侣钱包互动...");
            aiLoverInteractionService.processAiLoverWalletInteraction(event.getTransactionId());
            
            // 处理搭子钱包互动
            logger.info("开始处理搭子钱包互动...");
            buddyAutoCommentService.generateBuddyAutoComments(event.getTransactionId());
            
        } catch (Exception e) {
            logger.error("处理AI互动事件失败，交易ID: {}", event.getTransactionId(), e);
        }
    }
}
