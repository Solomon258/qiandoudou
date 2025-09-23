package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 梦想奖励实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dream_rewards")
public class DreamReward implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 奖励ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 钱包ID
     */
    @TableField("wallet_id")
    private Long walletId;

    /**
     * 奖励类型：1-优惠券，2-任务徽章
     */
    @TableField("reward_type")
    private Integer rewardType;

    /**
     * 奖励标题
     */
    private String title;

    /**
     * 奖励描述
     */
    private String description;

    /**
     * 解锁进度百分比（20,40,60,80,100）
     */
    @TableField("unlock_progress")
    private Integer unlockProgress;

    /**
     * 解锁条件描述
     */
    @TableField("unlock_condition")
    private String unlockCondition;

    /**
     * 是否已解锁：0-否，1-是
     */
    @TableField("is_unlocked")
    private Integer isUnlocked;

    /**
     * 解锁时间
     */
    @TableField("unlock_time")
    private LocalDateTime unlockTime;

    /**
     * 奖励图标URL
     */
    @TableField("reward_image")
    private String rewardImage;

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
