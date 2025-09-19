-- 修改现有表以支持搭子功能

-- 1. 修改钱包表，支持搭子钱包类型（如果type字段不存在则添加）
-- 先检查字段是否存在
SELECT COUNT(*) as type_exists FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'wallets' 
AND COLUMN_NAME = 'type';

-- 如果type字段不存在，请手动执行：
-- ALTER TABLE `wallets` ADD COLUMN `type` TINYINT NOT NULL DEFAULT 1 COMMENT '钱包类型：1-个人钱包，2-情侣钱包，3-搭子钱包';

-- 如果type字段存在，请手动执行：
-- ALTER TABLE `wallets` MODIFY COLUMN `type` TINYINT NOT NULL DEFAULT 1 COMMENT '钱包类型：1-个人钱包，2-情侣钱包，3-搭子钱包';

-- 2. 修改评论表，支持搭子评论（如果buddy_character_id字段不存在则添加）
-- 先检查字段是否存在
SELECT COUNT(*) as buddy_field_exists FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'post_comments' 
AND COLUMN_NAME = 'buddy_character_id';

-- 如果buddy_character_id字段不存在，请手动执行：
-- ALTER TABLE `post_comments` ADD COLUMN `buddy_character_id` BIGINT DEFAULT NULL COMMENT '搭子角色ID' AFTER `ai_partner_id`;
-- ALTER TABLE `post_comments` ADD KEY `idx_buddy_character_id` (`buddy_character_id`);
