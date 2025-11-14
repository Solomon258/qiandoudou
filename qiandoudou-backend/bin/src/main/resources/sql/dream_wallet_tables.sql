-- 梦想攒钱功能数据库表设计
-- 创建日期：2025-01-22
-- 功能：支持购物和旅行两种梦想攒钱类型

-- 先禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS `dream_rewards`;
DROP TABLE IF EXISTS `dream_share_records`;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 扩展钱包表，添加梦想攒钱相关字段（如果字段不存在才添加）
-- 检查并添加 dream_type 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_type') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_type` TINYINT DEFAULT NULL COMMENT ''梦想类型：1-购物，2-旅行''',
    'SELECT ''dream_type column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 dream_target_amount 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_target_amount') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_target_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''梦想目标金额''',
    'SELECT ''dream_target_amount column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 dream_item_name 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_item_name') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_item_name` VARCHAR(100) DEFAULT NULL COMMENT ''梦想物品名称''',
    'SELECT ''dream_item_name column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 dream_item_image 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_item_image') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_item_image` VARCHAR(500) DEFAULT NULL COMMENT ''梦想物品图片URL''',
    'SELECT ''dream_item_image column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 dream_destination 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_destination') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_destination` VARCHAR(100) DEFAULT NULL COMMENT ''旅行目的地''',
    'SELECT ''dream_destination column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 dream_days 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_days') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_days` INT DEFAULT NULL COMMENT ''旅行天数''',
    'SELECT ''dream_days column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 dream_itinerary 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_itinerary') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_itinerary` TEXT DEFAULT NULL COMMENT ''旅行行程（AI生成）''',
    'SELECT ''dream_itinerary column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 dream_progress 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_progress') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_progress` INT DEFAULT 0 COMMENT ''梦想进度百分比（0-100）''',
    'SELECT ''dream_progress column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 dream_last_milestone 字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'dream_last_milestone') = 0,
    'ALTER TABLE `wallets` ADD COLUMN `dream_last_milestone` INT DEFAULT 0 COMMENT ''上次达到的里程碑（0,20,40,60,80,100）''',
    'SELECT ''dream_last_milestone column already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 梦想奖励表（如果不存在才创建）
CREATE TABLE IF NOT EXISTS `dream_rewards` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '奖励ID',
  `wallet_id` BIGINT NOT NULL COMMENT '钱包ID',
  `reward_type` TINYINT NOT NULL COMMENT '奖励类型：1-优惠券，2-任务徽章',
  `title` VARCHAR(100) NOT NULL COMMENT '奖励标题',
  `description` TEXT COMMENT '奖励描述',
  `unlock_progress` INT NOT NULL COMMENT '解锁进度百分比（20,40,60,80,100）',
  `unlock_condition` VARCHAR(200) DEFAULT NULL COMMENT '解锁条件描述',
  `is_unlocked` TINYINT DEFAULT 0 COMMENT '是否已解锁：0-否，1-是',
  `unlock_time` DATETIME DEFAULT NULL COMMENT '解锁时间',
  `reward_image` VARCHAR(500) DEFAULT NULL COMMENT '奖励图标URL',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_reward_type` (`reward_type`),
  KEY `idx_is_unlocked` (`is_unlocked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='梦想奖励表';

