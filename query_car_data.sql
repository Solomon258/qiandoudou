-- 查看轿车的所有字段值
SELECT
    id,
    category,
    name,
    display_name,
    image_url,
    bubble_image_url,
    modal_image_url,
    is_active,
    deleted
FROM dream_items
WHERE name = 'car' AND category = 1;
