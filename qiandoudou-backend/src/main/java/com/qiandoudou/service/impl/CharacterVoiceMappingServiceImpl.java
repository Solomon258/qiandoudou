package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiandoudou.entity.CharacterVoiceMapping;
import com.qiandoudou.mapper.CharacterVoiceMappingMapper;
import com.qiandoudou.service.CharacterVoiceMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 人物声音映射配置服务实现类
 */
@Service
public class CharacterVoiceMappingServiceImpl extends ServiceImpl<CharacterVoiceMappingMapper, CharacterVoiceMapping> implements CharacterVoiceMappingService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterVoiceMappingServiceImpl.class);

    // 默认声音配置
    private static final String DEFAULT_ORIGINAL_TTS_VOICE_PATH = "wav/peiyin/配音女-奇妙栩/1.wav";
    private static final String DEFAULT_ORIGINAL_TTS_CHARACTER_NAME = "奇妙栩";
    private static final String DEFAULT_VOLCENGINE_CHARACTER_NAME = "婉婉小鹤";
    private static final String DEFAULT_VOLCENGINE_VOICE_TYPE = "zh_female_wanwanxiaohe_moon_bigtts";

    @Override
    public CharacterVoiceMapping getByCharacterName(String characterName) {
        if (characterName == null || characterName.trim().isEmpty()) {
            logger.warn("人物名称为空，返回null");
            return null;
        }
        
        try {
            CharacterVoiceMapping mapping = baseMapper.getByCharacterName(characterName.trim());
            logger.info("查询人物声音配置，人物名称: {}, 结果: {}", characterName, mapping != null ? "找到" : "未找到");
            return mapping;
        } catch (Exception e) {
            logger.error("查询人物声音配置失败，人物名称: {}", characterName, e);
            return null;
        }
    }

    @Override
    public String getOriginalTtsVoicePathByCharacterName(String characterName) {
        CharacterVoiceMapping mapping = getByCharacterName(characterName);
        if (mapping != null && mapping.getOriginalTtsVoicePath() != null) {
            logger.info("找到原TTS声音路径，人物名称: {}, 路径: {}", characterName, mapping.getOriginalTtsVoicePath());
            return mapping.getOriginalTtsVoicePath();
        }
        
        logger.info("未找到原TTS声音路径，使用默认路径，人物名称: {}, 默认路径: {}", characterName, DEFAULT_ORIGINAL_TTS_VOICE_PATH);
        return DEFAULT_ORIGINAL_TTS_VOICE_PATH;
    }

    @Override
    public String getOriginalTtsCharacterNameByCharacterName(String characterName) {
        CharacterVoiceMapping mapping = getByCharacterName(characterName);
        if (mapping != null && mapping.getOriginalTtsCharacterName() != null) {
            logger.info("找到原TTS人物名称，前端人名: {}, 原TTS人名: {}", characterName, mapping.getOriginalTtsCharacterName());
            return mapping.getOriginalTtsCharacterName();
        }
        
        logger.info("未找到原TTS人物名称，使用默认名称，前端人名: {}, 默认名称: {}", characterName, DEFAULT_ORIGINAL_TTS_CHARACTER_NAME);
        return DEFAULT_ORIGINAL_TTS_CHARACTER_NAME;
    }

    @Override
    public String getVolcengineVoiceTypeByCharacterName(String characterName) {
        CharacterVoiceMapping mapping = getByCharacterName(characterName);
        if (mapping != null && mapping.getVolcengineVoiceType() != null) {
            logger.info("找到火山引擎音色类型，前端人名: {}, 火山引擎音色: {}", characterName, mapping.getVolcengineVoiceType());
            return mapping.getVolcengineVoiceType();
        }
        
        logger.info("未找到火山引擎音色类型，使用默认类型，前端人名: {}, 默认音色: {}", characterName, DEFAULT_VOLCENGINE_VOICE_TYPE);
        return DEFAULT_VOLCENGINE_VOICE_TYPE;
    }

    @Override
    public String getVolcengineCharacterNameByCharacterName(String characterName) {
        CharacterVoiceMapping mapping = getByCharacterName(characterName);
        if (mapping != null && mapping.getVolcengineCharacterName() != null) {
            logger.info("找到火山引擎人物名称，前端人名: {}, 火山引擎人名: {}", characterName, mapping.getVolcengineCharacterName());
            return mapping.getVolcengineCharacterName();
        }
        
        logger.info("未找到火山引擎人物名称，使用默认名称，前端人名: {}, 默认名称: {}", characterName, DEFAULT_VOLCENGINE_CHARACTER_NAME);
        return DEFAULT_VOLCENGINE_CHARACTER_NAME;
    }
}
