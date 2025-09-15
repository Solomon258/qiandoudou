package com.qiandoudou.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiandoudou.common.Result;
import com.qiandoudou.service.UserLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录记录控制器
 */
@RestController
@RequestMapping("/login-log")
public class LoginLogController {

    @Autowired
    private UserLoginLogService userLoginLogService;

    /**
     * 分页查询登录记录
     */
    @GetMapping("/page")
    public Result<IPage<Map<String, Object>>> getLoginLogPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) String loginAccount,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Integer loginStatus) {
        try {
            IPage<Map<String, Object>> page = userLoginLogService.getLoginLogPage(
                    pageNum, pageSize, userId, loginType, loginAccount, startTime, endTime, loginStatus);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户最近的登录记录
     */
    @GetMapping("/recent/{userId}")
    public Result<List<Map<String, Object>>> getRecentLoginLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Map<String, Object>> logs = userLoginLogService.getRecentLoginLogs(userId, limit);
            return Result.success("查询成功", logs);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取登录统计数据
     */
    @GetMapping("/statistics")
    public Result<List<Map<String, Object>>> getLoginStatistics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            List<Map<String, Object>> statistics = userLoginLogService.getLoginStatistics(userId, startTime, endTime);
            return Result.success("查询成功", statistics);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日登录用户数
     */
    @GetMapping("/today-count")
    public Result<Integer> getTodayLoginUserCount() {
        try {
            Integer count = userLoginLogService.getTodayLoginUserCount();
            return Result.success("查询成功", count);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定时间段内的活跃用户数
     */
    @GetMapping("/active-count")
    public Result<Integer> getActiveUserCount(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            Integer count = userLoginLogService.getActiveUserCount(startTime, endTime);
            return Result.success("查询成功", count);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取登录数据概览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getLoginOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 今日登录用户数
            Integer todayCount = userLoginLogService.getTodayLoginUserCount();
            overview.put("todayLoginCount", todayCount);
            
            // 最近7天活跃用户数
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            LocalDateTime now = LocalDateTime.now();
            Integer weeklyActiveCount = userLoginLogService.getActiveUserCount(sevenDaysAgo, now);
            overview.put("weeklyActiveCount", weeklyActiveCount);
            
            // 最近30天活跃用户数
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            Integer monthlyActiveCount = userLoginLogService.getActiveUserCount(thirtyDaysAgo, now);
            overview.put("monthlyActiveCount", monthlyActiveCount);
            
            // 登录方式统计（最近30天）
            List<Map<String, Object>> loginTypeStats = userLoginLogService.getLoginStatistics(null, thirtyDaysAgo, now);
            overview.put("loginTypeStatistics", loginTypeStats);
            
            return Result.success("查询成功", overview);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户个人登录历史
     */
    @GetMapping("/user-history/{userId}")
    public Result<Map<String, Object>> getUserLoginHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 分页查询用户登录记录
            IPage<Map<String, Object>> page = userLoginLogService.getLoginLogPage(
                    pageNum, pageSize, userId, null, null, null, null, null);
            result.put("loginLogs", page);
            
            // 用户登录统计
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> statistics = userLoginLogService.getLoginStatistics(userId, thirtyDaysAgo, now);
            result.put("statistics", statistics);
            
            // 最近登录记录
            List<Map<String, Object>> recentLogs = userLoginLogService.getRecentLoginLogs(userId, 5);
            result.put("recentLogs", recentLogs);
            
            return Result.success("查询成功", result);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
