package com.qiandoudou.event;

import org.springframework.context.ApplicationEvent;

/**
 * 交易创建事件
 */
public class TransactionCreatedEvent extends ApplicationEvent {
    
    private final Long transactionId;
    
    public TransactionCreatedEvent(Object source, Long transactionId) {
        super(source);
        this.transactionId = transactionId;
    }
    
    public Long getTransactionId() {
        return transactionId;
    }
}
