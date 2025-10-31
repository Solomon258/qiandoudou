-- 检查相机和劳力士的配置差异

SELECT
    'rolex配置' as product,
    id,
    item_name,
    item_display_name,
    modal_image_url,
    progress_image_base_url,
    share_image_url,
    is_active,
    deleted
FROM `dream_image_config`
WHERE `item_name` = 'rolex' AND `dream_type` = 1 AND `deleted` = 0

UNION ALL

SELECT
    'camera配置' as product,
    id,
    item_name,
    item_display_name,
    modal_image_url,
    progress_image_base_url,
    share_image_url,
    is_active,
    deleted
FROM `dream_image_config`
WHERE `item_name` = 'camera' AND `dream_type` = 1 AND `deleted` = 0;