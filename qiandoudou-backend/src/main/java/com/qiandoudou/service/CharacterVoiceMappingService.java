package com.qiandoudou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiandoudou.entity.CharacterVoiceMapping;

/**
 * 人物声音映射配置服务接口
 */
public interface CharacterVoiceMappingService extends IService<CharacterVoiceMapping> {

    /**
     * 根据人物名称获取声音配置
     * @param characterName 人物名称
     * @return 声音配置信息
     */
    CharacterVoiceMapping getByCharacterName(String characterName);

    /**
     * 根据人物名称获取原TTS接口A的声音路径
     * @param characterName 人物名称
     * @return 声音路径，如果未找到则返回默认路径
     */
    String getOriginalTtsVoicePathByCharacterName(String characterName);

    /**
     * 根据人物名称获取原TTS接口A的人物名称
     * @param characterName 人物名称
     * @return 原TTS接口A的人物名称，如果未找到则返回默认名称
     */
    String getOriginalTtsCharacterNameByCharacterName(String characterName);

    /**
     * 根据人物名称获取火山引擎音色类型
     * @param characterName 人物名称
     * @return 火山引擎音色类型，如果未找到则返回默认音色
     */
    String getVolcengineVoiceTypeByCharacterName(String characterName);

    /**
     * 根据人物名称获取火山引擎人物名称
     * @param characterName 人物名称
     * @return 火山引擎人物名称，如果未找到则返回默认名称
     */
    String getVolcengineCharacterNameByCharacterName(String characterName);

    // 为了兼容性，保留旧的方法名（委托给新方法）
    /**
     * @deprecated 使用 getOriginalTtsVoicePathByCharacterName 替代
     */
    @Deprecated
    default String getVoicePathByCharacterName(String characterName) {
        return getOriginalTtsVoicePathByCharacterName(characterName);
    }

    /**
     * @deprecated 使用 getOriginalTtsCharacterNameByCharacterName 替代
     */
    @Deprecated
    default String getVoiceTypeByCharacterName(String characterName) {
        return getOriginalTtsCharacterNameByCharacterName(characterName);
    }
}
