-- 搭子攒钱功能相关数据库表

-- 1. 搭子圈子表
CREATE TABLE IF NOT EXISTS `buddy_circles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '圈子ID',
    `name` VARCHAR(100) NOT NULL COMMENT '圈子名称',
    `description` TEXT COMMENT '圈子描述',
    `cover_image` VARCHAR(255) COMMENT '圈子封面图',
    `background_image` VARCHAR(255) COMMENT '圈子背景图',
    `circle_type` TINYINT DEFAULT 1 COMMENT '圈子类型：1-经典IP, 2-原创',
    `member_count` INT DEFAULT 0 COMMENT '圈子成员数量',
    `sort_order` INT DEFAULT 0 COMMENT '排序权重',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (`id`),
    KEY `idx_circle_type` (`circle_type`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搭子圈子表';

-- 2. 搭子角色表
CREATE TABLE IF NOT EXISTS `buddy_characters` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `circle_id` BIGINT COMMENT '所属圈子ID，NULL表示通用角色',
    `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `nickname` VARCHAR(50) COMMENT '角色昵称',
    `avatar` VARCHAR(255) COMMENT '角色头像',
    `full_image` VARCHAR(255) COMMENT '角色全身图',
    `intro_gif` VARCHAR(255) COMMENT '角色介绍动图',
    `personality` TEXT COMMENT '性格设定JSON',
    `voice_type` VARCHAR(50) COMMENT '语音类型',
    `intro_voice` VARCHAR(255) COMMENT '介绍语音URL',
    `intro_text` VARCHAR(200) COMMENT '介绍文案',
    `tags` JSON COMMENT '角色标签',
    `comment_templates` JSON COMMENT '评论模板JSON',
    `sort_order` INT DEFAULT 0 COMMENT '排序权重',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (`id`),
    KEY `idx_circle_id` (`circle_id`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_sort_order` (`sort_order`),
    FOREIGN KEY (`circle_id`) REFERENCES `buddy_circles`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搭子角色表';

-- 3. 钱包搭子关联表
CREATE TABLE IF NOT EXISTS `wallet_buddies` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `wallet_id` BIGINT NOT NULL COMMENT '钱包ID',
    `character_id` BIGINT NOT NULL COMMENT '搭子角色ID',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否活跃：0-否，1-是',
    `interaction_count` INT DEFAULT 0 COMMENT '互动次数',
    `last_interaction_time` DATETIME COMMENT '最后互动时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wallet_character` (`wallet_id`, `character_id`),
    KEY `idx_wallet_id` (`wallet_id`),
    KEY `idx_character_id` (`character_id`),
    KEY `idx_is_active` (`is_active`),
    FOREIGN KEY (`wallet_id`) REFERENCES `wallets`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`character_id`) REFERENCES `buddy_characters`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包搭子关联表';

-- 4. 修改钱包表，添加搭子圈子ID字段
ALTER TABLE `wallets` 
ADD COLUMN `buddy_circle_id` BIGINT COMMENT '搭子圈子ID' AFTER `ai_partner_id`,
ADD KEY `idx_buddy_circle_id` (`buddy_circle_id`);

-- 5. 修改钱包表的类型注释
ALTER TABLE `wallets` 
MODIFY COLUMN `type` INT COMMENT '钱包类型：1-个人钱包，2-情侣钱包，3-搭子钱包';

-- 6. 搭子互动记录表（用于控制评论频率和统计）
CREATE TABLE IF NOT EXISTS `buddy_interactions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '互动ID',
    `wallet_id` BIGINT NOT NULL COMMENT '钱包ID',
    `character_id` BIGINT NOT NULL COMMENT '搭子角色ID',
    `transaction_id` BIGINT NOT NULL COMMENT '交易ID',
    `interaction_type` TINYINT NOT NULL COMMENT '互动类型：1-评论，2-点赞，3-语音',
    `content` TEXT COMMENT '互动内容',
    `voice_url` VARCHAR(255) COMMENT '语音URL',
    `voice_duration` VARCHAR(20) COMMENT '语音时长',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (`id`),
    KEY `idx_wallet_id` (`wallet_id`),
    KEY `idx_character_id` (`character_id`),
    KEY `idx_transaction_id` (`transaction_id`),
    KEY `idx_interaction_type` (`interaction_type`),
    KEY `idx_create_time` (`create_time`),
    FOREIGN KEY (`wallet_id`) REFERENCES `wallets`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`character_id`) REFERENCES `buddy_characters`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`transaction_id`) REFERENCES `transactions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搭子互动记录表';

