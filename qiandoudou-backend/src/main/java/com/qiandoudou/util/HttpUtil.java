package com.qiandoudou.util;

import javax.servlet.http.HttpServletRequest;

/**
 * HTTP工具类
 */
public class HttpUtil {

    /**
     * 获取客户端真实IP地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果通过多级反向代理，可能会有多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 获取用户代理信息
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }

    /**
     * 简化用户代理信息（提取主要信息）
     */
    public static String getSimplifiedUserAgent(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if ("unknown".equals(userAgent)) {
            return userAgent;
        }
        
        // 限制长度，避免存储过长的用户代理信息
        if (userAgent.length() > 500) {
            userAgent = userAgent.substring(0, 500) + "...";
        }
        
        return userAgent;
    }
}
