package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wallets")
public class Wallet implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 钱包ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 钱包名称
     */
    private String name;

    /**
     * 钱包类型：1-个人钱包，2-情侣钱包，3-梦想攒钱(购物)，4-梦想攒钱(旅行)
     */
    private Integer type;

    /**
     * 余额
     */
    private BigDecimal balance;

    /**
     * 背景图片URL
     */
    @TableField("background_image")
    private String backgroundImage;

    /**
     * AI伴侣ID（情侣钱包专用）
     */
    @TableField("ai_partner_id")
    private Long aiPartnerId;

    /**
     * 是否公开到社交圈：0-否，1-是
     */
    @TableField("is_public")
    private Integer isPublic;

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

    // ========== 梦想攒钱相关字段 ==========

    /**
     * 梦想类型：1-购物，2-旅行
     */
    @TableField("dream_type")
    private Integer dreamType;

    /**
     * 梦想目标金额
     */
    @TableField("dream_target_amount")
    private BigDecimal dreamTargetAmount;

    /**
     * 梦想物品名称
     */
    @TableField("dream_item_name")
    private String dreamItemName;

    /**
     * 梦想物品图片URL
     */
    @TableField("dream_item_image")
    private String dreamItemImage;

    /**
     * 旅行目的地
     */
    @TableField("dream_destination")
    private String dreamDestination;

    /**
     * 旅行天数
     */
    @TableField("dream_days")
    private Integer dreamDays;

    /**
     * 旅行行程（AI生成）
     */
    @TableField("dream_itinerary")
    private String dreamItinerary;

    /**
     * 梦想进度百分比（0-100）
     */
    @TableField("dream_progress")
    private Integer dreamProgress;

    /**
     * 上次达到的里程碑（0,20,40,60,80,100）
     */
    @TableField("dream_last_milestone")
    private Integer dreamLastMilestone;
}
