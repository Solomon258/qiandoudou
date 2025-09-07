package com.qiandoudou.listener;

import com.qiandoudou.event.TransactionCreatedEvent;
import com.qiandoudou.service.AiLoverInteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * AI情侣互动事件监听器
 */
@Component
public class AiLoverInteractionEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(AiLoverInteractionEventListener.class);
    
    @Autowired
    private AiLoverInteractionService aiLoverInteractionService;
    
    /**
     * 监听交易创建事件，触发AI情侣互动
     */
    @EventListener
    @Async
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        try {
            logger.info("监听到交易创建事件，交易ID: {}", event.getTransactionId());
            aiLoverInteractionService.processAiLoverWalletInteraction(event.getTransactionId());
        } catch (Exception e) {
            logger.error("处理AI情侣钱包互动事件失败，交易ID: {}", event.getTransactionId(), e);
        }
    }
}
