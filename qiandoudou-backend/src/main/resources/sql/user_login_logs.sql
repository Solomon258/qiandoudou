SELECT l.*, u.nickname, u.username, u.phone,
       CASE
           WHEN l.login_type = 'USERNAME' THEN CONCAT('用户名: ', l.login_account)
           WHEN l.login_type = 'PHONE' THEN CONCAT('手机号: ', l.login_account)
           WHEN l.login_type = 'WECHAT' THEN CONCAT('微信用户: ', COALESCE(u.nickname, '未知用户'))
           ELSE l.login_account
           END AS display_account,
       l.login_account as original_account
FROM user_login_logs l
         LEFT JOIN users u ON l.user_id = u.id