-- 插入默认圈子数据
INSERT INTO `buddy_circles` (`id`, `name`, `description`, `cover_image`, `background_image`, `circle_type`, `member_count`, `sort_order`) VALUES
(1, '西天取经小队', '师徒四人+白龙马+女儿国国王+奔波儿灞+灞波儿奔，一起为取经大业攒钱！', '/circles/xiyouji_cover.png', '/circles/xiyouji_bg.png', 1, 8, 1),
(2, '水浒好汉', '梁山108将，义薄云天一起攒钱！', '/circles/shuihu_cover.png', '/circles/shuihu_bg.png', 1, 4, 2),
(3, '三国英雄', '桃园三结义+曹操+吕布+小乔，英雄美人一起攒钱打天下！', '/circles/sanguo_cover.png', '/circles/sanguo_bg.png', 1, 6, 3),
(4, '盗墓笔记铁三角', '吴邪、张起灵、王胖子一起攒钱探险！', '/circles/daomu_cover.png', '/circles/daomu_bg.png', 1, 3, 4);

-- 插入默认通用角色数据
INSERT INTO `buddy_characters` (`id`, `circle_id`, `name`, `nickname`, `avatar`, `full_image`, `personality`, `voice_type`, `intro_text`, `tags`, `sort_order`) VALUES
(1, NULL, '小财迷', '小财迷', '/characters/xiaocaimi_avatar.png', '/characters/xiaocaimi_full.png', '{"traits": ["理财敏感", "精打细算", "督促攒钱"], "comment_style": "理性分析"}', 'sweet_female', '我是小财迷，专门帮你管好每一分钱！', '["理财达人", "精打细算"]', 1),
(2, NULL, '搞笑女', '搞笑女', '/characters/gaoxiaonv_avatar.png', '/characters/gaoxiaonv_full.png', '{"traits": ["幽默风趣", "调节压力", "轻松理财"], "comment_style": "搞笑段子"}', 'funny_female', '姐妹们！攒钱也要开开心心的，我来给你们讲段子！', '["幽默搞笑", "段子手"]', 2),
(3, NULL, '佛系少女', '佛系少女', '/characters/foxishaonv_avatar.png', '/characters/foxishaonv_full.png', '{"traits": ["淡定", "不急不躁", "佛系态度"], "comment_style": "佛系淡定"}', 'calm_female', '攒钱这事儿，顺其自然就好～', '["淡定", "佛系"]', 3),
(4, NULL, '搞笑王', '搞笑王', '/characters/gaoxiaowang_avatar.png', '/characters/gaoxiaowang_full.png', '{"traits": ["幽默风趣", "调节气氛", "充满欢乐"], "comment_style": "幽默搞笑"}', 'funny_male', '攒钱也要开开心心的嘛，哈哈哈！', '["幽默", "活跃"]', 4),
(5, NULL, '知心姐姐', '知心姐姐', '/characters/zhixinjiejie_avatar.png', '/characters/zhixinjiejie_full.png', '{"traits": ["温柔体贴", "善解人意", "关心情绪"], "comment_style": "温柔关怀"}', 'gentle_female', '有什么烦恼都可以跟姐姐说哦～', '["温柔", "贴心"]', 5),
(6, NULL, '学霸君', '学霸君', '/characters/xuebajun_avatar.png', '/characters/xuebajun_full.png', '{"traits": ["理性分析", "数据导向", "科学方法"], "comment_style": "理性分析"}', 'intellectual_male', '让我们用数据分析来优化攒钱策略！', '["理性", "分析"]', 6),
(7, NULL, '萌宠酱', '萌宠酱', '/characters/mengchongjiang_avatar.png', '/characters/mengchongjiang_full.png', '{"traits": ["可爱萌系", "萌萌鼓励"], "comment_style": "可爱萌系"}', 'cute_female', '主人，我们一起攒小钱钱吧～喵！', '["可爱", "萌系"]', 7),
(8, NULL, '黄段子老司机', '老司机', '/characters/huangduanzisiji_avatar.png', '/characters/huangduanzisiji_full.png', '{"traits": ["经验丰富", "内涵段子", "幽默智慧"], "comment_style": "内涵幽默"}', 'humorous_male', '嘿嘿，老司机带你们攒钱不翻车，顺便讲点内涵段子！', '["老司机", "内涵段子"]', 8);

