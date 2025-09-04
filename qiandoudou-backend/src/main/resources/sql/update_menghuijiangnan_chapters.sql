-- 更新梦回江南剧本集数为4集
-- 如果剧本已存在但集数不正确，使用此脚本更新

USE qiandoudou;

-- 更新梦回江南剧本的总集数为4
UPDATE `scripts` 
SET `total_chapters` = 4 
WHERE `title` = '梦回江南';

-- 验证更新结果
SELECT '=== 梦回江南剧本更新后信息 ===' as info;
SELECT id, title, total_chapters, description FROM scripts WHERE title = '梦回江南';
