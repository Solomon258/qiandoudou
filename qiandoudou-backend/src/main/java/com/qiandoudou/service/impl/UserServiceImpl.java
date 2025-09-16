package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiandoudou.entity.User;
import com.qiandoudou.entity.UserLoginLog;
import com.qiandoudou.entity.WeChatLoginResponse;
import com.qiandoudou.config.WeChatConfig;
import com.qiandoudou.mapper.UserMapper;
import com.qiandoudou.service.UserService;
import com.qiandoudou.service.UserLoginLogService;
import com.qiandoudou.util.JwtUtil;
import com.qiandoudou.util.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private WeChatConfig weChatConfig;

    @Autowired
    private UserLoginLogService userLoginLogService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前请求信息
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 记录登录成功日志
     */
    private void recordSuccessLoginLog(Long userId, String loginType, String loginAccount) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = HttpUtil.getClientIpAddress(request);
            String userAgent = HttpUtil.getSimplifiedUserAgent(request);
            userLoginLogService.recordSuccessLogin(userId, loginType, loginAccount, ipAddress, userAgent);
        } catch (Exception e) {
            System.err.println("记录登录成功日志失败: " + e.getMessage());
        }
    }

    /**
     * 记录登录失败日志
     */
    private void recordFailureLoginLog(Long userId, String loginType, String loginAccount, String failureReason) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = HttpUtil.getClientIpAddress(request);
            String userAgent = HttpUtil.getSimplifiedUserAgent(request);
            userLoginLogService.recordFailureLogin(userId, loginType, loginAccount, ipAddress, userAgent, failureReason);
        } catch (Exception e) {
            System.err.println("记录登录失败日志失败: " + e.getMessage());
        }
    }

    @Override
    public String login(String username, String password) {
        User user = null;
        try {
            // 根据用户名查找用户
            user = getUserByUsername(username);
            if (user == null) {
                // 记录失败日志 - 用户不存在
                recordFailureLoginLog(null, UserLoginLog.LoginType.USERNAME.getCode(), username, "用户不存在");
                throw new RuntimeException("用户不存在");
            }

            // 验证密码 (Demo版本使用明文比较)
            if (!password.equals(user.getPassword())) {
                // 记录失败日志 - 密码错误
                recordFailureLoginLog(user.getId(), UserLoginLog.LoginType.USERNAME.getCode(), username, "密码错误");
                throw new RuntimeException("密码错误");
            }

            // 生成JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            // 记录成功登录日志
            recordSuccessLoginLog(user.getId(), UserLoginLog.LoginType.USERNAME.getCode(), username);
            
            return token;
        } catch (RuntimeException e) {
            throw e; // 重新抛出已知异常
        } catch (Exception e) {
            // 记录系统异常
            if (user != null) {
                recordFailureLoginLog(user.getId(), UserLoginLog.LoginType.USERNAME.getCode(), username, "系统异常: " + e.getMessage());
            }
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    @Override
    public User register(String username, String password, String nickname, String phone) {
        // 检查用户名是否已存在
        if (getUserByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查手机号是否已存在 - 修复空字符串问题
        if (phone != null && !phone.trim().isEmpty() && getUserByPhone(phone) != null) {
            throw new RuntimeException("手机号已注册");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // Demo版本直接存储明文密码
        user.setNickname(nickname);
        user.setPhone(phone);

        // 保存用户
        save(user);
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return getOne(queryWrapper);
    }

    @Override
    public User getUserByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        return getOne(queryWrapper);
    }

    @Override
    public String wechatLogin(String code) {
        try {
            System.out.println("开始微信登录，code: " + code);
            
            // 检查微信配置
            if (!StringUtils.hasText(weChatConfig.getAppid()) || 
                !StringUtils.hasText(weChatConfig.getSecret()) ||
                weChatConfig.getAppid().equals("your_wx_appid")) {
                System.out.println("微信配置未正确设置，使用演示模式");
                return handleDemoWeChatLogin(code);
            }
            
            // 调用微信API获取openid和session_key
            String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    weChatConfig.getAuthUrl(),
                    weChatConfig.getAppid(),
                    weChatConfig.getSecret(),
                    code);
            
            System.out.println("调用微信API: " + url.replaceAll("secret=[^&]+", "secret=***"));
            
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("微信API响应: " + response);
            
            WeChatLoginResponse weChatResponse = objectMapper.readValue(response, WeChatLoginResponse.class);
            
            // 检查微信API是否返回错误
            if (weChatResponse.getErrcode() != null && weChatResponse.getErrcode() != 0) {
                throw new RuntimeException("微信登录失败: " + weChatResponse.getErrmsg());
            }
            
            // 检查是否获取到openid
            if (!StringUtils.hasText(weChatResponse.getOpenid())) {
                throw new RuntimeException("未获取到用户openid");
            }
            
            String openid = weChatResponse.getOpenid();
            System.out.println("获取到用户openid: " + openid);
            
            // 查找是否已有该openid的用户
            User user = getUserByOpenid(openid);
            
            if (user == null) {
                // 如果用户不存在，创建新用户
                user = new User();
                user.setUsername("wx_" + System.currentTimeMillis());
                user.setNickname("微信用户");
                user.setOpenid(openid);
                user.setPassword(""); // 微信用户设置空密码
                // 设置默认头像 - 使用存在的头像路径
                user.setAvatar("https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/53EAEFAA-39B8-4E6C-B88C-1DB241C01C23.png");
                save(user);
                System.out.println("创建新用户: " + user.getId());
            } else {
                System.out.println("找到现有用户: " + user.getId());
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            System.out.println("生成token成功");
            
            return token;
        } catch (Exception e) {
            System.err.println("微信登录异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> wechatLoginWithUser(String code) {
        User user = null;
        String openid = null;
        try {
            System.out.println("开始微信登录并获取用户信息，code: " + code);
            
            String token;
            
            // 检查是否启用开发模式
            if (weChatConfig.isDevMode()) {
                System.out.println("开发模式已启用，使用演示模式跳过IP白名单限制");
                token = handleDemoWeChatLogin(code);
                openid = "demo_wx_fixed_user";
                user = getUserByOpenid(openid);
            } else {
                // 检查微信配置
                if (!StringUtils.hasText(weChatConfig.getAppid()) || 
                    !StringUtils.hasText(weChatConfig.getSecret()) ||
                    weChatConfig.getAppid().equals("your_wx_appid")) {
                    System.out.println("微信配置未正确设置，使用演示模式");
                    token = handleDemoWeChatLogin(code);
                    openid = "demo_wx_fixed_user";
                    user = getUserByOpenid(openid);
                } else {
                    // 调用真实微信API - 使用统一处理方法
                    Map<String, Object> wechatResult = handleRealWeChatLogin(code);
                    token = (String) wechatResult.get("token");
                    user = (User) wechatResult.get("user");
                    openid = user != null ? user.getOpenid() : null;
                }
            }
            
            // 记录成功登录日志
            if (user != null && openid != null) {
                recordSuccessLoginLog(user.getId(), UserLoginLog.LoginType.WECHAT.getCode(), openid);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);
            
            System.out.println("微信登录完成，用户: " + (user != null ? user.getNickname() : "null"));
            return result;
        } catch (Exception e) {
            System.err.println("微信登录异常: " + e.getMessage());
            e.printStackTrace();
            // 记录失败登录日志
            if (user != null && openid != null) {
                recordFailureLoginLog(user.getId(), UserLoginLog.LoginType.WECHAT.getCode(), openid, "微信登录异常: " + e.getMessage());
            } else {
                recordFailureLoginLog(null, UserLoginLog.LoginType.WECHAT.getCode(), code, "微信登录异常: " + e.getMessage());
            }
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 处理真实微信登录（统一方法）
     */
    private Map<String, Object> handleRealWeChatLogin(String code) {
        try {
            System.out.println("处理真实微信登录，code: " + code);
            
            // 调用微信API获取openid和session_key
            String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    weChatConfig.getAuthUrl(),
                    weChatConfig.getAppid(),
                    weChatConfig.getSecret(),
                    code);
            
            System.out.println("调用微信API: " + url.replaceAll("secret=[^&]+", "secret=***"));
            
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("微信API响应: " + response);
            
            // 解析响应
            ObjectMapper objectMapper = new ObjectMapper();
            WeChatLoginResponse weChatResponse = objectMapper.readValue(response, WeChatLoginResponse.class);
            
            if (weChatResponse.getErrcode() != null && weChatResponse.getErrcode() != 0) {
                throw new RuntimeException("微信API调用失败: " + weChatResponse.getErrmsg());
            }
            
            // 检查是否获取到openid
            if (!StringUtils.hasText(weChatResponse.getOpenid())) {
                throw new RuntimeException("未获取到用户openid");
            }
            
            String openid = weChatResponse.getOpenid();
            System.out.println("获取到用户openid: " + openid);
            
            // 查找是否已有该openid的用户
            User user = getUserByOpenid(openid);
            
            if (user == null) {
                // 如果用户不存在，创建新用户
                user = new User();
                user.setUsername("wx_" + System.currentTimeMillis());
                user.setNickname("微信用户");
                user.setOpenid(openid);
                user.setPassword(""); // 微信用户设置空密码
                user.setAvatar("https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/default_avatar.png");
                save(user);
                System.out.println("创建新用户: " + user.getId());
            } else {
                System.out.println("找到现有用户: " + user.getId());
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            System.out.println("生成token成功");
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);
            
            return result;
        } catch (Exception e) {
            System.err.println("真实微信登录异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 演示模式的微信登录（用于开发测试）
     */
    private String handleDemoWeChatLogin(String code) {
        System.out.println("使用演示模式处理微信登录，code: " + code);
        
        // 使用固定的演示openid，确保同一个微信用户每次登录都是同一个账号
        String openid = "demo_wx_fixed_user";
        
        // 查找是否已有该openid的用户
        User user = getUserByOpenid(openid);
        
        if (user == null) {
            // 如果用户不存在，创建新用户
            user = new User();
            user.setUsername("wx_demo_user");
            user.setNickname("微信演示用户");
            user.setOpenid(openid);
            user.setPassword(""); // 微信用户设置空密码
            user.setAvatar("https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/default_avatar.png");
            save(user);
            System.out.println("创建固定演示用户: " + user.getId() + ", openid: " + openid);
        } else {
            System.out.println("找到现有演示用户: " + user.getId() + ", openid: " + openid);
        }
        
        // 生成JWT token
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public User getUserByOpenid(String openid) {
        if (openid == null) {
            return null;
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getOpenid, openid);
        return getOne(queryWrapper);
    }

    @Override
    public Map<String, Object> phoneLogin(String phone, String code) {
        User user = null;
        try {
            System.out.println("开始手机号登录，手机号: " + phone + ", 验证码: " + code);
            
            // 查找是否已有该手机号的用户
            user = getUserByPhone(phone);
            
            if (user == null) {
                // 如果用户不存在，创建新用户（登录即注册）
                user = new User();
                user.setUsername("phone_" + phone); // 用手机号作为用户名
                user.setNickname("用户" + phone.substring(phone.length() - 4)); // 用手机号后4位作为昵称
                user.setPhone(phone);
                user.setPassword(""); // 手机号登录用户设置空密码
                user.setAvatar("https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/default_avatar.png");
                save(user);
                System.out.println("创建新手机号用户: " + user.getId() + ", 手机号: " + phone);
            } else {
                System.out.println("找到现有手机号用户: " + user.getId() + ", 手机号: " + phone);
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            // 记录成功登录日志
            recordSuccessLoginLog(user.getId(), UserLoginLog.LoginType.PHONE.getCode(), phone);
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);
            
            System.out.println("手机号登录完成，用户: " + user.getNickname());
            return result;
        } catch (Exception e) {
            System.err.println("手机号登录异常: " + e.getMessage());
            // 记录失败登录日志
            if (user != null) {
                recordFailureLoginLog(user.getId(), UserLoginLog.LoginType.PHONE.getCode(), phone, "登录异常: " + e.getMessage());
            } else {
                recordFailureLoginLog(null, UserLoginLog.LoginType.PHONE.getCode(), phone, "登录异常: " + e.getMessage());
            }
            throw new RuntimeException("手机号登录失败: " + e.getMessage());
        }
    }
}
