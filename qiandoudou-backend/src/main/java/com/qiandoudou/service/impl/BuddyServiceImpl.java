package com.qiandoudou.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiandoudou.entity.*;
import com.qiandoudou.mapper.*;
import com.qiandoudou.service.*;
import com.qiandoudou.service.BuddyWalletInitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搭子服务实现类
 */
@Service
public class BuddyServiceImpl implements BuddyService {

    private static final Logger logger = LoggerFactory.getLogger(BuddyServiceImpl.class);

    @Autowired
    private BuddyCharacterMapper buddyCharacterMapper;
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private BuddyGroupMapper buddyGroupMapper;

    @Autowired
    private WalletBuddyMapper walletBuddyMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private AiService aiService;

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BuddyWalletInitService buddyWalletInitService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Override
    public List<Map<String, Object>> getFreeBuddyCharacters() {
        try {
            List<BuddyCharacter> characters = buddyCharacterMapper.getFreeBuddyCharacters();
            return characters.stream().map(this::convertCharacterToMap).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取自由选择搭子角色失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getBuddyGroups() {
        try {
            List<BuddyGroup> groups = buddyGroupMapper.getActiveBuddyGroups();
            return groups.stream().map(this::convertGroupToMap).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取搭子圈子失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getBuddyCharactersByGroupId(Long groupId) {
        try {
            List<BuddyCharacter> characters = buddyCharacterMapper.getBuddyCharactersByGroupId(groupId);
            return characters.stream().map(this::convertCharacterToMap).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取圈子角色失败，圈子ID: {}", groupId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getBuddyCharactersByWalletId(Long walletId) {
        try {
            List<BuddyCharacter> characters = buddyCharacterMapper.getBuddyCharactersByWalletId(walletId);
            return characters.stream().map(this::convertCharacterToMap).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取钱包搭子角色失败，钱包ID: {}", walletId, e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createBuddyWallet(Long userId, String walletName, List<Long> buddyCharacterIds, Long groupId, String backgroundImage) {
        try {
            // 如果前端未传或传得不全，且给了 groupId，则兜底用圈内全员
            if ((buddyCharacterIds == null || buddyCharacterIds.isEmpty()) && groupId != null) {
                List<BuddyCharacter> groupCharacters = buddyCharacterMapper.getBuddyCharactersByGroupId(groupId);
                buddyCharacterIds = groupCharacters.stream().map(BuddyCharacter::getId).collect(Collectors.toList());
                logger.info("根据groupId={} 兜底填充 buddyCharacterIds, 数量: {}", groupId, buddyCharacterIds.size());
            }

            int buddyCount = buddyCharacterIds == null ? 0 : buddyCharacterIds.size();

            // 校验规则：
            // - 自由选搭（无groupId）：2-6个
            // - 圈子模式（有groupId）：至少2个（允许圈内全员，可能>6）
            if (groupId == null) {
                if (buddyCount < 2 || buddyCount > 6) {
                    throw new RuntimeException("搭子数量必须在2-6个之间");
                }
            } else {
                if (buddyCount < 2) {
                    throw new RuntimeException("圈子模式至少需要2个搭子");
                }
            }

            // 经过校验后保证列表非空，避免静态检查告警
            if (buddyCharacterIds == null) {
                buddyCharacterIds = java.util.Collections.emptyList();
            }

            // 设置搭子钱包背景图
            String finalBackgroundImage = backgroundImage;
            if (finalBackgroundImage == null || finalBackgroundImage.trim().isEmpty()) {
                if (groupId != null) {
                    // 圈中选角：使用圈子的背景图
                    BuddyGroup buddyGroup = buddyGroupMapper.selectById(groupId);
                    if (buddyGroup != null && buddyGroup.getBackgroundImage() != null && !buddyGroup.getBackgroundImage().trim().isEmpty()) {
                        finalBackgroundImage = buddyGroup.getBackgroundImage();
                    } else {
                        // 如果圈子没有背景图，使用默认搭子背景图
                        finalBackgroundImage = "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dazi/dazizan_beijingtu.jpg";
                    }
                } else {
                    // 自由选搭：使用默认搭子背景图
                    finalBackgroundImage = "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dazi/dazizan_beijingtu.jpg";
                }
            }

            // 创建搭子钱包（类型为3）
            Wallet wallet = walletService.createWallet(userId, walletName, 3, finalBackgroundImage, null);

            // 添加搭子到钱包
            addBuddiesToWallet(wallet.getId(), buddyCharacterIds);

            logger.info("搭子钱包创建成功，钱包ID: {}, 搭子数量: {}", wallet.getId(), buddyCharacterIds.size());
            
            // 使用专门的初始化服务异步执行初始化，避免前端超时
            try {
                buddyWalletInitService.initializeBuddyWalletAsync(wallet.getId(), buddyCharacterIds);
                logger.info("搭子钱包异步初始化已启动");
            } catch (Exception e) {
                logger.error("搭子钱包异步初始化启动失败", e);
            }

            // 返回创建结果
            Map<String, Object> result = new HashMap<>();
            result.put("walletId", wallet.getId());
            result.put("walletName", wallet.getName());
            result.put("buddyCount", buddyCharacterIds.size());
            result.put("groupId", groupId);

            logger.info("搭子钱包创建成功，钱包ID: {}, 搭子数量: {}", wallet.getId(), buddyCharacterIds.size());
            
            return result;

        } catch (Exception e) {
            logger.error("创建搭子钱包失败", e);
            throw new RuntimeException("创建搭子钱包失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void addBuddiesToWallet(Long walletId, List<Long> buddyCharacterIds) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            for (Long buddyCharacterId : buddyCharacterIds) {
                // 检查是否已存在
                Integer existCount = walletBuddyMapper.checkBuddyExists(walletId, buddyCharacterId);
                if (existCount > 0) {
                    continue; // 已存在，跳过
                }

                // 创建钱包搭子关系
                WalletBuddy walletBuddy = new WalletBuddy();
                walletBuddy.setWalletId(walletId);
                walletBuddy.setBuddyCharacterId(buddyCharacterId);
                walletBuddy.setJoinTime(now);
                walletBuddy.setIsActive(1);

                walletBuddyMapper.insert(walletBuddy);
            }

            logger.info("为钱包 {} 添加搭子成功，数量: {}", walletId, buddyCharacterIds.size());

        } catch (Exception e) {
            logger.error("添加钱包搭子失败，钱包ID: {}", walletId, e);
            throw new RuntimeException("添加钱包搭子失败: " + e.getMessage());
        }
    }

    /**
     * 为每个搭子创建初始转账和评论（同步执行）
     */
    private void createInitialBuddyTransfers(Long walletId, List<Long> buddyCharacterIds) {
        try {
            logger.info("开始初始化搭子钱包，钱包ID: {}, 搭子数量: {}", walletId, buddyCharacterIds.size());
            
            // 获取钱包信息
            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                logger.error("钱包不存在，钱包ID: {}", walletId);
                return;
            }

            // 为每个搭子创建初始转账
            for (Long buddyCharacterId : buddyCharacterIds) {
                try {
                    // 获取搭子角色信息
                    BuddyCharacter buddyCharacter = buddyCharacterMapper.selectById(buddyCharacterId);
                    if (buddyCharacter == null) {
                        logger.warn("搭子角色不存在，ID: {}", buddyCharacterId);
                        continue;
                    }

                    // 创建初始转账（每个搭子转入0.01元）
                    createBuddyInitialTransfer(walletId, buddyCharacter);

                } catch (Exception e) {
                    logger.error("为搭子创建初始转账失败，搭子ID: {}", buddyCharacterId, e);
                }
            }

            logger.info("搭子钱包初始化完成，钱包ID: {}", walletId);

        } catch (Exception e) {
            logger.error("初始化搭子钱包失败，钱包ID: {}", walletId, e);
        }
    }

    /**
     * 为搭子创建初始转账
     */
    private void createBuddyInitialTransfer(Long walletId, BuddyCharacter buddyCharacter) {
        try {
            logger.info("开始为搭子创建初始转账，钱包ID: {}, 搭子: {}", walletId, buddyCharacter.getName());
            
            BigDecimal initialAmount = new BigDecimal("0.01");
            
            // 获取钱包信息
            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                logger.error("钱包不存在，钱包ID: {}", walletId);
                throw new RuntimeException("钱包不存在");
            }
            
            logger.info("钱包信息获取成功，当前余额: {}", wallet.getBalance());

            // 更新钱包余额
            BigDecimal newBalance = wallet.getBalance().add(initialAmount);
            wallet.setBalance(newBalance);
            walletService.updateById(wallet);
            
            logger.info("钱包余额更新成功，新余额: {}", newBalance);

            // 生成初始评论内容（简化版本，避免AI调用失败）
            String welcomeMessage = String.format("大家好！我是%s，很高兴加入这个搭子攒钱计划！让我们一起努力存钱吧！💪", buddyCharacter.getName());
            
            logger.info("欢迎消息生成: {}", welcomeMessage);

            // 创建交易记录
            Transaction transaction = new Transaction();
            transaction.setWalletId(walletId);
            transaction.setUserId(wallet.getUserId());
            transaction.setType(1); // 转入类型
            transaction.setAmount(initialAmount);
            transaction.setBalanceAfter(newBalance);
            transaction.setDescription("搭子初始转账");
            transaction.setNote(welcomeMessage);
            // 设置搭子相关字段
            transaction.setAiPartnerId(null); // 搭子不是AI伴侣
            transaction.setAiPartnerName(buddyCharacter.getName());
            transaction.setAiPartnerAvatar(buddyCharacter.getAvatar());
            transaction.setAiMessage(welcomeMessage);
            transaction.setCreateTime(LocalDateTime.now());
            
            logger.info("准备保存交易记录...");
            transactionService.save(transaction);
            logger.info("交易记录保存成功，交易ID: {}", transaction.getId());

            // 暂时跳过评论记录创建，专注于交易记录
            // saveBuddyComment(transaction.getId(), buddyCharacter.getId(), welcomeMessage, null);

            logger.info("搭子初始转账创建成功，搭子: {}, 金额: {}, 交易ID: {}", buddyCharacter.getName(), initialAmount, transaction.getId());

        } catch (Exception e) {
            logger.error("创建搭子初始转账失败，搭子: {}, 错误详情: ", buddyCharacter.getName(), e);
            throw e; // 重新抛出异常以便上层处理
        }
    }

    /**
     * 生成搭子欢迎消息
     */
    private String generateBuddyWelcomeMessage(BuddyCharacter buddyCharacter, String walletName) {
        try {
            // 构建欢迎提示词
            String prompt = String.format(
                "你是%s，%s。现在加入了一个新的搭子攒钱钱包'%s'，请生成一条欢迎加入的消息，表达你对这次搭子攒钱计划的期待和支持。消息要体现你的性格特点，控制在50字以内。",
                buddyCharacter.getName(),
                buddyCharacter.getPersonality(),
                walletName
            );

            // 调用AI生成服务 - 使用现有的评论生成方法
            String welcomeMessage = aiService.generatePartnerComment(null, prompt);
            if (welcomeMessage != null && !welcomeMessage.trim().isEmpty()) {
                return welcomeMessage.trim();
            }

        } catch (Exception e) {
            logger.warn("AI生成搭子欢迎消息失败: {}", e.getMessage());
        }

        // 如果AI生成失败，返回默认消息
        return String.format("大家好！我是%s，很高兴加入这个搭子攒钱计划！让我们一起努力存钱吧！💪", buddyCharacter.getName());
    }

    /**
     * 在新事务中初始化搭子钱包
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void initializeBuddyWalletInNewTransaction(Long walletId, List<Long> buddyCharacterIds) {
        try {
            logger.info("开始在新事务中初始化搭子钱包，钱包ID: {}, 搭子数量: {}", walletId, buddyCharacterIds.size());
            
            for (Long buddyCharacterId : buddyCharacterIds) {
                try {
                    BuddyCharacter buddyCharacter = buddyCharacterMapper.selectById(buddyCharacterId);
                    if (buddyCharacter != null) {
                        String welcomeMessage = String.format("大家好！我是%s，很高兴加入这个搭子攒钱计划！", buddyCharacter.getName());
                        
                        // 使用已有的transferIn方法
                        walletService.transferIn(walletId, new BigDecimal("0.01"), "搭子初始转账", null, welcomeMessage);
                        
                        logger.info("搭子 {} 初始转账成功", buddyCharacter.getName());
                    }
                } catch (Exception e) {
                    logger.error("搭子初始转账失败，搭子ID: {}", buddyCharacterId, e);
                }
            }
            
            logger.info("搭子钱包新事务初始化完成，钱包ID: {}", walletId);
        } catch (Exception e) {
            logger.error("搭子钱包新事务初始化失败，钱包ID: {}", walletId, e);
            throw e; // 重新抛出异常以便上层处理
        }
    }

    /**
     * 异步初始化搭子钱包
     */
    @Async
    public void initializeBuddyWalletAsync(Long walletId, List<Long> buddyCharacterIds) {
        try {
            logger.info("开始异步初始化搭子钱包，钱包ID: {}, 搭子数量: {}", walletId, buddyCharacterIds.size());
            
            // 等待一小段时间确保事务已提交
            Thread.sleep(1000);
            
            for (Long buddyCharacterId : buddyCharacterIds) {
                try {
                    BuddyCharacter buddyCharacter = buddyCharacterMapper.selectById(buddyCharacterId);
                    if (buddyCharacter != null) {
                        String welcomeMessage = String.format("大家好！我是%s，很高兴加入这个搭子攒钱计划！", buddyCharacter.getName());
                        
                        // 使用已有的transferIn方法
                        walletService.transferIn(walletId, new BigDecimal("0.01"), "搭子初始转账", null, welcomeMessage);
                        
                        logger.info("搭子 {} 初始转账成功", buddyCharacter.getName());
                    }
                } catch (Exception e) {
                    logger.error("搭子初始转账失败，搭子ID: {}", buddyCharacterId, e);
                }
            }
            
            logger.info("搭子钱包异步初始化完成，钱包ID: {}", walletId);
        } catch (Exception e) {
            logger.error("搭子钱包异步初始化失败，钱包ID: {}", walletId, e);
        }
    }

    @Override
    public Map<String, Object> generateBuddyComment(Long transactionId, Long buddyCharacterId, String postContent, String imageUrl) {
        try {
            logger.info("开始生成搭子评论，交易ID: {}, 搭子ID: {}", transactionId, buddyCharacterId);

            // 获取搭子角色信息
            BuddyCharacter buddyCharacter = buddyCharacterMapper.selectById(buddyCharacterId);
            if (buddyCharacter == null) {
                throw new RuntimeException("搭子角色不存在");
            }

            // 生成AI评论内容
            String commentContent;
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // 有图片时，结合图片和文本生成评论
                commentContent = generateBuddyCommentWithImage(buddyCharacter, postContent, imageUrl);
            } else {
                // 无图片时，只根据文本生成评论
                commentContent = generateBuddyCommentWithText(buddyCharacter, postContent);
            }

            // 生成语音
            String voiceUrl = null;
            try {
                voiceUrl = aiService.generatePartnerVoiceByCharacterName(buddyCharacter.getName(), commentContent);
                if (voiceUrl != null) {
                    logger.info("搭子语音生成成功: {}", voiceUrl);
                } else {
                    logger.warn("搭子语音生成失败，将只保存文字评论");
                }
            } catch (Exception e) {
                logger.warn("搭子语音生成异常: {}", e.getMessage());
            }

            // 保存评论记录
            Map<String, Object> comment = saveBuddyComment(transactionId, buddyCharacterId, commentContent, voiceUrl);

            // 补充搭子信息
            comment.put("buddyCharacterName", buddyCharacter.getName());
            comment.put("buddyCharacterAvatar", buddyCharacter.getAvatar());
            comment.put("isBuddyComment", true);

            logger.info("搭子评论生成完成，评论ID: {}", comment.get("id"));
            return comment;

        } catch (Exception e) {
            logger.error("生成搭子评论失败，交易ID: {}, 搭子ID: {}", transactionId, buddyCharacterId, e);
            throw new RuntimeException("生成搭子评论失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getBuddyCharacterDetail(Long characterId) {
        try {
            Map<String, Object> detail = buddyCharacterMapper.getBuddyCharacterDetail(characterId);
            if (detail != null) {
                // 解析标签JSON
                String tagsJson = (String) detail.get("tags");
                if (tagsJson != null) {
                    try {
                        List<String> tags = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
                        detail.put("tagsList", tags);
                    } catch (Exception e) {
                        logger.warn("解析角色标签失败: {}", e.getMessage());
                        detail.put("tagsList", new ArrayList<>());
                    }
                }
            }
            return detail;
        } catch (Exception e) {
            logger.error("获取搭子角色详情失败，角色ID: {}", characterId, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getBuddyGroupDetail(Long groupId) {
        try {
            return buddyGroupMapper.getBuddyGroupDetail(groupId);
        } catch (Exception e) {
            logger.error("获取圈子详情失败，圈子ID: {}", groupId, e);
            return null;
        }
    }

    /**
     * 转换角色实体为Map
     */
    private Map<String, Object> convertCharacterToMap(BuddyCharacter character) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", character.getId());
        map.put("name", character.getName());
        map.put("nickname", character.getNickname());
        map.put("avatar", character.getAvatar());
        map.put("fullImage", character.getFullImage());
        map.put("gender", character.getGender());
        map.put("description", character.getDescription());
        map.put("voiceSampleUrl", character.getVoiceSampleUrl());
        map.put("voiceDuration", character.getVoiceDuration());

        // 解析标签JSON
        try {
            if (character.getTags() != null) {
                List<String> tags = objectMapper.readValue(character.getTags(), new TypeReference<List<String>>() {});
                map.put("tags", tags);
            } else {
                map.put("tags", new ArrayList<>());
            }
        } catch (Exception e) {
            logger.warn("解析角色标签失败: {}", e.getMessage());
            map.put("tags", new ArrayList<>());
        }

        return map;
    }

    /**
     * 转换圈子实体为Map
     */
    private Map<String, Object> convertGroupToMap(BuddyGroup group) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        map.put("name", group.getName());
        map.put("description", group.getDescription());
        map.put("coverImage", group.getCoverImage());
        map.put("backgroundImage", group.getBackgroundImage());
        map.put("memberCount", group.getMemberCount());
        return map;
    }

    /**
     * 根据文本生成搭子评论
     */
    private String generateBuddyCommentWithText(BuddyCharacter buddyCharacter, String postContent) {
        try {
            // 构建针对搭子角色的提示词
            String prompt = String.format("%s。现在你的朋友发了一条动态：%s。请以你的性格特点回复一条简短的评论，不超过50字。", 
                buddyCharacter.getPersonality(), postContent);
            
            return aiService.generateAiText(prompt);
        } catch (Exception e) {
            logger.warn("生成搭子文本评论失败，使用默认回复: {}", e.getMessage());
            return buddyCharacter.getName() + "：不错哦！继续加油！";
        }
    }

    /**
     * 根据图片和文本生成搭子评论
     */
    private String generateBuddyCommentWithImage(BuddyCharacter buddyCharacter, String postContent, String imageUrl) {
        try {
            // 先从图片生成描述
            String imageDescription = aiService.generateTextFromImage(imageUrl);
            
            // 结合图片描述和文本生成评论
            String prompt = String.format("%s。现在你的朋友发了一条动态，内容是：%s，配图显示：%s。请以你的性格特点回复一条简短的评论，不超过50字。", 
                buddyCharacter.getPersonality(), postContent, imageDescription);
            
            return aiService.generateAiText(prompt);
        } catch (Exception e) {
            logger.warn("生成搭子图文评论失败，降级为文本评论: {}", e.getMessage());
            return generateBuddyCommentWithText(buddyCharacter, postContent);
        }
    }

    /**
     * 保存搭子评论记录
     */
    private Map<String, Object> saveBuddyComment(Long transactionId, Long buddyCharacterId, String content, String voiceUrl) {
        try {
            // 在post_comments表中插入搭子评论记录
            PostComment postComment = new PostComment();
            postComment.setPostId(transactionId);
            postComment.setUserId(buddyCharacterId); // 使用搭子角色ID作为用户ID
            postComment.setContent(content);
            postComment.setIsAiComment(1); // 标记为AI评论
            postComment.setBuddyCharacterId(buddyCharacterId); // 设置搭子角色ID
            postComment.setVoiceUrl(voiceUrl); // 设置语音URL

            postCommentMapper.insert(postComment);
            logger.info("搭子评论记录保存成功，评论ID: {}", postComment.getId());

            // 创建搭子评论通知
            createBuddyCommentNotification(buddyCharacterId, transactionId, content);

            // 返回评论信息
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", postComment.getId());
            comment.put("userId", buddyCharacterId);
            comment.put("transactionId", transactionId);
            comment.put("content", content);
            comment.put("voiceUrl", voiceUrl);
            comment.put("createTime", LocalDateTime.now());

            return comment;

        } catch (Exception e) {
            logger.error("保存搭子评论失败", e);
            throw new RuntimeException("保存搭子评论失败: " + e.getMessage());
        }
    }

    /**
     * 创建搭子评论通知
     */
    private void createBuddyCommentNotification(Long buddyCharacterId, Long transactionId, String content) {
        try {
            // 获取交易信息以找到交易所有者
            Transaction transaction = transactionMapper.selectById(transactionId);
            if (transaction == null) {
                logger.warn("交易不存在，无法创建搭子评论通知，交易ID: {}", transactionId);
                return;
            }

            // 获取搭子角色信息
            BuddyCharacter buddyCharacter = buddyCharacterMapper.selectById(buddyCharacterId);
            if (buddyCharacter == null) {
                logger.warn("搭子角色不存在，无法创建搭子评论通知，搭子ID: {}", buddyCharacterId);
                return;
            }

            // 获取钱包信息以找到钱包所有者
            Long ownerId = transaction.getUserId(); // 交易的用户ID就是钱包所有者
            
            if (ownerId != null) {
                // 创建搭子评论通知
                notificationMapper.createInteractionNotification(
                    ownerId,           // 接收者（钱包所有者）
                    buddyCharacterId,  // 发送者（搭子角色ID）
                    2,                 // 类型：2-评论
                    buddyCharacter.getName() + " 评论了你的动态",  // 标题
                    content,           // 评论内容
                    null,              // 关联ID
                    transaction.getWalletId(),  // 钱包ID
                    transactionId,     // 交易ID
                    transaction.getImageUrl()   // 交易图片
                );
                
                logger.info("创建搭子评论通知成功：搭子 {} 评论了用户 {} 的交易 {}", buddyCharacter.getName(), ownerId, transactionId);
            }
        } catch (Exception e) {
            logger.error("创建搭子评论通知失败: {}", e.getMessage(), e);
        }
    }
}
