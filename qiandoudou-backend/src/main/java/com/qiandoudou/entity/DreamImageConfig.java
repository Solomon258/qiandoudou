package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 梦想攒图片配置实体
 * 用于存放梦想攒（购物和旅游）所有需要的图片地址
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dream_image_config")
public class DreamImageConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 梦想类型：1-购物，2-旅游
     */
    private Integer dreamType;

    /**
     * 商品或目的地名称（如：car、suv、bag、beijing等）
     */
    private String itemName;

    /**
     * 商品或目的地显示名称（如：轿车、北京）
     */
    private String itemDisplayName;

    /**
     * 进度图片URL前缀
     * 例如：https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/
     * 后端会自动拼接 part1.png、part2.png 等文件名
     */
    private String progressImageBaseUrl;

    /**
     * 分享图片完整URL
     * 例如：https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png
     */
    private String shareImageUrl;

    /**
     * 弹框中显示的商品图片URL
     * 例如：https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/modal.png
     */
    private String modalImageUrl;

    /**
     * 是否启用：0-否，1-是
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-否，1-是
     */
    private Integer deleted;
}
