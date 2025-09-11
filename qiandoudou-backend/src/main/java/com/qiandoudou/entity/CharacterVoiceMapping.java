package com.qiandoudou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 人物声音映射配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("character_voice_mapping")
public class CharacterVoiceMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 前端传入的人物名称（如：黎初言）
     */
    private String characterName;

    /**
     * 原TTS接口A中对应的人物名称（如：奇妙栩）
     */
    private String originalTtsCharacterName;

    /**
     * 原TTS接口A的声音文件路径
     */
    private String originalTtsVoicePath;

    /**
     * 火山引擎中对应的人物名称（如：婉婉小鹤）
     */
    private String volcengineCharacterName;

    /**
     * 火山引擎音色类型（如：zh_female_wanwanxiaohe_moon_bigtts）
     */
    private String volcengineVoiceType;

    /**
     * 性别：1-男，2-女
     */
    private Integer gender;

    /**
     * 音色风格（温柔、甜美、成熟、活泼等）
     */
    private String voiceStyle;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-否，1-是
     */
    @TableLogic
    private Integer deleted;
}
