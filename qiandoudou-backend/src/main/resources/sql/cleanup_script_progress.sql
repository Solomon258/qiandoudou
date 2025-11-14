-- ========================================
-- 脚本攒钱用户历史数据清理脚本
-- 日期: 2025-11-14
-- 目的: 清空所有用户的脚本攒钱进度，让用户重新开始
-- 说明: 使用逻辑删除（将deleted设为1），保留钱包和交易记录
-- ========================================

-- 步骤1: 备份原始数据（用于审计和恢复）
CREATE TABLE IF NOT EXISTS user_script_progress_backup_20251114 AS
SELECT * FROM user_script_progress
WHERE script_id = 3 AND deleted = 0;

-- 验证备份
SELECT '备份完成' as message, COUNT(*) as record_count
FROM user_script_progress_backup_20251114;

-- 步骤2: 逻辑删除所有脚本3的用户进度
-- 注意：这里使用UPDATE而不是DELETE，因为系统启用了逻辑删除
UPDATE user_script_progress
SET deleted = 1, update_time = NOW()
WHERE script_id = 3 AND deleted = 0;

-- 验证更新
SELECT '清理完成' as message, COUNT(*) as remaining_count
FROM user_script_progress
WHERE script_id = 3 AND deleted = 0;

-- 步骤3: 检查有多少用户受影响
SELECT '受影响的用户数' as metric, COUNT(DISTINCT user_id) as count
FROM user_script_progress_backup_20251114;

-- 步骤4: 检查这些用户的总转入金额（用于验证钱包一致性）
SELECT
  '用户脚本攒钱总额' as metric,
  COUNT(DISTINCT user_id) as user_count,
  SUM(COALESCE(total_paid, 0)) as total_amount
FROM user_script_progress_backup_20251114;
