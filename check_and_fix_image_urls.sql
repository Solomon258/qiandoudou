-- 检查和修复dream_items表中旅行目的地的image_url
-- 功能：确认当前的image_url值，并提供修复方案

-- 1. 查看当前旅行目的地的image_url
SELECT
  id,
  name,
  display_name,
  image_url,
  bubble_image_url
FROM dream_items
WHERE category = 2
ORDER BY sort_order;

-- 2. 检查哪些钱包使用了这些image_url
SELECT
  w.id,
  w.name as wallet_name,
  w.dream_destination,
  w.dream_item_image,
  di.image_url as correct_image_url,
  CASE
    WHEN w.dream_item_image = di.image_url THEN '✓ 匹配'
    ELSE '✗ 不匹配'
  END as status
FROM wallet w
LEFT JOIN dream_items di ON w.dream_destination = di.name AND di.category = 2
WHERE w.dream_type = 2 AND w.deleted = 0;

-- 3. 检查OSS上实际存在的图片文件名
-- 注意：需要您手动在OSS管理控制台或通过工具确认以下哪些文件存在：
-- 方案A：中文文件名（如果OSS支持）
--   - 阿格拉市.png
--   - 西安.png
-- 方案B：英文文件名
--   - agra.png
--   - xian.png
-- 方案C：其他命名
--   - 阿格拉_back.jpg
--   - 西安_back.jpg

-- 4. 如果实际文件是 "_back.jpg" 格式，执行以下更新
-- UPDATE dream_items
-- SET image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', display_name, '_back.jpg')
-- WHERE category = 2;

-- 5. 如果实际文件是英文名格式（推荐），执行以下更新
-- UPDATE dream_items
-- SET image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', name, '.png'),
--     bubble_image_url = CONCAT('https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/', name, '.png')
-- WHERE category = 2;

-- 6. 更新钱包表，使其与dream_items保持一致
-- UPDATE wallet w
-- SET w.dream_item_image = (
--   SELECT di.image_url
--   FROM dream_items di
--   WHERE di.name = w.dream_destination AND di.category = 2
-- )
-- WHERE w.dream_type = 2 AND w.deleted = 0;
