package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.service.SocialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

/**
 * 社交功能控制器
 */
@RestController
@RequestMapping("/social")
public class SocialController {

    @Autowired
    private SocialService socialService;

    /**
     * 获取用户社交统计数据
     */
    @GetMapping("/user/social-stats")
    public Result<Map<String, Object>> getUserSocialStats(@RequestParam Long userId) {
        try {
            Map<String, Object> stats = socialService.getUserSocialStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 关注钱包
     */
    @PostMapping("/wallet/follow")
    public Result<String> followWallet(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long walletId = Long.valueOf(request.get("walletId").toString());
            
            socialService.followWallet(userId, walletId);
            return Result.success("关注成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消关注钱包
     */
    @PostMapping("/wallet/unfollow")
    public Result<String> unfollowWallet(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long walletId = Long.valueOf(request.get("walletId").toString());
            
            socialService.unfollowWallet(userId, walletId);
            return Result.success("取消关注成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 点赞交易记录
     */
    @PostMapping("/transaction/like")
    public Result<String> likeTransaction(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long transactionId = Long.valueOf(request.get("transactionId").toString());
            
            socialService.likeTransaction(userId, transactionId);
            return Result.success("点赞成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消点赞交易记录
     */
    @PostMapping("/transaction/unlike")
    public Result<String> unlikeTransaction(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long transactionId = Long.valueOf(request.get("transactionId").toString());
            
            socialService.unlikeTransaction(userId, transactionId);
            return Result.success("取消点赞成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 评论交易记录
     */
    @PostMapping("/transaction/comment")
    public Result<Map<String, Object>> commentTransaction(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long transactionId = Long.valueOf(request.get("transactionId").toString());
            String content = request.get("content").toString();
            
            Map<String, Object> comment = socialService.commentTransaction(userId, transactionId, content);
            return Result.success("评论成功", comment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取交易评论列表
     */
    @GetMapping("/transaction/comments")
    public Result<List<Map<String, Object>>> getTransactionComments(@RequestParam Long transactionId) {
        try {
            List<Map<String, Object>> comments = socialService.getTransactionComments(transactionId);
            return Result.success(comments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户互动消息
     */
    @GetMapping("/user/interaction-messages")
    public Result<List<Map<String, Object>>> getUserInteractionMessages(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            List<Map<String, Object>> messages = socialService.getUserInteractionMessages(userId, page, pageSize);
            return Result.success(messages);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取交易的社交数据（点赞数、评论数、用户是否已点赞）
     */
    @GetMapping("/transaction/social-data")
    public Result<Map<String, Object>> getTransactionSocialData(
            @RequestParam Long transactionId,
            @RequestParam(required = false) Long userId) {
        try {
            Map<String, Object> socialData = socialService.getTransactionSocialData(transactionId, userId);
            return Result.success(socialData);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户未读消息数量
     */
    @GetMapping("/user/unread-count")
    public Result<Integer> getUnreadMessageCount(@RequestParam Long userId) {
        try {
            Integer count = socialService.getUnreadMessageCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/user/mark-read")
    public Result<String> markMessagesAsRead(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            socialService.markMessagesAsRead(userId);
            return Result.success("标记成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查用户关注状态
     */
    @GetMapping("/user/check-follow")
    public Result<Boolean> checkFollowStatus(@RequestParam Long currentUserId, @RequestParam Long targetUserId) {
        try {
            Boolean isFollowing = socialService.checkUserFollowStatus(currentUserId, targetUserId);
            return Result.success(isFollowing);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取钱包社交统计数据
     */
    @GetMapping("/wallet/social-stats")
    public Result<Map<String, Object>> getWalletSocialStats(@RequestParam Long walletId) {
        try {
            Map<String, Object> stats = socialService.getWalletSocialStats(walletId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 记录钱包浏览
     */
    @PostMapping("/wallet/view")
    public Result<String> recordWalletView(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long walletId = Long.valueOf(request.get("walletId").toString());
            
            socialService.recordWalletView(userId, walletId);
            return Result.success("浏览记录成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}