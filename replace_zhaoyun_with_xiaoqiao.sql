-- =====================================================
-- 将三国英雄圈子中的赵云替换为小乔
-- 执行此脚本前请先备份相关数据
-- =====================================================

-- 1. 首先禁用赵云角色（保留数据但不在选择页面显示）
UPDATE `buddy_characters` 
SET `is_active` = 0, `deleted` = 1 
WHERE `name` = '赵云' AND `character_group_id` = 3;

-- 2. 添加小乔角色数据到三国英雄圈子
INSERT INTO `buddy_characters` (
    `name`, 
    `avatar`, 
    `full_image`, 
    `gender`, 
    `personality`, 
    `description`, 
    `tags`, 
    `character_group_id`, 
    `sort_order`, 
    `is_active`, 
    `deleted`
) VALUES (
    '小乔', 
    'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/sanguo/xiaoqiao_avatar.jpg', 
    'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/sanguo/xiaoqiao_full.jpg', 
    2, -- 女性
    '你是小乔，性格温柔娴静，说话优雅动人，善解人意，是江东第一美女，在理财方面注重细节和稳健规划，善于从生活小事中节约开支。', 
    '温柔娴静的江东美女', 
    '["温柔娴静", "善解人意"]', 
    3, -- 三国英雄圈子
    5, -- 排序位置（原赵云位置）
    1, -- 启用
    0  -- 未删除
);

-- 3. 添加小乔的语音映射配置
INSERT INTO `character_voice_mapping` (
    `character_name`, 
    `original_tts_character_name`, 
    `original_tts_voice_path`, 
    `volcengine_character_name`, 
    `volcengine_voice_type`, 
    `gender`, 
    `voice_style`, 
    `description`
) VALUES (
    '小乔', 
    '', 
    '', 
    '', 
    '', 
    2, -- 女性
    '温柔娴静', 
    '温柔娴静的江东美女音色'
);

-- 4. 禁用赵云的语音映射（保留但标记为已禁用）
UPDATE `character_voice_mapping` 
SET `character_name` = '赵云(已禁用)' 
WHERE `character_name` = '赵云';

-- =====================================================
-- 验证查询：检查修改结果
-- =====================================================

-- 查询三国英雄圈子的所有角色
SELECT 
    bc.id,
    bc.name,
    bc.description,
    bc.gender,
    bc.character_group_id,
    bc.sort_order,
    bc.is_active,
    bc.deleted,
    bg.name as group_name
FROM `buddy_characters` bc
LEFT JOIN `buddy_groups` bg ON bc.character_group_id = bg.id
WHERE bc.character_group_id = 3
ORDER BY bc.sort_order;

-- 查询语音映射配置
SELECT 
    character_name,
    gender,
    voice_style,
    description
FROM `character_voice_mapping`
WHERE character_name IN ('小乔', '赵云', '赵云(已禁用)')
ORDER BY character_name;
