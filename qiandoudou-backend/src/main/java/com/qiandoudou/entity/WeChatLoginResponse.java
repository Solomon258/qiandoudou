package com.qiandoudou.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信登录响应实体
 */
@Data
public class WeChatLoginResponse {
    
    /**
     * 用户唯一标识
     */
    private String openid;
    
    /**
     * 会话密钥
     */
    @JsonProperty("session_key")
    private String sessionKey;
    
    /**
     * 用户在开放平台的唯一标识符，若当前小程序已绑定到微信开放平台帐号下会返回
     */
    private String unionid;
    
    /**
     * 错误码
     */
    private Integer errcode;
    
    /**
     * 错误信息
     */
    private String errmsg;
}
