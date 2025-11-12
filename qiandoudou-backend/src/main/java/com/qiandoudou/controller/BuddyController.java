package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.service.BuddyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 搭子功能控制器
 */
@RestController
@RequestMapping("/buddy")
public class BuddyController {

    private static final Logger logger = LoggerFactory.getLogger(BuddyController.class);

    @Autowired
    private BuddyService buddyService;

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("搭子功能API正常", "Hello Buddy!");
    }

    /**
     * 获取所有可用的自由选择搭子角色
     */
    @GetMapping("/characters/free")
    public Result<List<Map<String, Object>>> getFreeBuddyCharacters() {
        try {
            List<Map<String, Object>> characters = buddyService.getFreeBuddyCharacters();
            return Result.success("获取自由搭子角色成功", characters);
        } catch (Exception e) {
            logger.error("获取自由搭子角色失败", e);
            return Result.error("获取自由搭子角色失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有可用的搭子圈子
     */
    @GetMapping("/groups")
    public Result<List<Map<String, Object>>> getBuddyGroups() {
        try {
            List<Map<String, Object>> groups = buddyService.getBuddyGroups();
            return Result.success("获取搭子圈子成功", groups);
        } catch (Exception e) {
            logger.error("获取搭子圈子失败", e);
            return Result.error("获取搭子圈子失败: " + e.getMessage());
        }
    }

    /**
     * 根据圈子ID获取角色列表
     */
    @GetMapping("/characters/group/{groupId}")
    public Result<List<Map<String, Object>>> getBuddyCharactersByGroupId(@PathVariable Long groupId) {
        try {
            List<Map<String, Object>> characters = buddyService.getBuddyCharactersByGroupId(groupId);
            return Result.success("获取圈子角色成功", characters);
        } catch (Exception e) {
            logger.error("获取圈子角色失败，圈子ID: {}", groupId, e);
            return Result.error("获取圈子角色失败: " + e.getMessage());
        }
    }

    /**
     * 根据钱包ID获取搭子角色信息
     */
    @GetMapping("/characters/wallet/{walletId}")
    public Result<List<Map<String, Object>>> getBuddyCharactersByWalletId(@PathVariable Long walletId) {
        try {
            List<Map<String, Object>> characters = buddyService.getBuddyCharactersByWalletId(walletId);
            return Result.success("获取钱包搭子角色成功", characters);
        } catch (Exception e) {
            logger.error("获取钱包搭子角色失败，钱包ID: {}", walletId, e);
            return Result.error("获取钱包搭子角色失败: " + e.getMessage());
        }
    }

    /**
     * 创建搭子钱包
     */
    @PostMapping("/wallet/create")
    public Result<Map<String, Object>> createBuddyWallet(@RequestBody Map<String, Object> request) {
        System.out.println("------------------1--------------");
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String walletName = request.get("walletName").toString();

            // 健壮解析 buddyCharacterIds（可能是 Integer/Long/字符串）
            List<Long> buddyCharacterIds = new java.util.ArrayList<>();
            Object idsObj = request.get("buddyCharacterIds");
            if (idsObj instanceof java.util.List) {
                for (Object item : (java.util.List<?>) idsObj) {
                    if (item != null) {
                        try {
                            buddyCharacterIds.add(Long.valueOf(item.toString()));
                        } catch (Exception parseEx) {
                            logger.warn("解析buddyCharacterIds元素失败: {}", item);
                        }
                    }
                }
            }

            Long groupId = request.get("groupId") != null ? Long.valueOf(request.get("groupId").toString()) : null;
            
            // 解析背景图参数
            String backgroundImage = request.get("backgroundImage") != null ? request.get("backgroundImage").toString() : null;

            Map<String, Object> result = buddyService.createBuddyWallet(userId, walletName, buddyCharacterIds, groupId, backgroundImage);
            return Result.success("创建搭子钱包成功", result);
        } catch (Exception e) {
            logger.error("创建搭子钱包失败", e);
            return Result.error("创建搭子钱包失败: " + e.getMessage());
        }
    }

    /**
     * 获取搭子角色详情
     */
    @GetMapping("/character/{characterId}")
    public Result<Map<String, Object>> getBuddyCharacterDetail(@PathVariable Long characterId) {
        try {
            Map<String, Object> detail = buddyService.getBuddyCharacterDetail(characterId);
            if (detail != null) {
                return Result.success("获取搭子角色详情成功", detail);
            } else {
                return Result.error("搭子角色不存在");
            }
        } catch (Exception e) {
            logger.error("获取搭子角色详情失败，角色ID: {}", characterId, e);
            return Result.error("获取搭子角色详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取圈子详情
     */
    @GetMapping("/group/{groupId}")
    public Result<Map<String, Object>> getBuddyGroupDetail(@PathVariable Long groupId) {
        try {
            Map<String, Object> detail = buddyService.getBuddyGroupDetail(groupId);
            if (detail != null) {
                return Result.success("获取圈子详情成功", detail);
            } else {
                return Result.error("圈子不存在");
            }
        } catch (Exception e) {
            logger.error("获取圈子详情失败，圈子ID: {}", groupId, e);
            return Result.error("获取圈子详情失败: " + e.getMessage());
        }
    }

    /**
     * 生成搭子评论（供内部调用）
     */
    @PostMapping("/comment/generate")
    public Result<Map<String, Object>> generateBuddyComment(@RequestBody Map<String, Object> request) {
        try {
            Long transactionId = Long.valueOf(request.get("transactionId").toString());
            Long buddyCharacterId = Long.valueOf(request.get("buddyCharacterId").toString());
            String postContent = request.get("postContent").toString();
            String imageUrl = request.get("imageUrl") != null ? request.get("imageUrl").toString() : null;

            Map<String, Object> comment = buddyService.generateBuddyComment(transactionId, buddyCharacterId, postContent, imageUrl);
            return Result.success("生成搭子评论成功", comment);
        } catch (Exception e) {
            logger.error("生成搭子评论失败", e);
            return Result.error("生成搭子评论失败: " + e.getMessage());
        }
    }
}
