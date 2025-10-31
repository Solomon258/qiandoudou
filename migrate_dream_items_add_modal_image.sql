-- 迁移脚本：为dream_items表添加modal_image_url字段

-- 如果表存在，添加modal_image_url列（如果还不存在）
ALTER TABLE `dream_items`
ADD COLUMN `modal_image_url` VARCHAR(500) DEFAULT NULL COMMENT '弹框中显示的商品图片URL' AFTER `bubble_image_url`;

-- 为已存在的数据更新modal_image_url，使用image_url作为默认值
UPDATE `dream_items`
SET `modal_image_url` = `image_url`
WHERE `modal_image_url` IS NULL;

-- 可选：为特定商品设置特殊的modal_image_url（如需要不同的图片）
-- 示例：
-- UPDATE `dream_items`
-- SET `modal_image_url` = '/static/icon/goods/sel-car.png'
-- WHERE `name` = 'car' AND `category` = 1;
