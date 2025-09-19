package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 钱包搭子关系实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wallet_buddies")
public class WalletBuddy implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关系ID
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
    @TableField("buddy_character_id")
    private Long buddyCharacterId;

    /**
     * 加入时间
     */
    @TableField("join_time")
    private LocalDateTime joinTime;

    /**
     * 是否活跃：0-否，1-是
     */
    @TableField("is_active")
    private Integer isActive;

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
