package com.qiandoudou.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.miniprogram")
public class WeChatConfig {
    
    /**
     * 小程序AppID
     */
    private String appid;
    
    /**
     * 小程序Secret
     */
    private String secret;
    
    /**
     * 微信登录API地址
     */
    private String authUrl = "https://api.weixin.qq.com/sns/jscode2session";
    
    /**
     * 开发模式：true=使用演示模式跳过IP白名单限制，false=使用真实微信API
     */
    private boolean devMode = false;
}
