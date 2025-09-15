package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiandoudou.entity.UserLoginLog;
import com.qiandoudou.mapper.UserLoginLogMapper;
import com.qiandoudou.service.UserLoginLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户登录记录服务实现类
 */
@Service
public class UserLoginLogServiceImpl extends ServiceImpl<UserLoginLogMapper, UserLoginLog> 
        implements UserLoginLogService {

    @Override
    public void recordLoginLog(Long userId, String loginType, String loginAccount, 
                              String ipAddress, String userAgent, Integer loginStatus, String failureReason) {
        try {
            UserLoginLog loginLog = new UserLoginLog();
            loginLog.setUserId(userId);
            loginLog.setLoginType(loginType);
            loginLog.setLoginAccount(loginAccount);
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setIpAddress(ipAddress);
            loginLog.setUserAgent(userAgent);
            loginLog.setLoginStatus(loginStatus);
            loginLog.setFailureReason(failureReason);
            
            save(loginLog);
            
            System.out.println("登录日志记录成功: userId=" + userId + ", loginType=" + loginType + 
                             ", status=" + (loginStatus == 1 ? "成功" : "失败"));
        } catch (Exception e) {
            // 记录日志失败不应该影响正常的登录流程，只打印错误日志
            System.err.println("记录登录日志失败: " + e.getMessage());
        }
    }

    @Override
    public void recordSuccessLogin(Long userId, String loginType, String loginAccount, 
                                  String ipAddress, String userAgent) {
        recordLoginLog(userId, loginType, loginAccount, ipAddress, userAgent, 
                      UserLoginLog.LoginStatus.SUCCESS.getCode(), null);
    }

    @Override
    public void recordFailureLogin(Long userId, String loginType, String loginAccount, 
                                  String ipAddress, String userAgent, String failureReason) {
        recordLoginLog(userId, loginType, loginAccount, ipAddress, userAgent, 
                      UserLoginLog.LoginStatus.FAILURE.getCode(), failureReason);
    }

    @Override
    public IPage<Map<String, Object>> getLoginLogPage(Integer pageNum, Integer pageSize,
                                                     Long userId, String loginType, String loginAccount,
                                                     LocalDateTime startTime, LocalDateTime endTime, Integer loginStatus) {
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectLoginLogPage(page, userId, loginType, loginAccount, startTime, endTime, loginStatus);
    }

    @Override
    public List<UserLoginLog> getRecentLoginLogs(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10; // 默认查询最近10条
        }
        return baseMapper.selectRecentLoginLogs(userId, limit);
    }

    @Override
    public List<Map<String, Object>> getLoginStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectLoginStatistics(userId, startTime, endTime);
    }

    @Override
    public Integer getTodayLoginUserCount() {
        return baseMapper.selectTodayLoginUserCount();
    }

    @Override
    public Integer getActiveUserCount(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectActiveUserCount(startTime, endTime);
    }
}
