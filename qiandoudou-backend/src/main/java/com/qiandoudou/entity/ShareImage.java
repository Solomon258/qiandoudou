package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分享图片实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("share_images")
public class ShareImage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分享类型：wallet=钱兜兜分享, script=剧本分享
     */
    private String type;

    /**
     * 目标ID：钱包ID或剧本ID，NULL表示通用分享
     */
    private Long targetId;

    /**
     * 目标名称：钱包名称或剧本名称
     */
    private String targetName;

    /**
     * 分享图片的OSS地址
     */
    private String imageUrl;

    /**
     * 分享标题
     */
    private String title;

    /**
     * 分享描述
     */
    private String description;

    /**
     * 状态：1=启用，0=禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
