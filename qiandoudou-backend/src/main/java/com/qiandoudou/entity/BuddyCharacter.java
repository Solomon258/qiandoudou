package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搭子角色实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("buddy_characters")
public class BuddyCharacter implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色昵称
     */
    private String nickname;

    /**
     * 角色头像URL
     */
    private String avatar;

    /**
     * 角色全身图URL
     */
    @TableField("full_image")
    private String fullImage;

    /**
     * 性别：1-男，2-女
     */
    private Integer gender;

    /**
     * 性格设定（AI提示词）
     */
    private String personality;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色标签JSON数组
     */
    private String tags;

    /**
     * 语音样例URL
     */
    @TableField("voice_sample_url")
    private String voiceSampleUrl;

    /**
     * 语音时长
     */
    @TableField("voice_duration")
    private String voiceDuration;

    /**
     * 所属角色组ID（圈子）
     */
    @TableField("character_group_id")
    private Long characterGroupId;

    /**
     * 排序权重
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否启用：0-否，1-是
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
