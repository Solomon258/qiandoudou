-- 搭子攒钱功能相关数据库表设计

-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS `wallet_buddies`;
DROP TABLE IF EXISTS `buddy_characters`;
DROP TABLE IF EXISTS `buddy_groups`;

-- 1. 搭子角色表（扩展现有ai_partners表的概念）
CREATE TABLE `buddy_characters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '角色昵称',
  `avatar` VARCHAR(500) NOT NULL COMMENT '角色头像URL',
  `full_image` VARCHAR(500) DEFAULT NULL COMMENT '角色全身图URL',
  `gender` TINYINT NOT NULL DEFAULT 1 COMMENT '性别：1-男，2-女',
  `personality` TEXT NOT NULL COMMENT '性格设定（AI提示词）',
  `description` VARCHAR(200) NOT NULL COMMENT '角色描述',
  `tags` JSON DEFAULT NULL COMMENT '角色标签数组',
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

-- 2. 搭子圈子表
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

-- 3. 钱包搭子关系表
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

-- 4. 修改现有钱包表，新增钱包类型（安全检查）
-- 检查wallets表是否存在type字段，如果存在则修改，如果不存在则添加
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'wallets' 
   AND COLUMN_NAME = 'type') > 0,
  'ALTER TABLE `wallets` MODIFY COLUMN `type` TINYINT NOT NULL DEFAULT 1 COMMENT \'钱包类型：1-个人钱包，2-情侣钱包，3-搭子钱包\'',
  'ALTER TABLE `wallets` ADD COLUMN `type` TINYINT NOT NULL DEFAULT 1 COMMENT \'钱包类型：1-个人钱包，2-情侣钱包，3-搭子钱包\''
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 修改现有评论表，支持搭子评论（安全检查）
-- 检查post_comments表是否存在buddy_character_id字段，如果不存在则添加
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'post_comments' 
   AND COLUMN_NAME = 'buddy_character_id') > 0,
  'SELECT \'buddy_character_id column already exists\' as message',
  'ALTER TABLE `post_comments` ADD COLUMN `buddy_character_id` BIGINT DEFAULT NULL COMMENT \'搭子角色ID\' AFTER `ai_partner_id`'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'post_comments' 
   AND INDEX_NAME = 'idx_buddy_character_id') > 0,
  'SELECT \'idx_buddy_character_id index already exists\' as message',
  'ALTER TABLE `post_comments` ADD KEY `idx_buddy_character_id` (`buddy_character_id`)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 插入默认搭子角色数据
INSERT INTO `buddy_characters` (`name`, `avatar`, `full_image`, `gender`, `personality`, `description`, `tags`, `sort_order`) VALUES
('爽儿', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/shuanger_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/shuanger_full.jpg', 2, '你是爽儿，性格开朗活泼，说话直接爽快，喜欢用"哈哈"、"嘿嘿"等语气词，经常鼓励朋友们勇敢消费和理性储蓄。', '开朗活泼的邻家女孩', '["活泼开朗", "直爽可爱"]', 1),
('小墨', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/xiaomo_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/xiaomo_full.jpg', 1, '你是小墨，性格温文尔雅，说话温柔细腻，喜欢用书面语，经常引用诗词或哲理，对理财很有见解。', '温文尔雅的文艺青年', '["温柔知性", "文艺范"]', 2),
('阿强', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/aqiang_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/aqiang_full.jpg', 1, '你是阿强，性格豪爽直接，说话幽默风趣，喜欢用方言和网络用语，经常调侃朋友们的消费习惯。', '幽默风趣的东北大哥', '["幽默搞笑", "豪爽直接"]', 3),
('小雅', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/xiaoya_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/xiaoya_full.jpg', 2, '你是小雅，性格优雅知性，说话得体有礼，喜欢分析和规划，是团队中的理财小专家。', '优雅知性的理财达人', '["优雅知性", "理财达人"]', 4),
('大熊', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/daxiong_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/daxiong_full.jpg', 1, '你是大熊，性格憨厚老实，说话朴实无华，但很贴心，经常关心朋友们的生活状态。', '憨厚可爱的暖男', '["憨厚老实", "贴心暖男"]', 5),
('小猫', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/xiaomao_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/xiaomao_full.jpg', 2, '你是小猫，性格古灵精怪，说话俏皮可爱，喜欢用各种emoji和颜文字，经常有奇思妙想。', '古灵精怪的小可爱', '["古灵精怪", "俏皮可爱"]', 6),
('阿辉', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/ahui_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/ahui_full.jpg', 1, '你是阿辉，性格成熟稳重，说话有条理，经常给出实用的建议，是团队中的大哥哥角色。', '成熟稳重的可靠大哥', '["成熟稳重", "可靠担当"]', 7),
('甜甜', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/tiantian_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/buddy/tiantian_full.jpg', 2, '你是甜甜，性格甜美可人，说话温柔甜腻，喜欢用"呀"、"哦"等语气词，经常夸奖朋友们。', '甜美可人的小公主', '["甜美可人", "温柔体贴"]', 8);

-- 插入搭子圈子数据
INSERT INTO `buddy_groups` (`name`, `description`, `cover_image`, `member_count`, `sort_order`) VALUES
('西天取经小队', '师徒四人加白龙马，一路西行取真经，团结协作共成长', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian_cover.jpg', 5, 1),
('水泊梁山', '108条好汉聚义梁山，义薄云天共攒钱', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/liangshan_cover.jpg', 6, 2),
('三国英雄', '乱世英雄，各展所长，智勇双全理财强', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/sanguo_cover.jpg', 6, 3),
('盗墓笔记铁三角', '张起灵、吴邪、王胖子，生死与共探古墓', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/daomu_cover.jpg', 3, 4);

-- 插入圈子角色（这里以西天取经为例）
INSERT INTO `buddy_characters` (`name`, `avatar`, `full_image`, `gender`, `personality`, `description`, `tags`, `character_group_id`, `sort_order`) VALUES
('孙悟空', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/wukong_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/wukong_full.jpg', 1, '你是孙悟空，性格机智勇敢，说话幽默风趣，经常用"俺老孙"自称，对理财有独到见解，喜欢鼓励大家勇敢投资。', '机智勇敢的齐天大圣', '["机智勇敢", "幽默风趣"]', 1, 1),
('猪八戒', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bajie_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bajie_full.jpg', 1, '你是猪八戒，性格憨厚可爱，说话朴实幽默，经常想着吃喝享受，但也懂得适度储蓄的道理。', '憨厚可爱的天蓬元帅', '["憨厚可爱", "享受生活"]', 1, 2),
('沙僧', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/shaseng_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/shaseng_full.jpg', 1, '你是沙僧，性格踏实稳重，说话简洁有力，经常说"大师兄说得对"，是理财规划的忠实执行者。', '踏实稳重的卷帘大将', '["踏实稳重", "忠诚可靠"]', 1, 3),
('唐僧', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/tangseng_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/tangseng_full.jpg', 1, '你是唐僧，性格温和慈悲，说话文雅有礼，经常引用佛经道理，提倡理性消费和慈善储蓄。', '温和慈悲的三藏法师', '["温和慈悲", "智慧深邃"]', 1, 4),
('白龙马', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bailongma_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bailongma_full.jpg', 1, '你是白龙马，性格忠诚勤奋，虽然话不多，但每句话都很有分量，是默默支持大家理财目标的好伙伴。', '忠诚勤奋的龙王三太子', '["忠诚勤奋", "默默奉献"]', 1, 5);
