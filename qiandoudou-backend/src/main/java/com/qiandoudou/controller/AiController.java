package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.TtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI服务控制器
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private TtsService ttsService;

    /**
     * 生成AI文案
     */
    @PostMapping("/generate-text")
    public Result<String> generateText(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                return Result.error("提示词不能为空");
            }
            
            String result = aiService.generateAiText(prompt);
            return Result.success("文案生成成功", result);
        } catch (Exception e) {
            return Result.error("文案生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成AI伴侣评论
     */
    @PostMapping("/generate-partner-comment")
    public Result<String> generatePartnerComment(@RequestBody Map<String, Object> request) {
        try {
            Long partnerId = Long.valueOf(request.get("partnerId").toString());
            String postContent = request.get("postContent").toString();
            
            String result = aiService.generatePartnerComment(partnerId, postContent);
            return Result.success("评论生成成功", result);
        } catch (Exception e) {
            return Result.error("评论生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成语音文件
     */
    @PostMapping("/generate-voice")
    public Result<String> generateVoice(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String voiceType = request.getOrDefault("voiceType", "奇妙栩");
            
            if (text == null || text.trim().isEmpty()) {
                return Result.error("文本不能为空");
            }
            
            String voiceUrl = ttsService.generateVoiceAndUpload(text, voiceType);
            return Result.success("语音生成成功", voiceUrl);
        } catch (Exception e) {
            return Result.error("语音生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成AI伴侣语音
     */
    @PostMapping("/generate-partner-voice")
    public Result<String> generatePartnerVoice(@RequestBody Map<String, Object> request) {
        try {
            Long partnerId = Long.valueOf(request.get("partnerId").toString());
            String text = request.get("text").toString();
            
            String voiceUrl = aiService.generatePartnerVoice(partnerId, text);
            if (voiceUrl == null) {
                return Result.error("语音生成失败");
            }
            
            return Result.success("语音生成成功", voiceUrl);
        } catch (Exception e) {
            return Result.error("语音生成失败: " + e.getMessage());
        }
    }
}
