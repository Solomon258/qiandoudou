package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.UserScriptProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户剧本进度Mapper接口
 */
@Mapper
public interface UserScriptProgressMapper extends BaseMapper<UserScriptProgress> {

    /**
     * 根据用户ID获取所有剧本进度
     */
    @Select("SELECT * FROM user_script_progress WHERE user_id = #{userId} AND deleted = 0")
    List<UserScriptProgress> getProgressByUserId(Long userId);

    /**
     * 根据用户ID和剧本ID获取进度
     */
    @Select("SELECT * FROM user_script_progress WHERE user_id = #{userId} AND script_id = #{scriptId} AND deleted = 0")
    UserScriptProgress getProgressByUserIdAndScriptId(Long userId, Long scriptId);

    /**
     * 根据用户ID和钱包ID获取进度
     */
    @Select("SELECT * FROM user_script_progress WHERE user_id = #{userId} AND wallet_id = #{walletId} AND deleted = 0")
    List<UserScriptProgress> getProgressByUserIdAndWalletId(Long userId, Long walletId);

    /**
     * 根据用户ID、钱包ID和剧本ID获取进度
     */
    @Select("SELECT * FROM user_script_progress WHERE user_id = #{userId} AND wallet_id = #{walletId} AND script_id = #{scriptId} AND deleted = 0")
    UserScriptProgress getProgressByUserIdWalletIdAndScriptId(Long userId, Long walletId, Long scriptId);
}
