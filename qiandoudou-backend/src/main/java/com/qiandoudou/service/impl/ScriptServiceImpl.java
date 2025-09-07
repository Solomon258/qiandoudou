package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiandoudou.entity.Script;
import com.qiandoudou.entity.ScriptChapter;
import com.qiandoudou.entity.UserScriptProgress;
import com.qiandoudou.mapper.ScriptChapterMapper;
import com.qiandoudou.mapper.ScriptMapper;
import com.qiandoudou.mapper.UserScriptProgressMapper;
import com.qiandoudou.service.ScriptService;
import com.qiandoudou.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 剧本服务实现类
 */
@Service
@Slf4j
public class ScriptServiceImpl extends ServiceImpl<ScriptMapper, Script> implements ScriptService {

    @Autowired
    private ScriptChapterMapper scriptChapterMapper;

    @Autowired
    private UserScriptProgressMapper userScriptProgressMapper;

    @Autowired
    private WalletService walletService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Script> getScriptList(Integer categoryId) {
        if (categoryId == null || categoryId == 1) {
            // 获取推荐剧本（全部）
            return baseMapper.getRecommendedScripts();
        } else {
            // 获取特定分类剧本
            return baseMapper.getScriptsByCategory(categoryId);
        }
    }

    @Override
    public Script getScriptDetail(Long scriptId) {
        return getById(scriptId);
    }

    @Override
    public List<ScriptChapter> getScriptChapters(Long scriptId) {
        return scriptChapterMapper.getChaptersByScriptId(scriptId);
    }

    @Override
    public ScriptChapter getChapterContent(Long scriptId, Integer chapterNumber) {
        return scriptChapterMapper.getChapterByScriptIdAndNumber(scriptId, chapterNumber);
    }

    @Override
    public UserScriptProgress getUserProgress(Long userId, Long scriptId) {
        return userScriptProgressMapper.getProgressByUserIdAndScriptId(userId, scriptId);
    }

    @Override
    public UserScriptProgress getUserProgressByWallet(Long userId, Long walletId, Long scriptId) {
        return userScriptProgressMapper.getProgressByUserIdWalletIdAndScriptId(userId, walletId, scriptId);
    }

    @Override
    @Transactional
    public UserScriptProgress startScript(Long userId, Long walletId, Long scriptId) {
        // 检查是否已经在这个钱包中开始过这个剧本
        UserScriptProgress existingProgress = getUserProgressByWallet(userId, walletId, scriptId);
        if (existingProgress != null) {
            return existingProgress;
        }

        // 创建新的进度记录
        UserScriptProgress progress = new UserScriptProgress();
        progress.setUserId(userId);
        progress.setWalletId(walletId);
        progress.setScriptId(scriptId);
        progress.setCurrentChapter(1);
        progress.setTotalPaid(BigDecimal.ZERO);
        progress.setChoicesMade("[]");
        progress.setStatus(1); // 进行中
        progress.setStartTime(LocalDateTime.now());

        userScriptProgressMapper.insert(progress);
        return progress;
    }

    @Override
    @Transactional
    public Map<String, Object> makeChoiceAndUnlock(Long userId, Long walletId, Long scriptId, Integer currentChapter, 
                                                  String selectedChoice, BigDecimal amount) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取用户在指定钱包的进度
            UserScriptProgress progress = getUserProgressByWallet(userId, walletId, scriptId);
            if (progress == null) {
                throw new RuntimeException("未找到剧本进度记录");
            }

            // 获取当前章节
            ScriptChapter currentChapterObj = getChapterContent(scriptId, currentChapter);
            if (currentChapterObj == null) {
                throw new RuntimeException("章节不存在");
            }

