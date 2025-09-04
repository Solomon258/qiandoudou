-- 梦回江南剧本数据插入脚本
-- 基于fixed_story_data.json中的剧本数据生成

USE qiandoudou;

-- 清理现有梦回江南剧本数据，避免主键冲突
DELETE FROM user_script_progress WHERE script_id = 4;
DELETE FROM script_chapters WHERE script_id = 4;
DELETE FROM scripts WHERE id = 4;

-- 插入梦回江南剧本基础信息
INSERT INTO `scripts` (`id`, `title`, `description`, `category_id`, `cover_image`, `total_chapters`, `target_amount`, `duration_days`, `daily_amount`, `follower_count`, `status`) 
VALUES (4, '梦回江南', '一个充满悬疑和推理的互动剧本，根据不同选择走向不同结局', 1, 'https://via.placeholder.com/200x120/8B4513/ffffff?text=梦回江南', 4, 600.00, 120, 5.00, 80000, 1);

-- 插入章节数据（基于JSON结构的4个层级）
INSERT INTO `script_chapters` (`script_id`, `chapter_number`, `title`, `content`, `choices`, `video_url`) VALUES
-- 第1集：醒来
(4, 1, '醒来', '一个老旧的钨丝灯被黑色的电线悬在屋子中央，闪烁着昏暗的光芒。静谧的气氛犹如墨汁滴入清水，正在房间内晕染蔓延...', '[{"selection": "A.质问山羊头为何说\"这里有十个人，为何你说是九位\"", "nextId": "2", "cost": 35}, {"selection": "B.\"不必跟我们介绍了，你已经构成了「非法拘禁罪」\"", "nextId": "3", "cost": 35}, {"selection": "C.你带着犹豫开口问山羊头：\"你…是谁？\"", "nextId": "4", "cost": 55}, {"selection": "D.怒喝道\"我不管这里有几个人…我劝你识相点\"", "nextId": "5", "cost": 40}]', 'static/script/梦回江南/醒来.mp4'),

-- 第2集：说谎者游戏
(4, 2, '说谎者游戏', '山羊头开始介绍游戏规则，一场生死攸关的心理博弈即将展开...', '[{"selection": "A.有些紧张的问道\"我们？创造……什么神？\"", "nextId": "6", "cost": 30}, {"selection": "B.默不作声", "nextId": "9", "cost": 10}]', 'static/script/梦回江南/说谎者游戏.mp4'),

-- 第3集：关键抉择
(4, 3, '关键抉择', '在游戏的关键时刻，你需要做出重要的选择，这将决定所有人的命运...', '[{"selection": "A.选择相信团队合作", "nextId": "10", "cost": 25}, {"selection": "B.选择独自分析", "nextId": "14", "cost": 35}]', 'static/script/梦回江南/关键抉择.mp4'),

-- 第4集：最终结局
(4, 4, '最终结局', '经过层层推理和选择，真相即将揭晓，你的命运将在此刻决定...', '[{"selection": "A.选择人羊", "nextId": "24", "cost": 99}, {"selection": "B.选择已经死掉的壮汉", "nextId": "25", "cost": 88}]', 'static/script/梦回江南/最终结局.mp4');

-- 验证插入结果
SELECT '=== 梦回江南剧本信息 ===' as info;
SELECT * FROM scripts WHERE id = 4;

SELECT '=== 章节信息 ===' as info;
SELECT id, script_id, chapter_number, title, choices, video_url FROM script_chapters WHERE script_id = 4 ORDER BY chapter_number;
