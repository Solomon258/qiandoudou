-- 更新轿车的 modal_image_url 为 /static/icon/sel-car.png

-- 更新 dream_items 表
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-car.png'
WHERE `name` = 'car' AND `category` = 1 AND `deleted` = 0;

-- 验证更新结果
SELECT
    id,
    name,
    display_name,
    image_url,
    modal_image_url
FROM `dream_items`
WHERE `name` = 'car' AND `category` = 1 AND `deleted` = 0;

-- 可选：如果你也在 dream_image_config 表中配置了数据，也需要更新
UPDATE `dream_image_config`
SET `modal_image_url` = '/static/icon/sel-car.png'
WHERE `item_name` = 'car' AND `dream_type` = 1 AND `deleted` = 0;

-- 验证 dream_image_config 表的更新结果
SELECT
    id,
    item_name,
    item_display_name,
    modal_image_url,
    progress_image_base_url,
    share_image_url
FROM `dream_image_config`
WHERE `item_name` = 'car' AND `dream_type` = 1 AND `deleted` = 0;
