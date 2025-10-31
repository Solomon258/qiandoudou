-- 检查并更新现有数据的脚本
-- 不添加字段，只更新配置数据

-- Step 1: 检查当前表结构和已有配置
SELECT
    'dream_items表结构检查' as info;
DESCRIBE `dream_items`;

SELECT
    '检查现有的option_config配置' as info;
SELECT
    id,
    name,
    display_name,
    category,
    option_config,
    is_active
FROM dream_items
WHERE deleted = 0 AND category = 1
ORDER BY id;

-- Step 2: 更新现有产品的 option_config 配置
-- 轿车 (car)
UPDATE `dream_items`
SET `option_config` = '{
  "options": [
    {"label": "法拉利", "suggestedAmounts": [1000000, 1500000, 2000000]},
    {"label": "兰博基尼", "suggestedAmounts": [1500000, 2000000, 3000000]},
    {"label": "特斯拉", "suggestedAmounts": [300000, 500000, 800000]}
  ]
}'
WHERE `name` = 'car' AND `category` = 1 AND `deleted` = 0;

-- SUV (suv)
UPDATE `dream_items`
SET `option_config` = '{
  "options": [
    {"label": "保时捷卡宴", "suggestedAmounts": [800000, 1200000, 1800000]},
    {"label": "路虎揽胜", "suggestedAmounts": [1000000, 1500000, 2000000]},
    {"label": "奔驰GLS", "suggestedAmounts": [900000, 1300000, 1700000]}
  ]
}'
WHERE `name` = 'suv' AND `category` = 1 AND `deleted` = 0;

-- 超跑 (supercar)
UPDATE `dream_items`
SET `option_config` = '{
  "options": [
    {"label": "布加迪", "suggestedAmounts": [20000000, 30000000, 50000000]},
    {"label": "迈凯伦", "suggestedAmounts": [3000000, 5000000, 8000000]},
    {"label": "兰博基尼Aventador", "suggestedAmounts": [5000000, 7000000, 10000000]}
  ]
}'
WHERE `name` = 'supercar' AND `category` = 1 AND `deleted` = 0;

-- 包包 (bag)
UPDATE `dream_items`
SET `option_config` = '{
  "options": [
    {"label": "爱马仕Birkin", "suggestedAmounts": [100000, 200000, 500000]},
    {"label": "香奈儿Classic", "suggestedAmounts": [50000, 80000, 120000]},
    {"label": "LV", "suggestedAmounts": [20000, 40000, 80000]}
  ]
}'
WHERE `name` = 'bag' AND `category` = 1 AND `deleted` = 0;

-- 劳力士 (rolex)
UPDATE `dream_items`
SET `option_config` = '{
  "options": [
    {"label": "Submariner", "suggestedAmounts": [70000, 100000, 150000]},
    {"label": "Daytona", "suggestedAmounts": [150000, 250000, 400000]},
    {"label": "GMT-Master II", "suggestedAmounts": [80000, 120000, 180000]}
  ]
}'
WHERE `name` = 'rolex' AND `category` = 1 AND `deleted` = 0;

-- 相机 (camera)
UPDATE `dream_items`
SET `option_config` = '{
  "options": [
    {"label": "徕卡M系列", "suggestedAmounts": [50000, 80000, 120000]},
    {"label": "哈苏X系列", "suggestedAmounts": [80000, 120000, 200000]},
    {"label": "索尼A1", "suggestedAmounts": [40000, 50000, 60000]}
  ]
}'
WHERE `name` = 'camera' AND `category` = 1 AND `deleted` = 0;

-- Step 3: 添加相机配置到 dream_image_config 表
INSERT IGNORE INTO `dream_image_config` (
    `item_name`,
    `item_display_name`,
    `dream_type`,
    `modal_image_url`,
    `progress_image_base_url`,
    `share_image_url`,
    `is_active`,
    `create_time`,
    `update_time`,
    `deleted`
) VALUES (
    'camera',
    '相机',
    1, -- 购物类型
    '/static/icon/sel-camera.png',
    '/static/icon/progress/progress-camera-{progress}.png',
    '/static/icon/share/share-camera.png',
    1, -- 启用
    NOW(),
    NOW(),
    0 -- 未删除
);

-- 更新已存在的相机配置
UPDATE `dream_image_config`
SET
    `modal_image_url` = '/static/icon/sel-camera.png',
    `progress_image_base_url` = '/static/icon/progress/progress-camera-{progress}.png',
    `share_image_url` = '/static/icon/share/share-camera.png',
    `update_time` = NOW()
WHERE `item_name` = 'camera' AND `dream_type` = 1 AND `deleted` = 0;

-- Step 4: 验证更新结果
SELECT
    '更新后的产品配置' as info;
SELECT
    id,
    name,
    display_name,
    category,
    option_config,
    is_active
FROM dream_items
WHERE deleted = 0 AND category = 1
ORDER BY id;

SELECT
    '相机配置检查' as info;
SELECT
    id,
    item_name,
    item_display_name,
    modal_image_url,
    progress_image_base_url,
    share_image_url,
    is_active,
    deleted
FROM dream_image_config
WHERE item_name = 'camera' AND dream_type = 1 AND deleted = 0;

-- 显示完成消息
SELECT 'Data update completed!' AS message,
       COUNT(*) AS total_products_updated
FROM dream_items
WHERE deleted = 0 AND option_config IS NOT NULL AND category = 1;