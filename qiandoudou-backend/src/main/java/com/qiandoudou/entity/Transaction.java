package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("transactions")
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 交易ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 钱包ID
     */
    @TableField("wallet_id")
    private Long walletId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 交易类型：1-转入，2-转出，3-剧本攒钱
     */
    private Integer type;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 交易后余额
     */
    @TableField("balance_after")
    private BigDecimal balanceAfter;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 交易图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 交易备注
     */
    private String note;

    /**
     * 关联的剧本章节ID（剧本攒钱专用）
     */
    @TableField("script_chapter_id")
    private Long scriptChapterId;

    /**
     * AI伴侣ID（AI伴侣交易专用）
     */
    @TableField("ai_partner_id")
    private Long aiPartnerId;

    /**
     * AI伴侣名称
     */
    @TableField("ai_partner_name")
    private String aiPartnerName;

    /**
     * 剧本封面图片URL（剧本攒钱专用，非数据库字段）
     */
    @TableField(exist = false)
    private String scriptCoverImage;

    /**
     * 剧本标题（剧本攒钱专用，非数据库字段）
     */
    @TableField(exist = false)
    private String scriptTitle;

    /**
     * AI伴侣头像
     */
    @TableField("ai_partner_avatar")
    private String aiPartnerAvatar;

    /**
     * AI伴侣消息内容
     */
    @TableField("ai_message")
    private String aiMessage;

    /**
     * 语音URL（AI伴侣专用）
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
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-否，1-是
     */
    @TableLogic
    private Integer deleted;
}
