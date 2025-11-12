-- 修复 balance 列数据类型，支持更大的金额
-- 问题：DECIMAL(10,2) 最大值为 99,999,999.99，超过此限制会报 "Out of range" 错误
-- 解决：修改为 DECIMAL(15,2)，最大值为 999,999,999,999.99（999万亿）

-- 1. 修改 wallets 表的 balance 列
ALTER TABLE `wallets`
MODIFY COLUMN `balance` DECIMAL(15,2) DEFAULT 0.00 COMMENT '余额';

-- 2. 修改 dream_target_amount 列（梦想目标金额也可能超限）
ALTER TABLE `wallets`
MODIFY COLUMN `dream_target_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '梦想目标金额';

-- 3. 修改 transactions 表的 amount 列（如果存在）
ALTER TABLE `transactions`
MODIFY COLUMN `amount` DECIMAL(15,2) NOT NULL COMMENT '交易金额';

-- 4. 确保所有涉及金额的列都使用 DECIMAL(15,2)
-- 修改 dream_progress_history 中的 trigger_amount
ALTER TABLE `dream_progress_history`
MODIFY COLUMN `trigger_amount` DECIMAL(15,2) NOT NULL COMMENT '触发进度变化的金额';

-- 5. 修改 dream_items 中的 suggested_amount
ALTER TABLE `dream_items`
MODIFY COLUMN `suggested_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '建议金额';

-- 验证修改结果
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('wallets', 'transactions', 'dream_progress_history', 'dream_items')
  AND (COLUMN_NAME LIKE '%amount%' OR COLUMN_NAME = 'balance')
ORDER BY TABLE_NAME, COLUMN_NAME;
