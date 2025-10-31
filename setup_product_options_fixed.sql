-- 修复版本：完整的产品选项配置设置脚本
-- 移除了 sort_order 字段的引用

-- Step 1: 添加 option_config 字段到 dream_items 表
ALTER TABLE `dream_items`
ADD COLUMN `option_config` JSON NULL COMMENT '产品选项配置JSON，包含品牌选项和建议金额数组';

-- Step 2: 为所有现有产品配置 option_config
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
-- 先检查 dream_image_config 表是否存在 sort_order 字段
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

-- Step 4: 验证配置结果（移除 sort_order 排序）
SELECT
    di.id,
    di.name,
    di.display_name,
    di.category,
    di.modal_image_url,
    di.option_config,
    di.is_active,
    dic.modal_image_url AS config_modal_url,
    dic.progress_image_base_url,
    dic.share_image_url
FROM dream_items di
LEFT JOIN dream_image_config dic ON di.name = dic.item_name AND di.category = dic.dream_type
WHERE di.deleted = 0
ORDER BY di.category, di.id;

-- 显示设置完成消息
SELECT 'Product options configuration setup completed!' AS message,
       COUNT(*) AS total_products_updated
FROM dream_items
WHERE deleted = 0 AND option_config IS NOT NULL;