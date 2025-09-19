-- 为transactions表添加AI相关字段（如果不存在）

-- 检查并添加ai_partner_id字段
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'transactions' 
   AND COLUMN_NAME = 'ai_partner_id') > 0,
  'SELECT \'ai_partner_id column already exists\' as message',
  'ALTER TABLE `transactions` ADD COLUMN `ai_partner_id` BIGINT DEFAULT NULL COMMENT \'AI伴侣ID\''
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加ai_partner_name字段
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'transactions' 
   AND COLUMN_NAME = 'ai_partner_name') > 0,
  'SELECT \'ai_partner_name column already exists\' as message',
  'ALTER TABLE `transactions` ADD COLUMN `ai_partner_name` VARCHAR(50) DEFAULT NULL COMMENT \'AI伴侣名称\''
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加ai_partner_avatar字段
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'transactions' 
   AND COLUMN_NAME = 'ai_partner_avatar') > 0,
  'SELECT \'ai_partner_avatar column already exists\' as message',
  'ALTER TABLE `transactions` ADD COLUMN `ai_partner_avatar` VARCHAR(500) DEFAULT NULL COMMENT \'AI伴侣头像URL\''
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加ai_message字段
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'transactions' 
   AND COLUMN_NAME = 'ai_message') > 0,
  'SELECT \'ai_message column already exists\' as message',
  'ALTER TABLE `transactions` ADD COLUMN `ai_message` TEXT DEFAULT NULL COMMENT \'AI消息内容\''
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加voice_url字段
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'transactions' 
   AND COLUMN_NAME = 'voice_url') > 0,
  'SELECT \'voice_url column already exists\' as message',
  'ALTER TABLE `transactions` ADD COLUMN `voice_url` VARCHAR(500) DEFAULT NULL COMMENT \'语音URL\''
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加voice_duration字段
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'transactions' 
   AND COLUMN_NAME = 'voice_duration') > 0,
  'SELECT \'voice_duration column already exists\' as message',
  'ALTER TABLE `transactions` ADD COLUMN `voice_duration` VARCHAR(10) DEFAULT NULL COMMENT \'语音时长\''
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'transactions' 
   AND INDEX_NAME = 'idx_ai_partner_id') > 0,
  'SELECT \'idx_ai_partner_id index already exists\' as message',
  'ALTER TABLE `transactions` ADD KEY `idx_ai_partner_id` (`ai_partner_id`)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
