-- 插入样例数据

USE qiandoudou;

-- 插入AI伴侣数据
INSERT INTO `ai_partners` (`id`, `name`, `gender`, `avatar`, `personality`, `voice_type`) VALUES
(1, '小雅', 2, '/img/ai_partners/xiaoya.png', '温柔体贴，善解人意，喜欢鼓励用户储蓄，会用甜美的声音和用户交流', 'sweet_female'),
(2, '阿俊', 1, '/img/ai_partners/ajun.png', '阳光帅气，幽默风趣，擅长理财规划，会用磁性的声音给用户建议', 'magnetic_male'),
(3, '小萌', 2, '/img/ai_partners/xiaomeng.png', '可爱活泼，充满活力，喜欢和用户分享生活小窍门', 'cute_female'),
(4, '子轩', 1, '/img/ai_partners/zixuan.png', '成熟稳重，理性分析，专业的理财顾问风格', 'mature_male');

-- 插入用户数据（Demo版本使用明文密码）
INSERT INTO `users` (`id`, `username`, `password`, `phone`, `nickname`, `avatar`, `openid`, `gender`) VALUES
(1, 'demo_user', '123456', '13800138001', '小明', '/img/avatars/user1.png', NULL, 1),
(2, 'demo_user2', '123456', '13800138002', '小红', '/img/avatars/user2.png', NULL, 2),
(3, 'demo_user3', '123456', '13800138003', '小刚', '/img/avatars/user3.png', NULL, 1),
(4, 'test_user', '123456', '13800138004', '测试用户', '/img/avatars/user4.png', NULL, 1);

-- 插入钱包数据
INSERT INTO `wallets` (`id`, `user_id`, `name`, `type`, `balance`, `background_image`, `ai_partner_id`, `is_public`) VALUES
(1, 1, '我的日常钱包', 1, 1580.50, '/img/backgrounds/bg1.jpg', NULL, 1),
(2, 1, '和小雅的甜蜜钱包', 2, 2300.00, '/img/backgrounds/bg2.jpg', 1, 1),
(3, 1, '旅行基金', 1, 5600.80, '/img/backgrounds/bg3.jpg', NULL, 0),
(4, 2, '小红的储蓄罐', 1, 980.20, '/img/backgrounds/bg4.jpg', NULL, 1),
(5, 2, '和阿俊的未来家', 2, 12000.00, '/img/backgrounds/bg5.jpg', 2, 1),
(6, 3, '小刚的投资账户', 1, 8500.00, '/img/backgrounds/bg6.jpg', NULL, 1);

-- 插入剧本数据（模仿《十日终焉》风格）
INSERT INTO `scripts` (`id`, `title`, `description`, `cover_image`, `total_chapters`, `target_amount`, `status`) VALUES
(1, '时间循环的储蓄密码', '在一个神秘的时间循环中，你需要通过明智的储蓄选择来破解时间的谜题。每一个决定都会影响你的未来，也会决定你能否在十日内积累足够的资金逃离循环。', '/img/scripts/script1_cover.jpg', 15, 300.00, 1),
(2, '平行世界的理财师', '你意外发现自己可以在不同的平行世界间穿梭，每个世界都有不同的经济规则。通过储蓄和投资，你需要在各个世界中积累财富，最终找到回家的路。', '/img/scripts/script2_cover.jpg', 12, 250.00, 1),
(3, '未来城市生存指南', '2050年，AI统治的未来城市中，金钱是唯一的生存工具。你需要通过各种储蓄挑战来获得生存资源，每一个选择都关乎生死。', '/img/scripts/script3_cover.jpg', 18, 400.00, 1);

-- 插入剧本章节数据（时间循环的储蓄密码 - 前3章）
INSERT INTO `script_chapters` (`id`, `script_id`, `chapter_number`, `title`, `content`, `image_url`, `choices`, `unlock_amount`) VALUES
(1, 1, 1, '循环的开始', '你在一个陌生的房间中醒来，桌上放着一张纸条："欢迎来到十日循环，只有储蓄足够的金钱才能打破循环。今天是第一天，你有100元启动资金。"窗外的日历显示着同一个日期，你意识到时间被困住了。', '/img/scripts/chapter1_1.jpg', '[{"text": "立即存入银行获得利息", "amount": 5, "next_chapter": 2}, {"text": "购买理财产品", "amount": 8, "next_chapter": 3}, {"text": "先观察一天再决定", "amount": 3, "next_chapter": 4}]', 0.00),
(2, 1, 2, '银行的秘密', '你选择将钱存入银行，银行职员神秘地笑了："明智的选择，但这里的利息规则很特殊。"你发现这个世界的银行每天会根据你的储蓄态度给出不同的利息。职员递给你一份特殊的储蓄计划书。', '/img/scripts/chapter1_2.jpg', '[{"text": "选择保守储蓄计划", "amount": 6, "next_chapter": 5}, {"text": "选择激进投资计划", "amount": 12, "next_chapter": 6}]', 5.00),
(3, 1, 3, '理财产品的陷阱', '你购买了一个看似诱人的理财产品，但销售员的眼神让你感到不安。第二天，你发现这个产品有着复杂的规则，你的钱可能会增加，也可能会减少，这取决于你接下来的选择。', '/img/scripts/chapter1_3.jpg', '[{"text": "继续持有并追加投资", "amount": 15, "next_chapter": 7}, {"text": "立即赎回止损", "amount": 4, "next_chapter": 8}]', 8.00);

