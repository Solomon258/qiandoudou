-- 修复user_follows表的并发重复插入问题
-- 问题：频繁点击关注/已关注时，由于软删除+唯一约束的组合，会导致重复插入错误
-- 解决方案：改用INSERT IGNORE + UPDATE的两步方案，避免UUID生成问题

-- 注意：表结构已经正确（uk_follower_following约束已存在）
-- 这个脚本只需要清理数据

-- 1. 清理数据：删除所有软删除的记录（因为新方案会激活重复记录，不需要保留已删除的）
DELETE FROM user_follows WHERE deleted = 1;

-- 2. 验证表结构是否正确
DESC user_follows;

-- 3. 验证约束是否存在
SHOW KEYS FROM user_follows WHERE Key_name = 'uk_follower_following';
