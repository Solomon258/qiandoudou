-- 更新所有购物商品的 modal_image_url

-- 轿车
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-car.png'
WHERE `name` = 'car' AND `category` = 1 AND `deleted` = 0;

-- SUV
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-suv.png'
WHERE `name` = 'suv' AND `category` = 1 AND `deleted` = 0;

-- 超跑
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-supercar.png'
WHERE `name` = 'supercar' AND `category` = 1 AND `deleted` = 0;

-- 包包
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-bag.png'
WHERE `name` = 'bag' AND `category` = 1 AND `deleted` = 0;

-- 劳力士
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-rolex.png'
WHERE `name` = 'rolex' AND `category` = 1 AND `deleted` = 0;

-- 更新所有旅行目的地的 modal_image_url
-- 北京
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-beijing.png'
WHERE `name` = 'beijing' AND `category` = 2 AND `deleted` = 0;

-- 西安
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-xian.png'
WHERE `name` = 'xian' AND `category` = 2 AND `deleted` = 0;

-- 乌鲁木齐
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-urumqi.png'
WHERE `name` = 'urumqi' AND `category` = 2 AND `deleted` = 0;

-- 悉尼
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-sydney.png'
WHERE `name` = 'sydney' AND `category` = 2 AND `deleted` = 0;

-- 东京
UPDATE `dream_items`
SET `modal_image_url` = '/static/icon/sel-tokyo.png'
WHERE `name` = 'tokyo' AND `category` = 2 AND `deleted` = 0;

-- 验证所有更新
SELECT
    category,
    name,
    display_name,
    modal_image_url
FROM `dream_items`
WHERE `deleted` = 0
ORDER BY category, sort_order;
