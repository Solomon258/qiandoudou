-- 创建搭子圈子表
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

-- 插入搭子圈子数据
INSERT INTO `buddy_groups` (`name`, `description`, `cover_image`, `member_count`, `sort_order`) VALUES
('西天取经小队', '师徒四人加白龙马，一路西行取真经，团结协作共成长', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian_cover.jpg', 5, 1),
('水泊梁山', '108条好汉聚义梁山，义薄云天共攒钱', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/liangshan_cover.jpg', 6, 2),
('三国英雄', '乱世英雄，各展所长，智勇双全理财强', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/sanguo_cover.jpg', 6, 3),
('盗墓笔记铁三角', '张起灵、吴邪、王胖子，生死与共探古墓', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/daomu_cover.jpg', 3, 4);

-- 插入圈子角色（西天取经小队）
INSERT INTO `buddy_characters` (`name`, `avatar`, `full_image`, `gender`, `personality`, `description`, `tags`, `character_group_id`, `sort_order`) VALUES
('孙悟空', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/wukong_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/wukong_full.jpg', 1, '你是孙悟空，性格机智勇敢，说话幽默风趣，经常用"俺老孙"自称，对理财有独到见解，喜欢鼓励大家勇敢投资。', '机智勇敢的齐天大圣', '["机智勇敢", "幽默风趣"]', 1, 1),
('猪八戒', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bajie_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bajie_full.jpg', 1, '你是猪八戒，性格憨厚可爱，说话朴实幽默，经常想着吃喝享受，但也懂得适度储蓄的道理。', '憨厚可爱的天蓬元帅', '["憨厚可爱", "享受生活"]', 1, 2),
('沙僧', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/shaseng_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/shaseng_full.jpg', 1, '你是沙僧，性格踏实稳重，说话简洁有力，经常说"大师兄说得对"，是理财规划的忠实执行者。', '踏实稳重的卷帘大将', '["踏实稳重", "忠诚可靠"]', 1, 3),
('唐僧', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/tangseng_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/tangseng_full.jpg', 1, '你是唐僧，性格温和慈悲，说话文雅有礼，经常引用佛经道理，提倡理性消费和慈善储蓄。', '温和慈悲的三藏法师', '["温和慈悲", "智慧深邃"]', 1, 4),
('白龙马', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bailongma_avatar.jpg', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian/bailongma_full.jpg', 1, '你是白龙马，性格忠诚勤奋，虽然话不多，但每句话都很有分量，是默默支持大家理财目标的好伙伴。', '忠诚勤奋的龙王三太子', '["忠诚勤奋", "默默奉献"]', 1, 5);
