package com.qiandoudou.controller;

import com.qiandoudou.entity.Script;
import com.qiandoudou.entity.ScriptChapter;
import com.qiandoudou.entity.UserScriptProgress;
import com.qiandoudou.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 剧本控制器
 */
@RestController
@RequestMapping("/scripts")
@Slf4j
public class ScriptController {

    @Autowired
    private ScriptService scriptService;

    /**
     * 获取剧本列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getScriptList(@RequestParam(required = false) Integer categoryId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Script> scripts = scriptService.getScriptList(categoryId);
            response.put("code", 200);
            response.put("data", scripts);
            response.put("message", "获取成功");
        } catch (Exception e) {
            log.error("获取剧本列表失败", e);
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 获取剧本详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getScriptDetail(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Script script = scriptService.getScriptDetail(id);
            response.put("code", 200);
            response.put("data", script);
            response.put("message", "获取成功");
        } catch (Exception e) {
            log.error("获取剧本详情失败", e);
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 获取剧本章节列表
     */
    @GetMapping("/{id}/chapters")
    public ResponseEntity<Map<String, Object>> getScriptChapters(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ScriptChapter> chapters = scriptService.getScriptChapters(id);
            response.put("code", 200);
            response.put("data", chapters);
            response.put("message", "获取成功");
        } catch (Exception e) {
            log.error("获取剧本章节失败", e);
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 获取指定章节内容
     */
    @GetMapping("/{scriptId}/chapters/{chapterNumber}")
    public ResponseEntity<Map<String, Object>> getChapterContent(
            @PathVariable Long scriptId, 
            @PathVariable Integer chapterNumber) {
        Map<String, Object> response = new HashMap<>();
        try {
            ScriptChapter chapter = scriptService.getChapterContent(scriptId, chapterNumber);
            response.put("code", 200);
            response.put("data", chapter);
            response.put("message", "获取成功");
        } catch (Exception e) {
            log.error("获取章节内容失败", e);
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户剧本进度（支持钱包维度）
     */
    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getUserProgress(
            @RequestParam Long userId, 
            @RequestParam Long scriptId,
            @RequestParam(required = false) Long walletId) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserScriptProgress progress;
            if (walletId != null) {
                // 获取指定钱包的进度
                progress = scriptService.getUserProgressByWallet(userId, walletId, scriptId);
            } else {
                // 兼容旧版本，不考虑钱包
                progress = scriptService.getUserProgress(userId, scriptId);
            }
            response.put("code", 200);
            response.put("data", progress);
            response.put("message", "获取成功");
        } catch (Exception e) {
            log.error("获取用户进度失败", e);
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 开始剧本
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startScript(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Long walletId = Long.valueOf(params.get("walletId").toString());
            Long scriptId = Long.valueOf(params.get("scriptId").toString());

            UserScriptProgress progress = scriptService.startScript(userId, walletId, scriptId);
            response.put("code", 200);
            response.put("data", progress);
            response.put("message", "开始剧本成功");
        } catch (Exception e) {
            log.error("开始剧本失败", e);
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 用户做出选择并解锁下一集
     */
    @PostMapping("/choice")
    public ResponseEntity<Map<String, Object>> makeChoice(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Long walletId = Long.valueOf(params.get("walletId").toString());
            Long scriptId = Long.valueOf(params.get("scriptId").toString());
            Integer currentChapter = Integer.valueOf(params.get("currentChapter").toString());
            String selectedChoice = params.get("selectedChoice").toString();
            BigDecimal amount = new BigDecimal(params.get("amount").toString());

            Map<String, Object> result = scriptService.makeChoiceAndUnlock(userId, walletId, scriptId, currentChapter, selectedChoice, amount);
            response.put("code", result.get("success").equals(true) ? 200 : 500);
            response.putAll(result);
        } catch (Exception e) {
            log.error("处理用户选择失败", e);
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户在指定钱包的剧本进度
     */
    @GetMapping("/wallet-progress")
    public ResponseEntity<Map<String, Object>> getWalletScriptProgress(
            @RequestParam Long userId, 
            @RequestParam Long walletId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> progressList = scriptService.getUserScriptProgressByWallet(userId, walletId);
            response.put("code", 200);
            response.put("data", progressList);
            response.put("message", "获取成功");
        } catch (Exception e) {
            log.error("获取钱包剧本进度失败", e);
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}