            // 解析选择项，验证选择和金额
            List<Map<String, Object>> choices = objectMapper.readValue(
                currentChapterObj.getChoices(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );

            Map<String, Object> selectedChoiceObj = null;
            for (Map<String, Object> choice : choices) {
                if (selectedChoice.equals(choice.get("selection"))) {
                    selectedChoiceObj = choice;
                    break;
                }
            }

            if (selectedChoiceObj == null) {
                throw new RuntimeException("无效的选择");
            }

            // 验证金额
            BigDecimal requiredAmount = new BigDecimal(selectedChoiceObj.get("cost").toString());
            if (amount.compareTo(requiredAmount) != 0) {
                throw new RuntimeException("攒钱金额不正确");
            }

            // 执行攒钱到钱包
            walletService.scriptSaving(progress.getWalletId(), amount, 
                "剧本攒钱：" + currentChapterObj.getTitle() + " - " + selectedChoice, 
                currentChapterObj.getId());

            // 更新用户进度
            List<Map<String, Object>> choicesMade = objectMapper.readValue(
                progress.getChoicesMade(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );

            Map<String, Object> newChoice = new HashMap<>();
            newChoice.put("chapter", currentChapter);
            newChoice.put("choice", selectedChoice);
            newChoice.put("amount", amount);
            newChoice.put("timestamp", LocalDateTime.now().toString());
            choicesMade.add(newChoice);

            // 计算下一章节 - 根据nextId（章节号）跳转到指定视频
            String nextIdValue = selectedChoiceObj.get("nextId") == null ? null : selectedChoiceObj.get("nextId").toString();
            Integer nextChapter = null;
            Long nextChapterDbId = null;
            
            if (nextIdValue != null) {
                // 根据nextId（章节号）查找对应的章节
                try {
                    Integer nextChapterNumber = Integer.parseInt(nextIdValue);
                    ScriptChapter nextChapterObj = scriptChapterMapper.getChapterByScriptIdAndNumber(scriptId, nextChapterNumber);
                    if (nextChapterObj != null) {
                        nextChapter = nextChapterObj.getChapterNumber();
                        nextChapterDbId = nextChapterObj.getId(); // 获取章节的数据库ID
                    }
                } catch (NumberFormatException e) {
                    log.warn("nextId格式错误: {}", nextIdValue);
                }
            }
            
            // 更新进度
            if (nextChapter != null) {
                progress.setCurrentChapter(nextChapter);
            }
            progress.setTotalPaid(progress.getTotalPaid().add(amount));
            progress.setChoicesMade(objectMapper.writeValueAsString(choicesMade));

            // 判断是否完成剧本：只有当nextId为null时才表示剧本结束
            boolean isCompleted = (nextIdValue == null);
            if (isCompleted) {
                progress.setStatus(2); // 已完成
                progress.setCompleteTime(LocalDateTime.now());
            }

            userScriptProgressMapper.updateById(progress);

            result.put("success", true);
            result.put("message", isCompleted ? "恭喜完成剧本！" : "选择成功，已解锁下一集");
            result.put("nextChapter", nextChapter);
            result.put("nextChapterId", nextChapterDbId != null ? nextChapterDbId.toString() : null);
            result.put("isCompleted", isCompleted);
            result.put("totalPaid", progress.getTotalPaid());

        } catch (Exception e) {
            log.error("用户选择处理失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getUserScriptProgressByWallet(Long userId, Long walletId) {
        List<UserScriptProgress> progressList = userScriptProgressMapper.getProgressByUserIdAndWalletId(userId, walletId);
        
        return progressList.stream().map(progress -> {
            Map<String, Object> item = new HashMap<>();
            Script script = getById(progress.getScriptId());
            
            item.put("scriptId", script.getId());
            item.put("title", script.getTitle());
            item.put("coverImage", script.getCoverImage());
            item.put("currentChapter", progress.getCurrentChapter());
            item.put("totalChapters", script.getTotalChapters());
            item.put("totalPaid", progress.getTotalPaid());
            item.put("targetAmount", script.getTargetAmount());
            item.put("status", progress.getStatus());
            item.put("startTime", progress.getStartTime());
            
            return item;
        }).collect(Collectors.toList());
    }
}
