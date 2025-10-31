-- Migration: Add option_config field to dream_items table
-- Purpose: Store product options (brands/models) and their suggested amounts

-- Add option_config field
ALTER TABLE `dream_items`
ADD COLUMN `option_config` JSON NULL COMMENT '产品选项配置JSON，包含品牌选项和建议金额数组';

-- Show table structure after adding field
DESCRIBE `dream_items`;