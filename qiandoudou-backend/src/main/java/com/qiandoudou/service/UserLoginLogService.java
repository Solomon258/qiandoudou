package com.qiandoudou.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qiandoudou.entity.UserLoginLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户登录记录服务接口
 */
public interface UserLoginLogService extends IService<UserLoginLog> {

    /**
     * 记录登录日志
     */
    void recordLoginLog(Long userId, String loginType, String loginAccount, 
                       String ipAddress, String userAgent, Integer loginStatus, String failureReason);

    /**
     * 记录成功登录日志
     */
    void recordSuccessLogin(Long userId, String loginType, String loginAccount, 
                           String ipAddress, String userAgent);

    /**
     * 记录失败登录日志
     */
    void recordFailureLogin(Long userId, String loginType, String loginAccount, 
                           String ipAddress, String userAgent, String failureReason);

    /**
     * 分页查询登录记录
     */
    IPage<Map<String, Object>> getLoginLogPage(Integer pageNum, Integer pageSize,
                                              Long userId, String loginType, String loginAccount,
                                              LocalDateTime startTime, LocalDateTime endTime, Integer loginStatus);

    /**
     * 获取用户最近的登录记录
     */
    List<Map<String, Object>> getRecentLoginLogs(Long userId, Integer limit);

    /**
     * 获取登录统计数据
     */
    List<Map<String, Object>> getLoginStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取今日登录用户数
     */
    Integer getTodayLoginUserCount();

    /**
     * 获取指定时间段内的活跃用户数
     */
    Integer getActiveUserCount(LocalDateTime startTime, LocalDateTime endTime);
}
