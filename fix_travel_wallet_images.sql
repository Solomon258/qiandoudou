-- 修复旅行钱包的背景图片
-- 功能：将所有旅行钱包（dreamType=2）的dreamItemImage更新为dream_items表中的正确image_url
-- 创建日期：2025-10-31

-- 检查修复前的数据
SELECT
  w.id,
  w.name,
  w.dream_type,
  w.dream_destination,
  w.dream_item_image as old_image,
  di.image_url as correct_image
FROM wallet w
LEFT JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
WHERE w.dream_type = 2 AND w.deleted = 0
ORDER BY w.create_time DESC;

-- 执行修复：更新所有旅行钱包的dreamItemImage为正确的image_url
UPDATE wallet w
SET w.dream_item_image = (
  SELECT di.image_url
  FROM dream_items di
  WHERE di.name = w.dream_destination AND di.category = 2
)
WHERE w.dream_type = 2 AND w.deleted = 0;

-- 验证修复结果
SELECT
  w.id,
  w.name,
  w.dream_type,
  w.dream_destination,
  w.dream_item_image as updated_image,
  di.image_url as dream_items_image,
  CASE
    WHEN w.dream_item_image = di.image_url THEN '✓ 正确'
    ELSE '✗ 不匹配'
  END as status
FROM wallet w
LEFT JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
WHERE w.dream_type = 2 AND w.deleted = 0
ORDER BY w.create_time DESC;

-- 总结统计
SELECT
  COUNT(*) as total_travel_wallets,
  SUM(CASE WHEN dream_item_image LIKE '%.png' AND dream_item_image NOT LIKE '%part%' THEN 1 ELSE 0 END) as fixed_count,
  SUM(CASE WHEN dream_item_image LIKE '%part%' THEN 1 ELSE 0 END) as still_broken_count
FROM wallet
WHERE dream_type = 2 AND deleted = 0;
