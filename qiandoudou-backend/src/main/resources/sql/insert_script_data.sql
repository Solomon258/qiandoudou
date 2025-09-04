-- 插入完整的剧本数据
USE qiandoudou;

-- 清理所有剧本相关数据，避免主键冲突
DELETE FROM user_script_progress WHERE script_id IN (1, 2, 3, 4);
DELETE FROM script_chapters WHERE script_id IN (1, 2, 3, 4);
DELETE FROM scripts WHERE id IN (1, 2, 3, 4);

-- 插入剧本：古风古情
INSERT INTO `scripts` (`id`, `title`, `description`, `category_id`, `cover_image`, `total_chapters`, `target_amount`, `duration_days`, `daily_amount`, `follower_count`, `status`) 
VALUES (1, '古风古情', '浪漫的恋爱故事，跟随主角体验甜蜜的爱情旅程', 1, 'https://via.placeholder.com/200x120/ff9a9e/ffffff?text=古风古情', 3, 400.00, 100, 4.00, 50000, 1);

-- 插入章节数据（使用视频内容）
INSERT INTO `script_chapters` (`script_id`, `chapter_number`, `title`, `content`, `choices`, `video_url`) VALUES
(1, 1, '寻佳缘', '今天你在上课铃打响的最后一秒跑进教室，没想到正式上课的第一天，老师就给了你一个下马威——数学老师', '[{"selection": "A.星汉灿烂", "nextId": "2", "cost": 8}, {"selection": "B.星汉灿烂", "nextId": "2", "cost": 10}]', 'static/script/古风古情/寻佳缘.mp4'),
(1, 2, '星汉灿烂', '然说要占用第三节课体育课！"还没有天理了！"你愤然"起义"，反抗老师。', '[{"selection": "A.好结局", "nextId": "3", "cost": 8}, {"selection": "B.好结局", "nextId": "3", "cost": 16}]', 'static/script/古风古情/星汉灿烂.mp4'),
(1, 3, '好结局', '经过一番努力，你终于迎来了美好的结局。', '[{"selection": "A.完美结局", "nextId": null, "cost": 8}, {"selection": "B.完美结局", "nextId": null, "cost": 12}]', 'static/script/古风古情/好结局.mp4');

-- 插入其他推荐剧本
INSERT INTO `scripts` (`id`, `title`, `description`, `category_id`, `cover_image`, `total_chapters`, `target_amount`, `duration_days`, `daily_amount`, `follower_count`, `status`) VALUES
(2, '模拟买房攒钱', '通过模拟买房过程，学会理财攒钱的重要性', 3, 'https://via.placeholder.com/200x120/4facfe/ffffff?text=买房攒钱', 10, 1300, 65, 20, 1500000, 1),
(3, '重新养小时候的自己', '回到童年，重新体验成长的美好时光', 4, 'https://via.placeholder.com/200x120/a8edea/ffffff?text=童年回忆', 10, 1300, 65, 20, 150, 1),
(4, '梦回江南', '一个充满悬疑和推理的互动剧本，根据不同选择走向不同结局', 1, 'https://via.placeholder.com/200x120/8B4513/ffffff?text=梦回江南', 4, 600.00, 120, 5.00, 80000, 1);

-- 为其他剧本添加示例章节
INSERT INTO `script_chapters` (`script_id`, `chapter_number`, `title`, `content`, `choices`, `image_url`) VALUES
(2, 1, '看房之旅', '你决定开始看房，面对琳琅满目的房源信息，你需要做出明智的选择...', '[{"selection": "A.选择市中心小户型", "nextId": "2", "cost": 50}, {"selection": "B.选择郊区大户型", "nextId": "2", "cost": 30}]', 'https://via.placeholder.com/400x300/4facfe/ffffff?text=看房之旅'),
(3, 1, '童年回忆', '回到小时候，你重新体验那些美好的时光，每一个选择都影响着未来的你...', '[{"selection": "A.努力学习", "nextId": "2", "cost": 15}, {"selection": "B.快乐玩耍", "nextId": "2", "cost": 10}]', 'https://via.placeholder.com/400x300/a8edea/ffffff?text=童年回忆');

-- 插入梦回江南剧本章节数据（基于fixed_story_data.json的4层结构）
INSERT INTO `script_chapters` (`script_id`, `chapter_number`, `title`, `content`, `choices`, `video_url`) VALUES
(4, 1, '醒来', '一个老旧的钨丝灯被黑色的电线悬在屋子中央，闪烁着昏暗的光芒。静谧的气氛犹如墨汁滴入清水，正在房间内晕染蔓延...', '[{"selection": "A.质问山羊头为何说\"这里有十个人，为何你说是九位\"", "nextId": "2", "cost": 35}, {"selection": "B.\"不必跟我们介绍了，你已经构成了「非法拘禁罪」\"", "nextId": "3", "cost": 35}, {"selection": "C.你带着犹豫开口问山羊头：\"你…是谁？\"", "nextId": "4", "cost": 55}, {"selection": "D.怒喝道\"我不管这里有几个人…我劝你识相点\"", "nextId": "5", "cost": 40}]', 'static/script/梦回江南/醒来.mp4'),
(4, 2, '说谎者游戏', '山羊头开始介绍游戏规则，一场生死攸关的心理博弈即将展开...', '[{"selection": "A.有些紧张的问道\"我们？创造……什么神？\"", "nextId": "6", "cost": 30}, {"selection": "B.默不作声", "nextId": "9", "cost": 10}]', 'static/script/梦回江南/说谎者游戏.mp4'),
(4, 3, '关键抉择', '在游戏的关键时刻，你需要做出重要的选择，这将决定所有人的命运...', '[{"selection": "A.选择相信团队合作", "nextId": "10", "cost": 25}, {"selection": "B.选择独自分析", "nextId": "14", "cost": 35}]', 'static/script/梦回江南/关键抉择.mp4'),
(4, 4, '最终结局', '经过层层推理和选择，真相即将揭晓，你的命运将在此刻决定...', '[{"selection": "A.选择人羊", "nextId": "24", "cost": 99}, {"selection": "B.选择已经死掉的壮汉", "nextId": "25", "cost": 88}]', 'static/script/梦回江南/最终结局.mp4');
