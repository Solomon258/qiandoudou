package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.service.TtsService;
import com.qiandoudou.service.VolcengineTtsService;
import com.qiandoudou.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 测试控制器 - 用于测试AI伴侣转账功能
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private TtsService ttsService;

    @Autowired
    private VolcengineTtsService volcengineTtsService;

    /**
     * 测试AI伴侣转账功能
     */
    @PostMapping("/ai-partner-transfer")
    public Result<String> testAiPartnerTransfer(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            Long aiPartnerId = Long.valueOf(request.get("aiPartnerId").toString());
            BigDecimal amount = new BigDecimal("0.01"); // 固定0.01元
            String message = "测试转账消息";
            String aiPartnerName = request.get("aiPartnerName").toString();
            String aiPartnerAvatar = (String) request.get("aiPartnerAvatar");

            // 为测试添加默认的人物名称参数
            String characterName = request.get("characterName") != null ? 
                request.get("characterName").toString() : aiPartnerName;
            walletService.aiPartnerTransfer(walletId, aiPartnerId, amount, message, aiPartnerName, aiPartnerAvatar, characterName);
            return Result.success("AI伴侣转账测试成功");
        } catch (Exception e) {
            return Result.error("AI伴侣转账测试失败: " + e.getMessage());
        }
    }

    /**
     * 直接测试TTS服务
     */
    @PostMapping("/tts")
    public Result<String> testTts(@RequestBody Map<String, Object> request) {
        try {
            String text = request.get("text").toString();
            String characterName = request.get("characterName").toString();
            
            // 直接调用TTS服务生成语音
            String voiceUrl = ttsService.generateVoiceByCharacterName(text, characterName);
            return Result.success("TTS测试成功，语音URL: " + voiceUrl);
        } catch (Exception e) {
            return Result.error("TTS测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试火山引擎TTS服务状态
     */
    @GetMapping("/volcengine-tts-status")
    public Result<Map<String, Object>> testVolcengineTtsStatus() {
        try {
            Map<String, Object> status = new java.util.HashMap<>();
            status.put("isAvailable", volcengineTtsService.isAvailable());
            status.put("message", volcengineTtsService.isAvailable() ? "火山引擎TTS服务可用" : "火山引擎TTS服务不可用");
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("检查火山引擎TTS状态失败: " + e.getMessage());
        }
    }

    /**
     * 直接测试火山引擎TTS
     */
    @PostMapping("/volcengine-tts")
    public Result<String> testVolcengineTts(@RequestBody Map<String, Object> request) {
        try {
            String text = request.get("text").toString();
            String voiceType = request.get("voiceType").toString();
            
            // 直接调用火山引擎TTS服务
            String voiceUrl = volcengineTtsService.generateVoiceAndUpload(text, voiceType);
            return Result.success("火山引擎TTS测试成功，语音URL: " + voiceUrl);
        } catch (Exception e) {
            return Result.error("火山引擎TTS测试失败: " + e.getMessage());
        }
    }
}
