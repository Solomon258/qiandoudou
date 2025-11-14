-- 为搭子圈子添加背景图
-- 根据用户提供的信息，西天取经小队使用卡片背景图作为钱包背景图

UPDATE `buddy_groups` SET `background_image` = 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/xitian_cover.jpg' WHERE `name` = '西天取经小队';
UPDATE `buddy_groups` SET `background_image` = 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/liangshan_cover.jpg' WHERE `name` = '水泊梁山';
UPDATE `buddy_groups` SET `background_image` = 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/sanguo_cover.jpg' WHERE `name` = '三国英雄';
UPDATE `buddy_groups` SET `background_image` = 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/groups/daomu_cover.jpg' WHERE `name` = '盗墓笔记铁三角';
