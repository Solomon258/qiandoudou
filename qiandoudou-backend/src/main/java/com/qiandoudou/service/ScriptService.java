package com.qiandoudou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiandoudou.entity.Script;
import com.qiandoudou.entity.ScriptChapter;
import com.qiandoudou.entity.UserScriptProgress;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 剧本服务接口
 */
public interface ScriptService extends IService<Script> {

    /**
     * 获取剧本列表
     */
    List<Script> getScriptList(Integer categoryId);

    /**
     * 获取剧本详情
     */
    Script getScriptDetail(Long scriptId);

    /**
     * 获取剧本章节列表
     */
    List<ScriptChapter> getScriptChapters(Long scriptId);

    /**
     * 获取指定章节内容
     */
    ScriptChapter getChapterContent(Long scriptId, Integer chapterNumber);

    /**
     * 获取用户剧本进度（不考虑钱包，兼容性方法）
     */
    UserScriptProgress getUserProgress(Long userId, Long scriptId);

    /**
     * 获取用户在指定钱包的剧本进度
     */
    UserScriptProgress getUserProgressByWallet(Long userId, Long walletId, Long scriptId);

    /**
     * 开始剧本
     */
    UserScriptProgress startScript(Long userId, Long walletId, Long scriptId);

    /**
     * 用户做出选择并解锁下一集
     */
    Map<String, Object> makeChoiceAndUnlock(Long userId, Long walletId, Long scriptId, Integer currentChapter, 
                                           String selectedChoice, BigDecimal amount);

    /**
     * 获取用户在指定钱包的所有剧本进度
     */
    List<Map<String, Object>> getUserScriptProgressByWallet(Long userId, Long walletId);
}
