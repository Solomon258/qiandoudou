-- 插入旅行目的地数据
-- 创建日期：2025-01-22
-- 功能：插入7个旅行目的地到 dream_items 表

-- 先删除旧的旅行目的地数据（如果存在）
DELETE FROM `dream_items` WHERE `category` = 2;

-- 插入新的旅行目的地数据
INSERT INTO `dream_items` (`category`, `name`, `display_name`, `image_url`, `bubble_image_url`, `description`, `suggested_amount`, `sort_order`, `is_active`) VALUES
(2, 'beijing', '北京', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/北京.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/北京.png', '首都北京，历史文化之旅', 5000.00, 1, 1),
(2, 'xian', '西安', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/西安.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/西安.png', '古都西安，兵马俑之旅', 3000.00, 2, 1),
(2, 'lasa', '拉萨', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/拉萨.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/拉萨.png', '圣城拉萨，雪域高原', 8000.00, 3, 1),
(2, 'sydney', '悉尼', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/悉尼.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/悉尼.png', '澳洲悉尼，海港城市', 15000.00, 4, 1),
(2, 'singapore', '新加坡', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/新加坡.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/新加坡.png', '狮城新加坡，花园城市', 12000.00, 5, 1),
(2, 'agra', '阿格拉市', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/阿格拉市.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/阿格拉市.png', '印度阿格拉市，泰姬陵所在地', 10000.00, 6, 1),
(2, 'chongqing', '重庆', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/重庆.png', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/重庆.png', '山城重庆，火辣美食', 4000.00, 7, 1);

-- 执行完成提示
SELECT 'Travel destinations inserted successfully!' as result;
