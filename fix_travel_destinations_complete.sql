-- 完整修复旅行目的地图片URL
-- 问题：1. 文件名包含中文字符 2. targetAmount可能为0
-- 创建时间：2025-10-31

-- ==================================================
-- 第一步：检查当前 dream_items 表的数据
-- ==================================================
SELECT
  id,
  name,
  display_name,
  image_url,
  suggested_amount
FROM dream_items
WHERE category = 2
ORDER BY sort_order;

-- ==================================================
-- 第二步：检查已创建的旅行钱包
-- ==================================================
SELECT
  w.id,
  w.name as wallet_name,
  w.dream_destination,
  w.dream_target_amount,
  w.dream_item_image,
  di.name as item_name,
  di.image_url as correct_image_url,
  CASE
    WHEN w.dream_item_image LIKE '%北京%' THEN '包含中文字符'
    WHEN w.dream_item_image LIKE '%part%' THEN '错误的进度图片'
    WHEN w.dream_item_image = di.image_url THEN '✓ 正确'
    ELSE '不匹配'
  END as status
FROM wallet w
LEFT JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
WHERE w.dream_type = 2 AND w.deleted = 0;

-- ==================================================
-- 第三步：确认OSS上实际的文件命名格式
-- ==================================================
-- 请在OSS控制台或通过工具检查以下路径中实际存在的文件：
-- https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/
--
-- 可能的文件命名格式：
-- 方案A：中文.png (北京.png, 西安.png)
-- 方案B：英文.png (beijing.png, xian.png) ← 推荐
-- 方案C：中文_back.jpg (北京_back.jpg, 西安_back.jpg)

-- ==================================================
-- 第四步：根据实际情况选择执行以下SQL之一
-- ==================================================

-- 方案A：如果OSS文件是中文.png格式
-- UPDATE dream_items
-- SET image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', display_name, '.png'),
--     bubble_image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', display_name, '.png')
-- WHERE category = 2;

-- 方案B：如果OSS文件是英文.png格式（推荐）
-- UPDATE dream_items
-- SET image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', name, '.png'),
--     bubble_image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', name, '.png')
-- WHERE category = 2;

-- 方案C：如果OSS文件是中文_back.jpg格式
-- UPDATE dream_items
-- SET image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', display_name, '_back.jpg'),
--     bubble_image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', display_name, '_back.jpg')
-- WHERE category = 2;

-- 方案D：如果OSS文件是英文_back.jpg格式
-- UPDATE dream_items
-- SET image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', name, '_back.jpg'),
--     bubble_image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', name, '_back.jpg')
-- WHERE category = 2;

-- ==================================================
-- 第五步：更新已创建的钱包，使其与 dream_items 保持一致
-- ==================================================
-- 注意：此步骤在更新 dream_items 表之后执行
UPDATE wallet w
INNER JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
SET w.dream_item_image = di.image_url
WHERE w.dream_type = 2 AND w.deleted = 0;

-- ==================================================
-- 第六步：修复目标金额为0的钱包
-- ==================================================
-- 将目标金额更新为 dream_items 表中的建议金额
UPDATE wallet w
INNER JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
SET w.dream_target_amount = di.suggested_amount
WHERE w.dream_type = 2
  AND w.deleted = 0
  AND (w.dream_target_amount IS NULL OR w.dream_target_amount = 0);

-- ==================================================
-- 第七步：验证修复结果
-- ==================================================
SELECT
  w.id,
  w.name as wallet_name,
  w.dream_destination,
  w.dream_target_amount,
  w.dream_item_image,
  di.image_url as correct_image_url,
  di.suggested_amount,
  CASE
    WHEN w.dream_item_image = di.image_url THEN '✓ 图片正确'
    ELSE '✗ 图片不匹配'
  END as image_status,
  CASE
    WHEN w.dream_target_amount > 0 THEN '✓ 金额正确'
    ELSE '✗ 金额为0'
  END as amount_status
FROM wallet w
LEFT JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
WHERE w.dream_type = 2 AND w.deleted = 0
ORDER BY w.create_time DESC;

-- ==================================================
-- 测试结果说明
-- ==================================================
-- 执行完毕后：
-- 1. 所有旅行钱包的 dream_item_image 应该与 dream_items.image_url 一致
-- 2. 所有旅行钱包的 dream_target_amount 应该大于0
-- 3. 图片URL应该能在浏览器中正常访问
