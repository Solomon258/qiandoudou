package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户登录记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_login_logs")
public class UserLoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录类型：USERNAME(用户名)、PHONE(手机号)、WECHAT(微信)
     */
    private String loginType;

    /**
     * 登录账号（用户名/手机号/微信openid）
     */
    private String loginAccount;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 登录IP地址
     */
    private String ipAddress;

    /**
     * 用户代理信息（浏览器/设备信息）
     */
    private String userAgent;

    /**
     * 登录状态：0-失败，1-成功
     */
    private Integer loginStatus;

    /**
     * 登录失败原因
     */
    private String failureReason;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 登录类型枚举
     */
    public enum LoginType {
        USERNAME("USERNAME", "用户名登录"),
        PHONE("PHONE", "手机号登录"),
        WECHAT("WECHAT", "微信登录");

        private final String code;
        private final String desc;

        LoginType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    /**
     * 登录状态枚举
     */
    public enum LoginStatus {
        FAILURE(0, "登录失败"),
        SUCCESS(1, "登录成功");

        private final Integer code;
        private final String desc;

        LoginStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}
