package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搭子互动记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("buddy_interactions")
public class BuddyInteraction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 互动ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 钱包ID
     */
    @TableField("wallet_id")
    private Long walletId;

    /**
     * 搭子角色ID
     */
    @TableField("character_id")
    private Long characterId;

    /**
     * 交易ID
     */
    @TableField("transaction_id")
    private Long transactionId;

    /**
     * 互动类型：1-评论，2-点赞，3-语音
     */
    @TableField("interaction_type")
    private Integer interactionType;

    /**
     * 互动内容
     */
    private String content;

    /**
     * 语音URL
     */
    @TableField("voice_url")
    private String voiceUrl;

    /**
     * 语音时长
     */
    @TableField("voice_duration")
    private String voiceDuration;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 是否删除：0-否，1-是
     */
    @TableLogic
    private Integer deleted;
}
