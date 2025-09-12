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
     * 手机号登录
     */
    @PostMapping("/phone-login")
    public Result<Map<String, Object>> phoneLogin(@RequestBody Map<String, String> request) {
        try {
            String phone = request.get("phone");
            String code = request.get("code");

            if (phone == null || phone.isEmpty()) {
                return Result.error("手机号不能为空");
            }

            if (code == null || code.isEmpty()) {
                return Result.error("验证码不能为空");
            }

            // 验证手机号格式
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                return Result.error("手机号格式不正确");
            }

            // 验证码长度检查（这里是模拟验证，实际项目中应该验证真实验证码）
            if (code.length() != 6) {
                return Result.error("验证码格式不正确");
            }

            // 调用手机号登录服务
            Map<String, Object> loginResult = userService.phoneLogin(phone, code);
            String token = (String) loginResult.get("token");
            User user = (User) loginResult.get("user");

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);

            return Result.success("登录成功", result);
        } catch (Exception e) {
            return Result.error("登录失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current-user")
    public Result<User> getCurrentUser(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                System.out.println("获取用户信息失败：未提供用户ID");
                return Result.error("用户ID不能为空");
            }
            
            System.out.println("获取用户信息，用户ID: " + userId);
            
            // 从数据库获取真实用户信息
            User user = userService.getById(userId);
            
            if (user == null) {
                System.out.println("用户不存在，ID: " + userId);
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

    /**
     * 更新用户昵称
     */
    @PostMapping("/update-nickname")
    public Result<String> updateUserNickname(@RequestBody Map<String, Object> request) {
        try {
            String nickname = request.get("nickname").toString();
            Long userId = request.get("userId") != null ? 
                Long.valueOf(request.get("userId").toString()) : 1961688416014127106L;
            
            System.out.println("开始更新用户昵称 - 用户ID: " + userId + ", 昵称: " + nickname);
            
            // 从数据库获取用户信息
            User user = userService.getById(userId);
            if (user == null) {
                System.out.println("用户不存在，ID: " + userId);
                return Result.error("用户不存在");
            }
            
            // 更新用户昵称
            user.setNickname(nickname);
            boolean updateResult = userService.updateById(user);
            
            if (updateResult) {
                System.out.println("用户 " + userId + " 昵称更新成功: " + nickname);
                return Result.success("昵称更新成功");
            } else {
                System.out.println("用户 " + userId + " 昵称更新失败");
                return Result.error("昵称更新失败");
            }
        } catch (Exception e) {
            System.err.println("更新用户昵称异常: " + e.getMessage());
            return Result.error("昵称更新失败：" + e.getMessage());
        }
    }

    /**
     * 同时更新用户头像和昵称
     */
    @PostMapping("/update-profile")
    public Result<String> updateUserProfile(@RequestBody Map<String, Object> request) {
        try {
            String nickname = request.get("nickname").toString();
            String avatarUrl = request.get("avatarUrl").toString();
            Long userId = request.get("userId") != null ? 
                Long.valueOf(request.get("userId").toString()) : 1961688416014127106L;
            
            System.out.println("开始更新用户资料 - 用户ID: " + userId + ", 昵称: " + nickname + ", 头像: " + avatarUrl);
            
            // 从数据库获取用户信息
            User user = userService.getById(userId);
            if (user == null) {
                System.out.println("用户不存在，ID: " + userId);
                return Result.error("用户不存在");
            }
            
            // 同时更新用户头像和昵称
            user.setNickname(nickname);
            user.setAvatar(avatarUrl);
            boolean updateResult = userService.updateById(user);
            
            if (updateResult) {
                System.out.println("用户 " + userId + " 资料更新成功 - 昵称: " + nickname + ", 头像: " + avatarUrl);
                return Result.success("个人资料更新成功");
            } else {
                System.out.println("用户 " + userId + " 资料更新失败");
                return Result.error("个人资料更新失败");
            }
        } catch (Exception e) {
            System.err.println("更新用户资料异常: " + e.getMessage());
            return Result.error("个人资料更新失败：" + e.getMessage());
        }
    }
}
