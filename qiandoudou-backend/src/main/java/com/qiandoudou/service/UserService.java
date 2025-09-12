package com.qiandoudou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiandoudou.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     */
    String login(String username, String password);

    /**
     * 用户注册
     */
    User register(String username, String password, String nickname, String phone);

    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);

    /**
     * 根据手机号获取用户
     */
    User getUserByPhone(String phone);

    /**
     * 微信登录
     */
    String wechatLogin(String code);

    /**
     * 微信登录并返回用户信息
     */
    Map<String, Object> wechatLoginWithUser(String code);

    /**
     * 根据openid获取用户
     */
    User getUserByOpenid(String openid);

    /**
     * 手机号登录
     */
    Map<String, Object> phoneLogin(String phone, String code);
}
