package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiandoudou.entity.UserLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户登录记录Mapper接口
 */
@Mapper
public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {

    /**
     * 分页查询用户登录记录
     */
    @Select("<script>" +
            "SELECT l.*, u.nickname, u.username, u.phone, " +
            "CASE " +
            "  WHEN l.login_type = 'USERNAME' THEN CONCAT('用户名: ', l.login_account) " +
            "  WHEN l.login_type = 'PHONE' THEN CONCAT('手机号: ', l.login_account) " +
            "  WHEN l.login_type = 'WECHAT' THEN CONCAT('微信用户: ', COALESCE(u.nickname, '未知用户')) " +
            "  ELSE l.login_account " +
            "END AS display_account, " +
            "l.login_account as original_account " +
            "FROM user_login_logs l " +
            "LEFT JOIN users u ON l.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='userId != null'> AND l.user_id = #{userId} </if>" +
            "<if test='loginType != null and loginType != \"\"'> AND l.login_type = #{loginType} </if>" +
            "<if test='loginAccount != null and loginAccount != \"\"'> AND (l.login_account LIKE CONCAT('%', #{loginAccount}, '%') OR u.nickname LIKE CONCAT('%', #{loginAccount}, '%') OR u.username LIKE CONCAT('%', #{loginAccount}, '%') OR u.phone LIKE CONCAT('%', #{loginAccount}, '%')) </if>" +
            "<if test='startTime != null'> AND l.login_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND l.login_time &lt;= #{endTime} </if>" +
            "<if test='loginStatus != null'> AND l.login_status = #{loginStatus} </if>" +
            "ORDER BY l.login_time DESC" +
            "</script>")
    IPage<Map<String, Object>> selectLoginLogPage(
            Page<Map<String, Object>> page,
            @Param("userId") Long userId,
            @Param("loginType") String loginType,
            @Param("loginAccount") String loginAccount,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("loginStatus") Integer loginStatus
    );

    /**
     * 查询用户最近的登录记录
     */
    @Select("SELECT l.*, u.nickname, u.username, u.phone, " +
            "CASE " +
            "  WHEN l.login_type = 'USERNAME' THEN CONCAT('用户名: ', l.login_account) " +
            "  WHEN l.login_type = 'PHONE' THEN CONCAT('手机号: ', l.login_account) " +
            "  WHEN l.login_type = 'WECHAT' THEN CONCAT('微信用户: ', COALESCE(u.nickname, '未知用户')) " +
            "  ELSE l.login_account " +
            "END AS display_account, " +
            "l.login_account as original_account " +
            "FROM user_login_logs l " +
            "LEFT JOIN users u ON l.user_id = u.id " +
            "WHERE l.user_id = #{userId} AND l.login_status = 1 " +
            "ORDER BY l.login_time DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecentLoginLogs(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 统计用户登录次数
     */
    @Select("<script>" +
            "SELECT " +
            "login_type, " +
            "COUNT(*) as login_count, " +
            "COUNT(CASE WHEN login_status = 1 THEN 1 END) as success_count, " +
            "COUNT(CASE WHEN login_status = 0 THEN 1 END) as failure_count " +
            "FROM user_login_logs " +
            "WHERE 1=1 " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='startTime != null'> AND login_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND login_time &lt;= #{endTime} </if>" +
            "GROUP BY login_type" +
            "</script>")
    List<Map<String, Object>> selectLoginStatistics(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询今日登录用户数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_login_logs " +
            "WHERE DATE(login_time) = CURDATE() AND login_status = 1")
    Integer selectTodayLoginUserCount();

    /**
     * 查询指定时间段内的活跃用户数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_login_logs " +
            "WHERE login_time >= #{startTime} AND login_time <= #{endTime} AND login_status = 1")
    Integer selectActiveUserCount(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