-- 3. 梦想分享记录表（如果不存在才创建）
CREATE TABLE IF NOT EXISTS `dream_share_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分享记录ID',
  `wallet_id` BIGINT NOT NULL COMMENT '钱包ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `share_type` TINYINT NOT NULL COMMENT '分享类型：1-进度分享，2-完成分享',
  `share_progress` INT NOT NULL COMMENT '分享时的进度百分比',
  `share_image_url` VARCHAR(500) DEFAULT NULL COMMENT '分享图片URL',
  `share_text` TEXT DEFAULT NULL COMMENT '分享文案',
  `share_platform` VARCHAR(50) DEFAULT 'wechat' COMMENT '分享平台',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_share_type` (`share_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='梦想分享记录表';

-- 4. 插入默认的梦想奖励数据（如果不存在才插入）
INSERT IGNORE INTO `dream_rewards` (`wallet_id`, `reward_type`, `title`, `description`, `unlock_progress`, `unlock_condition`) VALUES
-- 通用进度奖励（wallet_id为0表示模板，创建钱包时复制）
(0, 1, '新手攒钱券', '恭喜你开启梦想攒钱之旅！', 20, '达到20%进度'),
(0, 1, '坚持攒钱券', '坚持就是胜利，继续加油！', 40, '达到40%进度'),
(0, 1, '半程达成券', '已经完成一半啦，离梦想更近了！', 60, '达到60%进度'),
(0, 1, '冲刺加油券', '最后冲刺阶段，胜利在望！', 80, '达到80%进度'),
(0, 1, '梦想成真券', '恭喜你实现梦想，太棒了！', 100, '达到100%进度');

-- 5. 创建梦想商品配置表（用于存储可选择的商品信息）
CREATE TABLE IF NOT EXISTS `dream_items` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `category` TINYINT NOT NULL COMMENT '商品分类：1-购物商品，2-旅行目的地',
  `name` VARCHAR(100) NOT NULL COMMENT '商品/目的地名称',
  `display_name` VARCHAR(100) NOT NULL COMMENT '显示名称',
  `image_url` VARCHAR(500) NOT NULL COMMENT '商品图片URL',
  `bubble_image_url` VARCHAR(500) DEFAULT NULL COMMENT '气泡图片URL',
  `description` TEXT DEFAULT NULL COMMENT '商品描述',
  `suggested_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '建议金额',
  `tags` JSON DEFAULT NULL COMMENT '标签数组',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否启用：0-否，1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_is_active` (`is_active`, `deleted`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='梦想商品配置表';

-- 6. 插入初始商品数据（如果不存在才插入）
INSERT IGNORE INTO `dream_items` (`category`, `name`, `display_name`, `image_url`, `bubble_image_url`, `description`, `suggested_amount`, `sort_order`) VALUES
-- 购物商品
(1, 'car', '轿车', '/static/icon/goods/轿车@3x.png', '/static/icon/paopao/shopping/轿车@3x.png', '梦想中的轿车', 100000.00, 1),
(1, 'suv', 'SUV', '/static/icon/goods/suv@3x.png', '/static/icon/paopao/shopping/suv@3x.png', '实用的SUV', 150000.00, 2),
(1, 'supercar', '超跑', '/static/icon/goods/超跑@3x.png', '/static/icon/paopao/shopping/超跑@3x.png', '速度与激情的超跑', 500000.00, 3),
(1, 'bag', '包包', '/static/icon/goods/包包@3x.png', '/static/icon/paopao/shopping/包包@3x.png', '心仪的包包', 5000.00, 4),
(1, 'rolex', '劳力士', '/static/icon/goods/劳力士@3x.png', '/static/icon/paopao/shopping/劳力士@3x.png', '奢华的劳力士手表', 50000.00, 5),

-- 旅行目的地
(2, 'beijing', '北京', '/static/icon/destinations/beijing.jpg', '/static/icon/paopao/travel/编组 2.png', '首都北京，历史文化之旅', 5000.00, 1),
(2, 'xian', '西安', '/static/icon/destinations/xian.jpg', '/static/icon/paopao/travel/编组 3.png', '古都西安，兵马俑之旅', 4000.00, 2),
(2, 'urumqi', '乌鲁木齐', '/static/icon/destinations/urumqi.jpg', '/static/icon/paopao/travel/编组 4.png', '新疆乌鲁木齐，大美新疆', 8000.00, 3),
(2, 'sydney', '悉尼', '/static/icon/destinations/sydney.jpg', '/static/icon/paopao/travel/编组 5.png', '澳洲悉尼，海港城市', 15000.00, 4),
(2, 'tokyo', '东京', '/static/icon/destinations/tokyo.jpg', '/static/icon/paopao/travel/编组 6.png', '日本东京，现代都市', 12000.00, 5);

-- 7. 创建梦想进度历史表（用于记录进度变化历史）
CREATE TABLE IF NOT EXISTS `dream_progress_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
  `wallet_id` BIGINT NOT NULL COMMENT '钱包ID',
  `old_progress` INT NOT NULL COMMENT '原进度百分比',
  `new_progress` INT NOT NULL COMMENT '新进度百分比',
  `milestone_reached` INT DEFAULT NULL COMMENT '达到的里程碑（20,40,60,80,100）',
  `trigger_amount` DECIMAL(10,2) NOT NULL COMMENT '触发进度变化的金额',
  `trigger_type` TINYINT NOT NULL COMMENT '触发类型：1-转入，2-转出',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_milestone` (`milestone_reached`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='梦想进度历史表';

-- 8. 创建索引优化查询性能（如果不存在才创建）
-- 检查并创建 idx_dream_type 索引
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND INDEX_NAME = 'idx_dream_type') = 0,
    'ALTER TABLE `wallets` ADD INDEX `idx_dream_type` (`dream_type`)',
    'SELECT ''idx_dream_type index already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并创建 idx_dream_progress 索引
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND INDEX_NAME = 'idx_dream_progress') = 0,
    'ALTER TABLE `wallets` ADD INDEX `idx_dream_progress` (`dream_progress`)',
    'SELECT ''idx_dream_progress index already exists'' as result'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

COMMIT;

-- 执行完成提示
SELECT 'Dream wallet tables created successfully!' as result;
