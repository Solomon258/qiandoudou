-- 搭子攒钱功能数据库表创建（简化版）

-- 先禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 按正确顺序删除表（先删除有外键依赖的表）
DROP TABLE IF EXISTS `wallet_buddies`;
DROP TABLE IF EXISTS `buddy_characters`;
DROP TABLE IF EXISTS `buddy_groups`;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 创建搭子角色表
CREATE TABLE `buddy_characters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '角色昵称',
  `avatar` VARCHAR(500) NOT NULL COMMENT '角色头像URL',
  `full_image` VARCHAR(500) DEFAULT NULL COMMENT '角色全身图URL',
  `gender` TINYINT NOT NULL DEFAULT 1 COMMENT '性别：1-男，2-女',
  `personality` TEXT NOT NULL COMMENT '性格设定（AI提示词）',
  `description` VARCHAR(200) NOT NULL COMMENT '角色描述',
  `tags` TEXT DEFAULT NULL COMMENT '角色标签JSON',
  `voice_sample_url` VARCHAR(500) DEFAULT NULL COMMENT '语音样例URL',
  `voice_duration` VARCHAR(10) DEFAULT NULL COMMENT '语音时长',
  `character_group_id` BIGINT DEFAULT NULL COMMENT '所属角色组ID（圈子）',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否启用：0-否，1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_character_group` (`character_group_id`),
  KEY `idx_is_active` (`is_active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子角色表';

-- 2. 创建搭子圈子表
CREATE TABLE `buddy_groups` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '圈子ID',
  `name` VARCHAR(100) NOT NULL COMMENT '圈子名称',
  `description` VARCHAR(500) NOT NULL COMMENT '圈子描述',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '圈子封面图',
  `background_image` VARCHAR(500) DEFAULT NULL COMMENT '圈子背景图',
  `member_count` INT DEFAULT 0 COMMENT '成员数量',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否启用：0-否，1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_is_active` (`is_active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子圈子表';

-- 3. 创建钱包搭子关系表
CREATE TABLE `wallet_buddies` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `wallet_id` BIGINT NOT NULL COMMENT '钱包ID',
  `buddy_character_id` BIGINT NOT NULL COMMENT '搭子角色ID',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否活跃：0-否，1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_buddy` (`wallet_id`, `buddy_character_id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_buddy_character_id` (`buddy_character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包搭子关系表';
