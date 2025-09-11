package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.CharacterVoiceMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 人物声音映射配置Mapper接口
 */
@Mapper
public interface CharacterVoiceMappingMapper extends BaseMapper<CharacterVoiceMapping> {

    /**
     * 根据人物名称获取声音配置
     * @param characterName 人物名称
     * @return 声音配置信息
     */
    CharacterVoiceMapping getByCharacterName(@Param("characterName") String characterName);
}