-- 插入西游记角色数据
INSERT INTO `buddy_characters` (`id`, `circle_id`, `name`, `nickname`, `avatar`, `full_image`, `personality`, `voice_type`, `intro_text`, `tags`, `sort_order`) VALUES
(101, 1, '孙悟空', '猴哥', '/characters/wukong_avatar.png', '/characters/wukong_full.png', '{"traits": ["机智勇敢", "火眼金睛", "识破陷阱"], "comment_style": "机智幽默"}', 'wukong_male', '俺老孙来也！一起攒钱取真经！', '["机智", "勇敢", "火眼金睛"]', 1),
(102, 1, '猪八戒', '八戒', '/characters/bajie_avatar.png', '/characters/bajie_full.png', '{"traits": ["憨厚善良", "贪吃好色", "容易冲动"], "comment_style": "憨厚可爱"}', 'bajie_male', '师父，俺也要攒钱娶媳妇！', '["憨厚", "可爱", "吃货"]', 2),
(103, 1, '沙僧', '沙师弟', '/characters/shaseng_avatar.png', '/characters/shaseng_full.png', '{"traits": ["勤劳踏实", "默默攒钱", "不张扬"], "comment_style": "踏实朴素"}', 'shaseng_male', '师父，师兄，我们一起攒钱吧。', '["踏实", "勤劳", "默默无闻"]', 3),
(104, 1, '唐僧', '师父', '/characters/tangseng_avatar.png', '/characters/tangseng_full.png', '{"traits": ["慈悲为怀", "理性消费", "修行态度"], "comment_style": "慈悲理性"}', 'tangseng_male', '阿弥陀佛，攒钱也是修行啊。', '["慈悲", "理性", "修行"]', 4),
(105, 1, '白龙马', '小白龙', '/characters/bailongma_avatar.png', '/characters/bailongma_full.png', '{"traits": ["忠诚可靠", "默默支持", "任劳任怨"], "comment_style": "忠诚支持"}', 'bailongma_male', '嘶～我也要为取经大业出力！', '["忠诚", "可靠", "默默支持"]', 5),
(106, 1, '女儿国国王', '国王陛下', '/characters/nverguo_avatar.png', '/characters/nverguo_full.png', '{"traits": ["温柔贤淑", "内心坚强", "女性视角"], "comment_style": "温柔关怀"}', 'elegant_female', '御弟哥哥，奴家也要和你们一起攒钱呢～', '["温柔", "贤淑", "专情"]', 6),
(107, 1, '奔波儿灞', '奔波儿灞', '/characters/benboerba_avatar.png', '/characters/benboerba_full.png', '{"traits": ["机灵活泼", "总是忙碌", "催促进度"], "comment_style": "急性子催促"}', 'energetic_male', '哎呀，时间就是金钱，大家快点攒钱啊！', '["机灵", "忙碌", "催促"]', 7),
(108, 1, '灞波儿奔', '灞波儿奔', '/characters/baboerben_avatar.png', '/characters/baboerben_full.png', '{"traits": ["好搭档", "同样机灵", "配合催促"], "comment_style": "配合呼应"}', 'lively_male', '对对对，奔波儿灞说得对，攒钱要抓紧！', '["配合", "机灵", "进度控"]', 8);

-- 插入三国角色数据（新增角色）
INSERT INTO `buddy_characters` (`id`, `circle_id`, `name`, `nickname`, `avatar`, `full_image`, `personality`, `voice_type`, `intro_text`, `tags`, `sort_order`) VALUES
(301, 3, '刘备', '刘大哥', '/characters/liubei_avatar.png', '/characters/liubei_full.png', '{"traits": ["仁德之君", "关心他人", "领导者"], "comment_style": "仁德关怀"}', 'liubei_male', '兄弟们，我们一起攒钱复兴汉室！', '["仁德", "关怀", "领导者"]', 1),
(302, 3, '关羽', '关二哥', '/characters/guanyu_avatar.png', '/characters/guanyu_full.png', '{"traits": ["忠义无双", "讲信义", "威武霸气"], "comment_style": "忠义正气"}', 'guanyu_male', '攒钱如做人，要讲信义！', '["忠义", "信用", "威武"]', 2),
(303, 3, '张飞', '张三弟', '/characters/zhangfei_avatar.png', '/characters/zhangfei_full.png', '{"traits": ["性格直爽", "简单粗暴", "有效直接"], "comment_style": "直爽豪迈"}', 'zhangfei_male', '俺老张攒钱，就是这么直接！', '["直爽", "简单", "有力"]', 3),
(304, 3, '曹操', '曹丞相', '/characters/caocao_avatar.png', '/characters/caocao_full.png', '{"traits": ["雄才大略", "善于谋划", "长远战略"], "comment_style": "霸气谋略"}', 'domineering_male', '宁可我负天下人，不可天下人负我的钱包！', '["雄才大略", "谋略", "霸气"]', 4),
(305, 3, '吕布', '温侯', '/characters/lvbu_avatar.png', '/characters/lvbu_full.png', '{"traits": ["武艺超群", "性格冲动", "容易冲动消费"], "comment_style": "冲动直接"}', 'impulsive_male', '人中吕布，马中赤兔！攒钱我也要第一！', '["武艺超群", "冲动", "第一"]', 5),
(306, 3, '小乔', '小乔妹妹', '/characters/xiaoqiao_avatar.png', '/characters/xiaoqiao_full.png', '{"traits": ["温婉可人", "温柔鼓励", "情绪管理"], "comment_style": "温婉体贴"}', 'gentle_sweet_female', '公瑾哥哥说了，攒钱也要保持好心情呢～', '["温婉", "体贴", "情绪管理"]', 6);