-- 插入交易记录数据
INSERT INTO `transactions` (`id`, `wallet_id`, `user_id`, `type`, `amount`, `balance_after`, `description`) VALUES
(1, 1, 1, 1, 100.00, 100.00, '初始资金'),
(2, 1, 1, 1, 50.00, 150.00, '零花钱存入'),
(3, 1, 1, 2, 20.00, 130.00, '午餐支出'),
(4, 1, 1, 3, 5.00, 125.00, '时间循环的储蓄密码-第1章'),
(5, 2, 1, 1, 500.00, 500.00, '和小雅一起的第一笔存款'),
(6, 2, 1, 1, 200.00, 700.00, '小雅鼓励我存的钱'),
(7, 4, 2, 1, 300.00, 300.00, '工资存入'),
(8, 4, 2, 1, 80.00, 380.00, '兼职收入');

-- 插入用户剧本进度数据
INSERT INTO `user_script_progress` (`id`, `user_id`, `wallet_id`, `script_id`, `current_chapter`, `total_paid`, `choices_made`, `status`) VALUES
(1, 1, 1, 1, 2, 5.00, '[{"chapter": 1, "choice": 0}]', 1),
(2, 2, 4, 1, 1, 0.00, '[]', 1);

-- 插入社交动态数据
INSERT INTO `social_posts` (`id`, `user_id`, `wallet_id`, `transaction_id`, `content`, `images`, `like_count`, `comment_count`) VALUES
(1, 1, 1, 2, '今天又存了50块钱！距离我的小目标又近了一步~', '["/img/posts/post1_1.jpg"]', 3, 2),
(2, 1, 2, 6, '小雅今天鼓励我多存点钱，她说这样我们的未来会更美好💕', '["/img/posts/post2_1.jpg", "/img/posts/post2_2.jpg"]', 8, 5),
(3, 2, 4, 8, '兼职赚的钱全部存起来，为了我的梦想努力！', '["/img/posts/post3_1.jpg"]', 2, 1),
(4, 1, 1, 4, '开始了新的剧本攒钱计划，这个时间循环的故事好有趣！', '["/img/posts/post4_1.jpg"]', 5, 3);

-- 插入动态评论数据
INSERT INTO `post_comments` (`id`, `post_id`, `user_id`, `content`, `images`, `voice_url`, `is_ai_comment`, `ai_partner_id`) VALUES
(1, 1, 2, '加油！储蓄是个好习惯！', NULL, NULL, 0, NULL),
(2, 1, 1, '谢谢支持~', NULL, NULL, 0, NULL),
(3, 2, 1, '小雅真的很贴心呢~', NULL, '/voice/ai_comment_1.mp3', 1, 1),
(4, 2, 2, '你们好甜蜜啊！', NULL, NULL, 0, NULL),
(5, 2, 3, '羡慕有AI伴侣的生活', NULL, NULL, 0, NULL),
(6, 4, 2, '我也想试试这个剧本！', NULL, NULL, 0, NULL);

-- 插入动态点赞数据
INSERT INTO `post_likes` (`id`, `post_id`, `user_id`, `is_ai_like`, `ai_partner_id`) VALUES
(1, 1, 2, 0, NULL),
(2, 1, 3, 0, NULL),
(3, 1, 1, 1, 1), -- AI伴侣小雅的点赞
(4, 2, 2, 0, NULL),
(5, 2, 3, 0, NULL),
(6, 2, 1, 1, 1), -- AI伴侣小雅的点赞
(7, 3, 1, 0, NULL),
(8, 3, 3, 0, NULL),
(9, 4, 2, 0, NULL),
(10, 4, 3, 0, NULL);

-- 插入关注数据
INSERT INTO `user_follows` (`id`, `follower_id`, `following_id`) VALUES
(1, 1, 2),
(2, 1, 3),
(3, 2, 1),
(4, 3, 1),
(5, 3, 2);

-- 插入消息通知数据
INSERT INTO `notifications` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `is_read`) VALUES
(1, 1, 1, '小红点赞了你的动态', '小红点赞了你的动态"今天又存了50块钱！距离我的小目标又近了一步~"', 1, 0),
(2, 1, 2, '小红评论了你的动态', '小红评论了你的动态：加油！储蓄是个好习惯！', 1, 0),
(3, 1, 1, '小雅为你点赞', '你的AI伴侣小雅为你的动态点赞，并说："亲爱的，你今天的储蓄表现真棒！"', 2, 1),
(4, 2, 3, '小明关注了你', '小明开始关注你的钱包动态', NULL, 0),
(5, 1, 4, '剧本更新提醒', '你参与的剧本"时间循环的储蓄密码"有新章节可以解锁了！', 1, 0);
