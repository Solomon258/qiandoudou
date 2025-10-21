package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.entity.DreamItem;
import com.qiandoudou.entity.DreamReward;
import com.qiandoudou.entity.Wallet;
import com.qiandoudou.service.DreamWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 梦想钱包控制器
 */
@Slf4j
@RestController
@RequestMapping("/dream-wallet")
public class DreamWalletController {

    @Autowired
    private DreamWalletService dreamWalletService;

    /**
     * 获取梦想商品列表
     */
    @GetMapping("/items")
    public Result<List<DreamItem>> getDreamItems(@RequestParam Integer category) {
        try {
            List<DreamItem> items = dreamWalletService.getDreamItems(category);
            return Result.success("获取成功", items);
        } catch (Exception e) {
            log.error("获取梦想商品列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索梦想商品
     */
    @GetMapping("/items/search")
    public Result<List<DreamItem>> searchDreamItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer category) {
        try {
            List<DreamItem> items = dreamWalletService.searchDreamItems(keyword, category);
            return Result.success("搜索成功", items);
        } catch (Exception e) {
            log.error("搜索梦想商品失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建梦想钱包
     */
    @PostMapping("/create")
    public Result<Wallet> createDreamWallet(@RequestBody Map<String, Object> request) {
        log.info("==================== 收到创建梦想钱包请求 ====================");
        log.info("请求参数: {}", request);

        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Integer dreamType = Integer.valueOf(request.get("dreamType").toString());
            String walletName = request.get("walletName").toString();
            BigDecimal targetAmount = new BigDecimal(request.get("targetAmount").toString());
            Long itemId = Long.valueOf(request.get("itemId").toString());
            Integer days = request.get("days") != null ?
                Integer.valueOf(request.get("days").toString()) : null;

            log.info("解析后参数 - userId: {}, dreamType: {}, walletName: {}, targetAmount: {}, itemId: {}, days: {}",
                userId, dreamType, walletName, targetAmount, itemId, days);

            log.info("准备调用 dreamWalletService.createDreamWallet()");
            Wallet wallet = dreamWalletService.createDreamWallet(
                userId, dreamType, walletName, targetAmount, itemId, days);

            log.info("钱包创建成功，钱包ID: {}", wallet.getId());
            log.info("==================== 创建梦想钱包请求完成 ====================");

            return Result.success("梦想钱包创建成功", wallet);
        } catch (Exception e) {
            log.error("==================== 创建梦想钱包失败 ====================");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常消息: {}", e.getMessage());
            log.error("完整堆栈:", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取梦想钱包详情
     */
    @GetMapping("/detail/{walletId}")
    public Result<Map<String, Object>> getDreamWalletDetail(@PathVariable Long walletId) {
        try {
            Map<String, Object> detail = dreamWalletService.getDreamWalletDetail(walletId);
            return Result.success("获取成功", detail);
        } catch (Exception e) {
            log.error("获取梦想钱包详情失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取梦想钱包奖励列表
     */
    @GetMapping("/rewards/{walletId}")
    public Result<List<DreamReward>> getDreamRewards(@PathVariable Long walletId) {
        try {
            List<DreamReward> rewards = dreamWalletService.getDreamRewards(walletId);
            return Result.success("获取成功", rewards);
        } catch (Exception e) {
            log.error("获取梦想奖励失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成旅行行程
     */
    @PostMapping("/generate-itinerary")
    public Result<String> generateTravelItinerary(@RequestBody Map<String, Object> request) {
        try {
            String destination = request.get("destination").toString();
            Integer days = Integer.valueOf(request.get("days").toString());
            BigDecimal budget = new BigDecimal(request.get("budget").toString());

            String itinerary = dreamWalletService.generateTravelItinerary(destination, days, budget);
            return Result.success("生成成功", itinerary);
        } catch (Exception e) {
            log.error("生成旅行行程失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成分享图片
     */
    @PostMapping("/generate-share")
    public Result<String> generateShareImage(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            Integer progress = Integer.valueOf(request.get("progress").toString());

            String imageUrl = dreamWalletService.generateShareImage(walletId, progress);
            return Result.success("生成成功", imageUrl);
        } catch (Exception e) {
            log.error("生成分享图片失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 记录分享行为
     */
    @PostMapping("/record-share")
    public Result<String> recordShare(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            Long userId = Long.valueOf(request.get("userId").toString());
            Integer shareType = Integer.valueOf(request.get("shareType").toString());
            Integer progress = Integer.valueOf(request.get("progress").toString());
            String imageUrl = (String) request.get("imageUrl");
            String shareText = (String) request.get("shareText");

            dreamWalletService.recordShare(walletId, userId, shareType, progress, imageUrl, shareText);
            return Result.success("记录成功");
        } catch (Exception e) {
            log.error("记录分享失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手动更新梦想进度（用于测试）
     */
    @PostMapping("/update-progress")
    public Result<String> updateProgress(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            BigDecimal newBalance = new BigDecimal(request.get("newBalance").toString());
            BigDecimal triggerAmount = new BigDecimal(request.get("triggerAmount").toString());
            Integer triggerType = Integer.valueOf(request.get("triggerType").toString());

            boolean reachedMilestone = dreamWalletService.updateDreamProgress(
                walletId, newBalance, triggerAmount, triggerType);
            
            String message = reachedMilestone ? "进度更新成功，达到新里程碑！" : "进度更新成功";
            return Result.success(message);
        } catch (Exception e) {
            log.error("更新梦想进度失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取梦想钱包交易记录
     */
    @GetMapping("/records/{walletId}")
    public Result<Map<String, Object>> getDreamWalletRecords(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            Map<String, Object> records = dreamWalletService.getDreamWalletRecords(walletId, page, pageSize);
            return Result.success("获取成功", records);
        } catch (Exception e) {
            log.error("获取梦想钱包交易记录失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
