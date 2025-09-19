package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搭子圈子实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("buddy_groups")
public class BuddyGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 圈子ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 圈子名称
     */
    private String name;

    /**
     * 圈子描述
     */
    private String description;

    /**
     * 圈子封面图
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 圈子背景图
     */
    @TableField("background_image")
    private String backgroundImage;

    /**
     * 成员数量
     */
    @TableField("member_count")
    private Integer memberCount;

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
