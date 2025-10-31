-- Populate option_config for existing products in dream_items table
-- Each product gets specific options with corresponding suggested amounts

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

-- 验证配置结果
SELECT
    id,
    name,
    display_name,
    category,
    option_config,
    is_active,
    deleted
FROM `dream_items`
WHERE `deleted` = 0
ORDER BY category, sort_order;