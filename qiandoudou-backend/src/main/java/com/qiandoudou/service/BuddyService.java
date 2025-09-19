package com.qiandoudou.service;

import java.util.List;
import java.util.Map;

/**
 * 搭子服务接口
 */
public interface BuddyService {

    /**
     * 获取所有可用的自由选择搭子角色
     */
    List<Map<String, Object>> getFreeBuddyCharacters();

    /**
     * 获取所有可用的搭子圈子
     */
    List<Map<String, Object>> getBuddyGroups();

    /**
     * 根据圈子ID获取角色列表
     */
    List<Map<String, Object>> getBuddyCharactersByGroupId(Long groupId);

    /**
     * 根据钱包ID获取搭子角色信息
     */
    List<Map<String, Object>> getBuddyCharactersByWalletId(Long walletId);

    /**
     * 创建搭子钱包
     */
    Map<String, Object> createBuddyWallet(Long userId, String walletName, List<Long> buddyCharacterIds, Long groupId, String backgroundImage);

    /**
     * 为钱包添加搭子
     */
    void addBuddiesToWallet(Long walletId, List<Long> buddyCharacterIds);

    /**
     * 生成搭子评论
     */
    Map<String, Object> generateBuddyComment(Long transactionId, Long buddyCharacterId, String postContent, String imageUrl);

    /**
     * 获取搭子角色详情
     */
    Map<String, Object> getBuddyCharacterDetail(Long characterId);

    /**
     * 获取圈子详情
     */
    Map<String, Object> getBuddyGroupDetail(Long groupId);
}
