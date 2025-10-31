-- Add camera configuration to dream_image_config table
-- This fixes the issue where camera wallet shows car images

-- Check if camera configuration already exists
SELECT * FROM `dream_image_config` WHERE `item_name` = 'camera' AND `dream_type` = 1;

-- Insert camera configuration if it doesn't exist
INSERT IGNORE INTO `dream_image_config` (
    `item_name`,
    `item_display_name`,
    `dream_type`,
    `modal_image_url`,
    `progress_image_base_url`,
    `share_image_url`,
    `is_active`,
    `sort_order`,
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
    50, -- 排序
    NOW(),
    NOW(),
    0 -- 未删除
);

-- Update existing camera configuration if it exists
UPDATE `dream_image_config`
SET
    `modal_image_url` = '/static/icon/sel-camera.png',
    `progress_image_base_url` = '/static/icon/progress/progress-camera-{progress}.png',
    `share_image_url` = '/static/icon/share/share-camera.png',
    `update_time` = NOW()
WHERE `item_name` = 'camera' AND `dream_type` = 1 AND `deleted` = 0;

-- Verify the camera configuration
SELECT
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

-- Also check camera in dream_items table
SELECT
    id,
    name,
    display_name,
    modal_image_url,
    option_config,
    is_active,
    deleted
FROM `dream_items`
WHERE `name` = 'camera' AND `category` = 1 AND `deleted` = 0;