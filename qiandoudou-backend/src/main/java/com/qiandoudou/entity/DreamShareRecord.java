package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 梦想分享记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dream_share_records")
public class DreamShareRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分享记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 分享类型：1-进度分享，2-完成分享
     */
    @TableField("share_type")
    private Integer shareType;

    /**
     * 分享时的进度百分比
     */
    @TableField("share_progress")
    private Integer shareProgress;

    /**
     * 分享图片URL
     */
    @TableField("share_image_url")
    private String shareImageUrl;

    /**
     * 分享文案
     */
    @TableField("share_text")
    private String shareText;

    /**
     * 分享平台
     */
    @TableField("share_platform")
    private String sharePlatform;

    /**
     * 分享时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 是否删除：0-否，1-是
     */
    @TableLogic
    private Integer deleted;
}
