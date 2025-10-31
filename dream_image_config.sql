-- 梦想攒图片配置表
-- 用于存放梦想攒（购物和旅游）所有需要的图片地址
CREATE TABLE `dream_image_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dream_type` TINYINT NOT NULL COMMENT '梦想类型：1-购物，2-旅游',
  `item_name` VARCHAR(100) NOT NULL COMMENT '商品或目的地名称（如：car、suv、bag、beijing等）',
  `item_display_name` VARCHAR(100) NOT NULL COMMENT '商品或目的地显示名称（如：轿车、北京）',
  `progress_image_base_url` VARCHAR(500) NOT NULL COMMENT '进度图片URL前缀（如：https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/）',
  `share_image_url` VARCHAR(500) NOT NULL COMMENT '分享图片完整URL（如：https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png）',
  `modal_image_url` VARCHAR(500) COMMENT '弹框中显示的商品图片URL（如：https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/modal.png）',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否启用：0-否，1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_name` (`dream_type`, `item_name`, `deleted`),
  KEY `idx_dream_type` (`dream_type`),
  KEY `idx_item_name` (`item_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='梦想攒图片配置表';

-- 汽车商品的插入语句示例
INSERT INTO `dream_image_config` (
  `dream_type`,
  `item_name`,
  `item_display_name`,
  `progress_image_base_url`,
  `share_image_url`,
  `modal_image_url`,
  `is_active`
) VALUES (
  1,
  'car',
  '轿车',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/modal.png',
  1
);

-- 其他购物商品示例
INSERT INTO `dream_image_config` (
  `dream_type`,
  `item_name`,
  `item_display_name`,
  `progress_image_base_url`,
  `share_image_url`,
  `modal_image_url`,
  `is_active`
) VALUES
(1, 'suv', 'SUV', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/suv/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/suv/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/suv/modal.png', 1),
(1, 'supercar', '超跑', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/supercar/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/supercar/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/supercar/modal.png', 1),
(1, 'bag', '包包', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/bag/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/bag/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/bag/modal.png', 1),
(1, 'rolex', '劳力士', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/rolex/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/rolex/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/rolex/modal.png', 1);

-- 旅游目的地示例
INSERT INTO `dream_image_config` (
  `dream_type`,
  `item_name`,
  `item_display_name`,
  `progress_image_base_url`,
  `share_image_url`,
  `modal_image_url`,
  `is_active`
) VALUES
(2, 'beijing', '北京', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/beijing/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/beijing/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/beijing/modal.png', 1),
(2, 'xian', '西安', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/xian/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/xian/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/xian/modal.png', 1),
(2, 'lasa', '拉萨', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/lasa/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/lasa/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/lasa/modal.png', 1),
(2, 'sydney', '悉尼', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/sydney/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/sydney/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/sydney/modal.png', 1),
(2, 'singapore', '新加坡', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/singapore/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/singapore/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/singapore/modal.png', 1),
(2, 'agra', '阿格拉市', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/agra/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/agra/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/agra/modal.png', 1),
(2, 'chongqing', '重庆', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/chongqing/', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/chongqing/share.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/chongqing/modal.png', 1);
