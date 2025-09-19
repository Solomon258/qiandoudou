-- 为搭子角色插入语音映射配置
-- 音色字段使用""占位符，需要手动填充实际音色值

-- 自由选择搭子角色的语音映射
INSERT INTO `character_voice_mapping` (`character_name`, `original_tts_character_name`, `original_tts_voice_path`, `volcengine_character_name`, `volcengine_voice_type`, `gender`, `voice_style`, `description`) VALUES
('爽儿', '', '', '', '', 2, '活泼开朗', '开朗活泼的邻家女孩音色'),
('小墨', '', '', '', '', 1, '温柔知性', '温文尔雅的文艺青年音色'),
('阿强', '', '', '', '', 1, '幽默豪爽', '幽默风趣的东北大哥音色'),
('小雅', '', '', '', '', 2, '优雅知性', '优雅知性的理财达人音色'),
('大熊', '', '', '', '', 1, '憨厚老实', '憨厚可爱的暖男音色'),
('小猫', '', '', '', '', 2, '古灵精怪', '古灵精怪的小可爱音色'),
('阿辉', '', '', '', '', 1, '成熟稳重', '成熟稳重的可靠大哥音色'),
('甜甜', '', '', '', '', 2, '甜美可人', '甜美可人的小公主音色');

-- 圈子角色的语音映射（西天取经小队）
INSERT INTO `character_voice_mapping` (`character_name`, `original_tts_character_name`, `original_tts_voice_path`, `volcengine_character_name`, `volcengine_voice_type`, `gender`, `voice_style`, `description`) VALUES
('孙悟空', '', '', '', '', 1, '机智勇敢', '机智勇敢的齐天大圣音色'),
('猪八戒', '', '', '', '', 1, '憨厚幽默', '憨厚可爱的天蓬元帅音色'),
('沙僧', '', '', '', '', 1, '踏实稳重', '踏实稳重的卷帘大将音色'),
('唐僧', '', '', '', '', 1, '温和慈悲', '温和慈悲的三藏法师音色'),
('白龙马', '', '', '', '', 1, '忠诚勤奋', '忠诚勤奋的龙王三太子音色');

-- 其他圈子角色的语音映射（根据圈子设定预设）
-- 注意：以下角色需要在对应圈子的角色插入SQL执行后才能使用

-- 水泊梁山圈子角色（预设，需要配合对应的角色数据）
INSERT INTO `character_voice_mapping` (`character_name`, `original_tts_character_name`, `original_tts_voice_path`, `volcengine_character_name`, `volcengine_voice_type`, `gender`, `voice_style`, `description`) VALUES
('宋江', '', '', '', '', 1, '仁义豪迈', '仁义豪迈的及时雨音色'),
('林冲', '', '', '', '', 1, '英勇刚毅', '英勇刚毅的豹子头音色'),
('武松', '', '', '', '', 1, '刚烈豪爽', '刚烈豪爽的行者音色'),
('李逵', '', '', '', '', 1, '粗犷直率', '粗犷直率的黑旋风音色'),
('鲁智深', '', '', '', '', 1, '豪放不羁', '豪放不羁的花和尚音色'),
('吴用', '', '', '', '', 1, '智慧深沉', '智慧深沉的智多星音色');

-- 三国英雄圈子角色（预设，需要配合对应的角色数据）
INSERT INTO `character_voice_mapping` (`character_name`, `original_tts_character_name`, `original_tts_voice_path`, `volcengine_character_name`, `volcengine_voice_type`, `gender`, `voice_style`, `description`) VALUES
('刘备', '', '', '', '', 1, '仁德温和', '仁德温和的昭烈帝音色'),
('关羽', '', '', '', '', 1, '忠义凛然', '忠义凛然的武圣音色'),
('张飞', '', '', '', '', 1, '豪迈粗犷', '豪迈粗犷的翼德音色'),
('诸葛亮', '', '', '', '', 1, '智慧儒雅', '智慧儒雅的卧龙音色'),
('赵云', '', '', '', '', 1, '英勇潇洒', '英勇潇洒的子龙音色'),
('曹操', '', '', '', '', 1, '雄才大略', '雄才大略的魏武帝音色');

-- 盗墓笔记铁三角圈子角色（预设，需要配合对应的角色数据）
INSERT INTO `character_voice_mapping` (`character_name`, `original_tts_character_name`, `original_tts_voice_path`, `volcengine_character_name`, `volcengine_voice_type`, `gender`, `voice_style`, `description`) VALUES
('张起灵', '', '', '', '', 1, '神秘冷静', '神秘冷静的小哥音色'),
('吴邪', '', '', '', '', 1, '机智幽默', '机智幽默的天真音色'),
('王胖子', '', '', '', '', 1, '幽默豪爽', '幽默豪爽的胖子音色');
