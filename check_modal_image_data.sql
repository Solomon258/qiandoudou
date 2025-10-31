-- 检查 dream_items 表的 modal_image_url 数据

-- 查看所有购物商品的 modal_image_url
SELECT
    id,
    category,
    name,
    display_name,
    image_url,
    modal_image_url,
    is_active
FROM dream_items
WHERE category = 1 AND deleted = 0
ORDER BY sort_order;

-- 查看所有旅行目的地的 modal_image_url
SELECT
    id,
    category,
    name,
    display_name,
    image_url,
    modal_image_url,
    is_active
FROM dream_items
WHERE category = 2 AND deleted = 0
ORDER BY sort_order;

-- 检查轿车的 modal_image_url（应该是 /static/icon/sel-car.png）
SELECT
    id,
    name,
    display_name,
    image_url,
    modal_image_url
FROM dream_items
WHERE name = 'car' AND category = 1 AND deleted = 0;
