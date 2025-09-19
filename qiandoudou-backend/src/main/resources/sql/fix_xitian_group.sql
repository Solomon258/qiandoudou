-- 修复西天取经小队圈子和角色数据
-- 确保西天取经小队在选择圈子页面正常显示

-- 1. 确保西天取经小队圈子存在且状态正确
INSERT IGNORE INTO `buddy_groups` (`id`, `name`, `description`, `cover_image`, `member_count`, `sort_order`, `is_active`, `deleted`) VALUES
(1, '西天取经小队', '师徒四人加白龙马，一路西行取真经，团结协作共成长', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian_cover.jpg', 5, 1, 1, 0);

-- 2. 更新西天取经小队圈子状态（防止被误删除或禁用）
UPDATE `buddy_groups` SET 
    `is_active` = 1, 
    `deleted` = 0,
    `name` = '西天取经小队',
    `description` = '师徒四人加白龙马，一路西行取真经，团结协作共成长',
    `cover_image` = 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian_cover.jpg',
    `member_count` = 5,
    `sort_order` = 1
WHERE `name` = '西天取经小队' OR `id` = 1;

-- 3. 确保西天取经小队角色存在且状态正确
INSERT IGNORE INTO `buddy_characters` (`name`, `avatar`, `full_image`, `gender`, `personality`, `description`, `tags`, `character_group_id`, `sort_order`, `is_active`, `deleted`) VALUES
('孙悟空', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/wukong_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/wukong_full.jpg', 1, '你是孙悟空，性格机智勇敢，说话幽默风趣，经常用"俺老孙"自称，对理财有独到见解，喜欢鼓励大家勇敢投资。', '机智勇敢的齐天大圣', '["机智勇敢", "幽默风趣"]', 1, 1, 1, 0),
('猪八戒', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bajie_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bajie_full.jpg', 1, '你是猪八戒，性格憨厚可爱，说话朴实幽默，经常想着吃喝享受，但也懂得适度储蓄的道理。', '憨厚可爱的天蓬元帅', '["憨厚可爱", "享受生活"]', 1, 2, 1, 0),
('沙僧', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/shaseng_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/shaseng_full.jpg', 1, '你是沙僧，性格踏实稳重，说话简洁有力，经常说"大师兄说得对"，是理财规划的忠实执行者。', '踏实稳重的卷帘大将', '["踏实稳重", "忠诚可靠"]', 1, 3, 1, 0),
('唐僧', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/tangseng_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/tangseng_full.jpg', 1, '你是唐僧，性格温和慈悲，说话文雅有礼，经常引用佛经道理，提倡理性消费和慈善储蓄。', '温和慈悲的三藏法师', '["温和慈悲", "智慧深邃"]', 1, 4, 1, 0),
('白龙马', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bailongma_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bailongma_full.jpg', 1, '你是白龙马，性格忠诚勤奋，虽然话不多，但每句话都很有分量，是默默支持大家理财目标的好伙伴。', '忠诚勤奋的龙王三太子', '["忠诚勤奋", "默默奉献"]', 1, 5, 1, 0);

-- 4. 更新西天取经小队角色状态（防止被误删除或禁用）
UPDATE `buddy_characters` SET 
    `is_active` = 1, 
    `deleted` = 0,
    `character_group_id` = 1
WHERE `name` IN ('孙悟空', '猪八戒', '沙僧', '唐僧', '白龙马');

-- 5. 确保其他圈子也存在且状态正确
INSERT IGNORE INTO `buddy_groups` (`id`, `name`, `description`, `cover_image`, `member_count`, `sort_order`, `is_active`, `deleted`) VALUES
(2, '水泊梁山', '108条好汉聚义梁山，义薄云天共攒钱', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/liangshan_cover.jpg', 6, 2, 1, 0),
(3, '三国英雄', '乱世英雄，各展所长，智勇双全理财强', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/sanguo_cover.jpg', 6, 3, 1, 0),
(4, '盗墓笔记铁三角', '张起灵、吴邪、王胖子，生死与共探古墓', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/daomu_cover.jpg', 3, 4, 1, 0);

-- 6. 更新其他圈子状态
UPDATE `buddy_groups` SET `is_active` = 1, `deleted` = 0 WHERE `name` IN ('水泊梁山', '三国英雄', '盗墓笔记铁三角');

-- 7. 验证查询（可选，用于检查数据是否正确）
-- SELECT * FROM buddy_groups WHERE is_active = 1 AND deleted = 0 ORDER BY sort_order ASC;
-- SELECT * FROM buddy_characters WHERE character_group_id = 1 AND is_active = 1 AND deleted = 0 ORDER BY sort_order ASC;
