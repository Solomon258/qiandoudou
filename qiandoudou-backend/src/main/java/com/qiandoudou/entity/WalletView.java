package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 钱包浏览记录实体类
 */
@Data
@TableName("wallet_views")
public class WalletView {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 浏览者用户ID
     */
    private Long userId;

    /**
     * 被浏览的钱包ID
     */
    private Long walletId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
