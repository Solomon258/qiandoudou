-- 修复旅行目的地图片URL和建议金额
-- 文件格式：中文_back.jpg (如：北京_back.jpg)
-- 执行时间：2025-10-31

-- ==================================================
-- 第一步：查看修复前的数据
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
-- 第二步：更新 dream_items 表的 image_url
-- ==================================================
-- 格式：中文display_name + '_back.jpg'
UPDATE dream_items
SET
  image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', display_name, '_back.jpg'),
  bubble_image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', display_name, '_back.jpg')
WHERE category = 2;

-- ==================================================
-- 第三步：验证 dream_items 更新结果
-- ==================================================
SELECT
  id,
  name,
  display_name,
  image_url,
  bubble_image_url,
  suggested_amount
FROM dream_items
WHERE category = 2
ORDER BY sort_order;

-- 应该看到类似：
-- beijing | 北京 | https://...北京_back.jpg
-- xian    | 西安 | https://...西安_back.jpg

-- ==================================================
-- 第四步：更新已创建的旅行钱包，同步 dream_items 的 image_url
-- ==================================================
UPDATE wallet w
INNER JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
SET w.dream_item_image = di.image_url
WHERE w.dream_type = 2 AND w.deleted = 0;

-- ==================================================
-- 第五步：修复目标金额为0的钱包
-- ==================================================
-- 将目标金额设置为 dream_items 表中的 suggested_amount
UPDATE wallet w
INNER JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
SET w.dream_target_amount = di.suggested_amount
WHERE w.dream_type = 2
  AND w.deleted = 0
  AND (w.dream_target_amount IS NULL OR w.dream_target_amount = 0);

-- ==================================================
-- 第六步：验证钱包修复结果
-- ==================================================
SELECT
  w.id,
  w.name as wallet_name,
  w.dream_destination,
  w.dream_target_amount,
  w.dream_item_image,
  di.display_name,
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
-- 第七步：统计修复结果
-- ==================================================
SELECT
  COUNT(*) as total_travel_wallets,
  SUM(CASE WHEN dream_target_amount > 0 THEN 1 ELSE 0 END) as fixed_amount_count,
  SUM(CASE WHEN dream_item_image LIKE '%_back.jpg' THEN 1 ELSE 0 END) as fixed_image_count
FROM wallet
WHERE dream_type = 2 AND deleted = 0;

-- ==================================================
-- 执行完成后的预期结果
-- ==================================================
-- 1. dream_items.image_url 格式：https://...北京_back.jpg
-- 2. wallet.dream_item_image 与 dream_items.image_url 一致
-- 3. wallet.dream_target_amount 从 dream_items.suggested_amount 获取（>0）
-- 4. 前端能正常加载图片
