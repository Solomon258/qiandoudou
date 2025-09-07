-- 微信登录功能数据库设置脚本
-- 确保users表有openid字段用于微信登录

USE qiandoudou;

-- 检查并添加openid字段（如果不存在）
-- 使用存储过程方式安全添加字段
DELIMITER //
CREATE PROCEDURE AddOpenidColumn()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO column_exists 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'qiandoudou' 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'openid';
    
    IF column_exists = 0 THEN
        ALTER TABLE users ADD COLUMN openid VARCHAR(50) COMMENT '微信openid' AFTER phone;
    END IF;
END //
DELIMITER ;

-- 执行存储过程
CALL AddOpenidColumn();

-- 删除临时存储过程
DROP PROCEDURE AddOpenidColumn;

-- 为openid字段创建索引（如果不存在）
CREATE INDEX idx_users_openid ON users(openid);

-- 查看当前users表结构
DESCRIBE users;

-- 创建一些测试数据（可选）
INSERT IGNORE INTO users (username, nickname, openid, avatar, create_time, update_time) 
VALUES 
('demo_user_1', '演示用户1', 'demo_wx_test1', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/default_avatar.png', NOW(), NOW()),
('demo_user_2', '演示用户2', 'demo_wx_test2', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/default_avatar.png', NOW(), NOW());

-- 查看插入的测试数据
SELECT id, username, nickname, openid, avatar, create_time FROM users WHERE openid LIKE 'demo_wx_%';

COMMIT;
