-- 修复user_follows表软删除导致的唯一约束问题
-- 问题：软删除的记录（deleted=1）仍然占用唯一约束，导致重新关注时唯一约束冲突
-- 解决方案：改为真删除（完全删除记录），而不是软删除

-- 1. 删除所有软删除的记录（彻底清理）
DELETE FROM user_follows WHERE deleted = 1;

-- 2. 如果user_follows表有deleted列但不再使用，可以考虑在未来删除这一列
-- 但为了向后兼容性，暂时保留deleted列，但所有新操作都使用真删除

-- 3. 验证表中数据
SELECT COUNT(*) as total_records FROM user_follows;
SELECT COUNT(*) as deleted_records FROM user_follows WHERE deleted = 1;

-- 4. 验证唯一约束
SHOW KEYS FROM user_follows WHERE Key_name = 'uk_follower_following';
