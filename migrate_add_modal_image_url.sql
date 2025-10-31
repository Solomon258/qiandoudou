-- 迁移脚本：为dream_image_config表添加modal_image_url字段

-- 如果表存在，添加modal_image_url列
ALTER TABLE `dream_image_config`
ADD COLUMN `modal_image_url` VARCHAR(500) COMMENT '弹框中显示的商品图片URL' AFTER `share_image_url`;

-- 为已存在的数据更新modal_image_url
-- 购物商品
UPDATE `dream_image_config`
SET `modal_image_url` = CONCAT(SUBSTRING_INDEX(`progress_image_base_url`, '/', CHAR_LENGTH(`progress_image_base_url`) - CHAR_LENGTH(REPLACE(`progress_image_base_url`, '/', ''))), '/modal.png')
WHERE `dream_type` = 1 AND `modal_image_url` IS NULL;

-- 旅行目的地
UPDATE `dream_image_config`
SET `modal_image_url` = CONCAT(SUBSTRING_INDEX(`progress_image_base_url`, '/', CHAR_LENGTH(`progress_image_base_url`) - CHAR_LENGTH(REPLACE(`progress_image_base_url`, '/', ''))), '/modal.png')
WHERE `dream_type` = 2 AND `modal_image_url` IS NULL;
