-- 用户登录记录表
CREATE TABLE IF NOT EXISTS user_login_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    login_type VARCHAR(20) NOT NULL COMMENT '登录类型：USERNAME(用户名)、PHONE(手机号)、WECHAT(微信)',
    login_account VARCHAR(100) COMMENT '登录账号（用户名/手机号/微信openid）',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    ip_address VARCHAR(50) COMMENT '登录IP地址',
    user_agent TEXT COMMENT '用户代理信息（浏览器/设备信息）',
    login_status TINYINT NOT NULL DEFAULT 1 COMMENT '登录状态：0-失败，1-成功',
    failure_reason VARCHAR(200) COMMENT '登录失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time),
    INDEX idx_login_type (login_type),
    INDEX idx_login_account (login_account),
    INDEX idx_login_status (login_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录记录表';
