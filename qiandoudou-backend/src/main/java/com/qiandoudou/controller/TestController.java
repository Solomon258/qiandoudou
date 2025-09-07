package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
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

            walletService.aiPartnerTransfer(walletId, aiPartnerId, amount, message, aiPartnerName, aiPartnerAvatar);
            return Result.success("AI伴侣转账测试成功");
        } catch (Exception e) {
            return Result.error("AI伴侣转账测试失败: " + e.getMessage());
        }
    }
}
