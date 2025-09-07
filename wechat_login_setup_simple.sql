-- 微信登录功能数据库设置脚本（简化版）
-- 注意：如果openid字段已存在，会报错，但不影响功能

USE qiandoudou;

-- 查看当前users表结构
DESCRIBE users;

-- 尝试添加openid字段（如果已存在会报错，可以忽略）
-- 方法1：直接添加（如果字段已存在会报错）
ALTER TABLE users ADD COLUMN openid VARCHAR(50) COMMENT '微信openid' AFTER phone;

-- 为openid字段创建索引
CREATE INDEX idx_users_openid ON users(openid);

-- 再次查看表结构确认字段已添加
DESCRIBE users;

-- 创建一些测试数据（可选）
INSERT IGNORE INTO users (username, nickname, openid, avatar, create_time, update_time) 
VALUES 
('demo_user_1', '演示用户1', 'demo_wx_test1', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/default_avatar.png', NOW(), NOW()),
('demo_user_2', '演示用户2', 'demo_wx_test2', 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/default_avatar.png', NOW(), NOW());

-- 查看插入的测试数据
SELECT id, username, nickname, openid, avatar, create_time FROM users WHERE openid LIKE 'demo_wx_%';

COMMIT;
