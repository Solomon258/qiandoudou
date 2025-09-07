-- 钱包公开状态修复脚本
-- 用途：将现有的所有钱包设置为公开状态，使其在社交圈中可见

-- 1. 查看当前钱包的公开状态统计
SELECT 
    is_public,
    COUNT(*) as wallet_count,
    CASE 
        WHEN is_public = 1 THEN '公开'
        WHEN is_public = 0 THEN '私密'
        ELSE '未知'
    END as status_desc
FROM wallets 
WHERE deleted = 0 
GROUP BY is_public;

-- 2. 查看具体哪些钱包是私密状态
SELECT 
    id,
    user_id,
    name,
    type,
    CASE 
        WHEN type = 1 THEN '个人钱包'
        WHEN type = 2 THEN '情侣钱包'
        ELSE '其他类型'
    END as type_desc,
    is_public,
    create_time
FROM wallets 
WHERE deleted = 0 AND is_public = 0
ORDER BY create_time DESC;

-- 3. 将所有现有钱包设置为公开状态
UPDATE wallets 
SET is_public = 1, 
    update_time = NOW()
WHERE deleted = 0 AND is_public = 0;

-- 4. 验证修改结果 - 查看修改后的统计
SELECT 
    is_public,
    COUNT(*) as wallet_count,
    CASE 
        WHEN is_public = 1 THEN '公开'
        WHEN is_public = 0 THEN '私密'
        ELSE '未知'
    END as status_desc
FROM wallets 
WHERE deleted = 0 
GROUP BY is_public;

-- 5. 特别验证"test2-情侣1"钱包的状态
SELECT 
    id,
    user_id,
    name,
    type,
    is_public,
    CASE 
        WHEN is_public = 1 THEN '公开'
        WHEN is_public = 0 THEN '私密'
        ELSE '未知'
    END as status_desc,
    create_time,
    update_time
FROM wallets 
WHERE name LIKE '%test2-情侣1%' AND deleted = 0;
