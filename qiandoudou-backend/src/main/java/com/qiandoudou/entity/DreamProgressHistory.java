package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 梦想进度历史实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dream_progress_history")
public class DreamProgressHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 历史记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 钱包ID
     */
    @TableField("wallet_id")
    private Long walletId;

    /**
     * 原进度百分比
     */
    @TableField("old_progress")
    private Integer oldProgress;

    /**
     * 新进度百分比
     */
    @TableField("new_progress")
    private Integer newProgress;

    /**
     * 达到的里程碑（20,40,60,80,100）
     */
    @TableField("milestone_reached")
    private Integer milestoneReached;

    /**
     * 触发进度变化的金额
     */
    @TableField("trigger_amount")
    private BigDecimal triggerAmount;

    /**
     * 触发类型：1-转入，2-转出
     */
    @TableField("trigger_type")
    private Integer triggerType;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
