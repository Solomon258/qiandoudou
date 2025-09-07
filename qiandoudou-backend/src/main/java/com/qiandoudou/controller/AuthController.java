package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.entity.User;
import com.qiandoudou.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            String token = userService.login(username, password);
            User user = userService.getUserByUsername(username);

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);

            return Result.success("登录成功", result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<User> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String nickname = request.get("nickname");
            String phone = request.get("phone");

            User user = userService.register(username, password, nickname, phone);
            return Result.success("注册成功", user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 微信登录
     */
    @PostMapping("/wechat-login")
    public Result<Map<String, Object>> wechatLogin(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            if (code == null || code.isEmpty()) {
                return Result.error("微信授权码不能为空");
            }

            // 调用微信登录服务，返回token和用户信息
            Map<String, Object> loginResult = userService.wechatLoginWithUser(code);
            String token = (String) loginResult.get("token");
            User user = (User) loginResult.get("user");

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);

            return Result.success("微信登录成功", result);
        } catch (Exception e) {
            return Result.error("微信登录失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current-user")
    public Result<User> getCurrentUser(@RequestParam(required = false) Long userId) {
        try {
            // 如果没有提供userId参数，使用默认的test1用户ID
            Long targetUserId = userId != null ? userId : 1961688416014127106L;
            
            System.out.println("获取用户信息，用户ID: " + targetUserId);
            
            // 从数据库获取真实用户信息
            User user = userService.getById(targetUserId);
            
            if (user == null) {
                System.out.println("用户不存在，ID: " + targetUserId);
                return Result.error("用户不存在");
            }
            
            System.out.println("获取到用户信息: " + user.getNickname() + ", 头像: " + user.getAvatar());
            
            return Result.success("获取用户信息成功", user);
        } catch (Exception e) {
            System.err.println("获取用户信息失败: " + e.getMessage());
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户头像
     */
    @PostMapping("/update-avatar")
    public Result<String> updateUserAvatar(@RequestBody Map<String, Object> request) {
        try {
            String avatarUrl = request.get("avatarUrl").toString();
            Long userId = request.get("userId") != null ? 
                Long.valueOf(request.get("userId").toString()) : 1961688416014127106L;
            
            System.out.println("开始更新用户头像 - 用户ID: " + userId + ", 头像URL: " + avatarUrl);
            
            // 从数据库获取用户信息
            User user = userService.getById(userId);
            if (user == null) {
                System.out.println("用户不存在，ID: " + userId);
                return Result.error("用户不存在");
            }
            
            // 更新用户头像
            user.setAvatar(avatarUrl);
            boolean updateResult = userService.updateById(user);
            
            if (updateResult) {
                System.out.println("用户 " + userId + " 头像更新成功: " + avatarUrl);
                return Result.success("头像更新成功");
            } else {
                System.out.println("用户 " + userId + " 头像更新失败");
                return Result.error("头像更新失败");
            }
        } catch (Exception e) {
            System.err.println("更新用户头像异常: " + e.getMessage());
            return Result.error("头像更新失败：" + e.getMessage());
        }
    }
}
