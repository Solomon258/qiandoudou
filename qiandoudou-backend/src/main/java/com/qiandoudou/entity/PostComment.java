package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 动态评论实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("post_comments")
public class PostComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 动态ID（这里指向交易ID）
     */
    @TableField("post_id")
    private Long postId;

    /**
     * 评论用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论图片（JSON格式）
     */
    private String images;

    /**
     * 语音URL（AI伴侣专用）
     */
    @TableField("voice_url")
    private String voiceUrl;

    /**
     * 是否AI评论：0-否，1-是
     */
    @TableField("is_ai_comment")
    private Integer isAiComment;

    /**
     * AI伴侣ID
     */
    @TableField("ai_partner_id")
    private Long aiPartnerId;

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
