package com.qiandoudou.service.impl;

import com.alibaba.fastjson2.JSON;
import com.qiandoudou.entity.*;
import com.qiandoudou.mapper.BuddyCharacterMapper;
import com.qiandoudou.mapper.BuddyInteractionMapper;
import com.qiandoudou.mapper.WalletBuddyMapper;
import com.qiandoudou.mapper.PostCommentMapper;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.BuddyInteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 搭子互动服务实现类
 */
@Service
public class BuddyInteractionServiceImpl implements BuddyInteractionService {

    private static final Logger logger = LoggerFactory.getLogger(BuddyInteractionServiceImpl.class);

    // 互动类型常量
    private static final int INTERACTION_TYPE_COMMENT = 1;
    private static final int INTERACTION_TYPE_LIKE = 2;
    private static final int INTERACTION_TYPE_VOICE = 3;

    // 冷却时间（分钟）
    private static final int COOLDOWN_MINUTES = 5;

    // 最大评论数
    private static final int MAX_COMMENTS_PER_TRANSACTION = 3;

    @Autowired
    private BuddyCharacterMapper buddyCharacterMapper;

    @Autowired
    private BuddyInteractionMapper buddyInteractionMapper;

    @Autowired
    private WalletBuddyMapper walletBuddyMapper;

    @Autowired
    private AiService aiService;

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Override
    @Async
    public void processTransactionInteraction(Long walletId, Transaction transaction) {
        try {
            logger.info("开始处理搭子互动，钱包ID: {}, 交易ID: {}", walletId, transaction.getId());

            // 1. 获取钱包的搭子列表
            List<BuddyCharacter> buddies = buddyCharacterMapper.getWalletBuddies(walletId);
            if (buddies.isEmpty()) {
                logger.info("钱包没有搭子角色，跳过互动处理，钱包ID: {}", walletId);
                return;
            }

            // 2. 生成搭子评论
            List<PostComment> comments = generateBuddyComments(walletId, transaction, buddies);

            // 3. 保存评论到数据库
            for (PostComment comment : comments) {
                try {
                    postCommentMapper.insert(comment);
                    logger.info("搭子评论已保存，角色: {}, 交易ID: {}", comment.getAiPartnerId(), transaction.getId());
                } catch (Exception e) {
                    logger.error("保存搭子评论失败，角色ID: {}, 交易ID: {}", comment.getAiPartnerId(), transaction.getId(), e);
                }
            }

            logger.info("搭子互动处理完成，钱包ID: {}, 生成评论数: {}", walletId, comments.size());

        } catch (Exception e) {
            logger.error("处理搭子互动失败，钱包ID: {}, 交易ID: {}", walletId, transaction.getId(), e);
        }
    }

    @Override
    @Async
    public void triggerWelcomeInteraction(Long walletId) {
        try {
            logger.info("触发欢迎互动，钱包ID: {}", walletId);

            // 获取钱包的搭子列表
            List<BuddyCharacter> buddies = buddyCharacterMapper.getWalletBuddies(walletId);
            if (buddies.isEmpty()) {
                return;
            }

            // 随机选择一个搭子发送欢迎消息
            BuddyCharacter welcomeBuddy = buddies.get(new Random().nextInt(buddies.size()));
            
            // 生成欢迎消息
            String welcomeMessage = generateWelcomeMessage(welcomeBuddy);
            
            // 生成语音
            String voiceUrl = generateBuddyCommentVoice(welcomeMessage, welcomeBuddy.getVoiceType());
            
            // 记录互动
            BuddyInteraction interaction = new BuddyInteraction();
            interaction.setWalletId(walletId);
            interaction.setCharacterId(welcomeBuddy.getId());
            interaction.setTransactionId(null); // 欢迎消息不关联具体交易
            interaction.setInteractionType(INTERACTION_TYPE_COMMENT);
            interaction.setContent(welcomeMessage);
            interaction.setVoiceUrl(voiceUrl);
            interaction.setVoiceDuration(estimateVoiceDuration(welcomeMessage));
            buddyInteractionMapper.insert(interaction);

            logger.info("欢迎互动完成，钱包ID: {}, 搭子: {}", walletId, welcomeBuddy.getName());

        } catch (Exception e) {
            logger.error("触发欢迎互动失败，钱包ID: {}", walletId, e);
        }
    }

    @Override
    public boolean canCharacterInteract(Long characterId, Long walletId, Integer interactionType) {
        try {
            LocalDateTime cooldownTime = LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES);
            int recentCount = buddyInteractionMapper.countRecentInteractions(
                characterId, walletId, interactionType, cooldownTime);
            return recentCount == 0;
        } catch (Exception e) {
            logger.error("检查角色互动冷却失败，角色ID: {}, 钱包ID: {}", characterId, walletId, e);
            return false;
        }
    }

    @Override
    public String generateBuddyComment(Transaction transaction, Long characterId) {
        try {
            BuddyCharacter character = buddyCharacterMapper.selectById(characterId);
            if (character == null) {
                return null;
            }

            // 根据角色性格和交易信息生成评论
            return generateCommentByCharacter(transaction, character);

        } catch (Exception e) {
            logger.error("生成搭子评论失败，角色ID: {}, 交易ID: {}", characterId, transaction.getId(), e);
            return null;
        }
    }

    @Override
    public String generateBuddyCommentVoice(String content, String voiceType) {
        try {
            if (content == null || content.isEmpty()) {
                return null;
            }
            return aiService.generatePartnerVoiceByCharacterName(voiceType, content);
        } catch (Exception e) {
            logger.error("生成搭子评论语音失败，语音类型: {}", voiceType, e);
            return null;
        }
    }

    /**
     * 生成搭子评论列表
     */
    private List<PostComment> generateBuddyComments(Long walletId, Transaction transaction, List<BuddyCharacter> buddies) {
        List<PostComment> comments = new ArrayList<>();
        int commentCount = 0;

        // 打乱搭子列表，增加随机性
        List<BuddyCharacter> shuffledBuddies = new ArrayList<>(buddies);
        Collections.shuffle(shuffledBuddies);

        for (BuddyCharacter buddy : shuffledBuddies) {
            if (commentCount >= MAX_COMMENTS_PER_TRANSACTION) {
                break;
            }

            // 检查是否可以互动（冷却控制）
            if (!canCharacterInteract(buddy.getId(), walletId, INTERACTION_TYPE_COMMENT)) {
                continue;
            }

            // 根据概率判断是否评论
            if (!shouldBuddyComment(transaction, buddy)) {
                continue;
            }

            // 生成评论
            PostComment comment = createBuddyComment(transaction, buddy);
            if (comment != null) {
                comments.add(comment);
                commentCount++;

                // 记录互动
                recordBuddyInteraction(walletId, buddy.getId(), transaction.getId(), 
                    INTERACTION_TYPE_COMMENT, comment.getContent(), comment.getVoiceUrl());

                // 更新搭子互动统计
                walletBuddyMapper.updateInteractionStats(walletId, buddy.getId());
            }
        }

        return comments;
    }

    /**
     * 判断搭子是否应该评论
     */
    private boolean shouldBuddyComment(Transaction transaction, BuddyCharacter buddy) {
        double baseProbability = 0.6; // 基础概率

        // 根据交易金额调整概率
        BigDecimal amount = transaction.getAmount();
        if (amount.compareTo(new BigDecimal("100")) > 0) {
            baseProbability += 0.2;
        }
        if (amount.compareTo(new BigDecimal("500")) > 0) {
            baseProbability += 0.1;
        }

        // 根据交易类型调整概率
        if (transaction.getType() == 2) { // 转出
            baseProbability += 0.1; // 花钱更容易引起评论
        }

        // 根据角色性格调整概率
        try {
            Map<String, Object> personality = JSON.parseObject(buddy.getPersonality(), Map.class);
            // 这里可以根据角色性格进一步调整概率
        } catch (Exception e) {
            // 忽略解析错误
        }

        return Math.random() < Math.min(baseProbability, 0.9);
    }

    /**
     * 创建搭子评论
     */
    private PostComment createBuddyComment(Transaction transaction, BuddyCharacter buddy) {
        try {
            // 生成评论内容
            String content = generateCommentByCharacter(transaction, buddy);
            if (content == null || content.isEmpty()) {
                return null;
            }

            // 生成语音
            String voiceUrl = generateBuddyCommentVoice(content, buddy.getVoiceType());
            String voiceDuration = estimateVoiceDuration(content);

            // 创建评论对象
            PostComment comment = new PostComment();
            comment.setPostId(transaction.getId());
            comment.setUserId(buddy.getId()); // 使用角色ID作为虚拟用户ID
            comment.setContent(content);
            comment.setVoiceUrl(voiceUrl);
            comment.setVoiceDuration(voiceDuration);
            comment.setIsAiComment(1);
            comment.setAiPartnerId(buddy.getId());
            comment.setCreateTime(LocalDateTime.now());

            return comment;

        } catch (Exception e) {
            logger.error("创建搭子评论失败，角色: {}, 交易ID: {}", buddy.getName(), transaction.getId(), e);
            return null;
        }
    }

    /**
     * 根据角色特点生成评论内容
     */
    private String generateCommentByCharacter(Transaction transaction, BuddyCharacter character) {
        // 获取评论模板
        Map<String, List<String>> templates = getCommentTemplates(character);
        
        String transactionType = transaction.getType() == 1 ? "transfer_in" : "transfer_out";
        List<String> characterTemplates = templates.get(character.getName().toLowerCase());
        
        if (characterTemplates == null || characterTemplates.isEmpty()) {
            // 使用通用模板
            characterTemplates = getDefaultTemplates(transactionType);
        }
        
        if (characterTemplates.isEmpty()) {
            return null;
        }
        
        // 随机选择模板
        String template = characterTemplates.get(new Random().nextInt(characterTemplates.size()));
        
        // 替换变量
        return template.replace("{amount}", transaction.getAmount().toString())
                      .replace("{description}", transaction.getDescription() != null ? transaction.getDescription() : "");
    }

    /**
     * 获取角色评论模板
     */
    private Map<String, List<String>> getCommentTemplates(BuddyCharacter character) {
        Map<String, List<String>> templates = new HashMap<>();
        
        // 小财迷的评论模板
        templates.put("小财迷", Arrays.asList(
            "不错不错，又攒了{amount}元！离目标更近一步了！",
            "好习惯！这样下去很快就能攒到大钱了！",
            "棒棒的！理财就是要这样坚持！",
            "哎呀，怎么又花钱了？{amount}元就这么没了...",
            "花钱可以，但要花得值得哦！记得控制一下呢～"
        ));
        
        // 搞笑女的评论模板
        templates.put("搞笑女", Arrays.asList(
            "哈哈，又有钱进账了！姐妹你这是要发财的节奏啊！",
            "攒钱小能手上线！给你点个赞👍",
            "哎呀妈呀，花钱如流水啊！心疼钱包君一秒钟💔",
            "花钱一时爽，攒钱火葬场！哈哈哈开玩笑的～",
            "钱钱快到碗里来！攒钱大业不能停！"
        ));
        
        // 黄段子老司机的评论模板
        templates.put("黄段子老司机", Arrays.asList(
            "嘿嘿，这钱攒得比我开车还稳！老司机带你飞～",
            "攒钱如开车，要稳中带皮！这波操作我给满分！",
            "花钱要有技巧，就像开车要会踩刹车，懂我意思吧？😏",
            "钱包和车一样，都需要好好保养，不能乱开哦～",
            "老司机提醒：前方有消费陷阱，请减速慢行！"
        ));
        
        // 孙悟空的评论模板
        templates.put("孙悟空", Arrays.asList(
            "好！俺老孙最喜欢看到银子入账！{amount}元到手！",
            "不错不错，这点钱虽然不多，但蚊子腿也是肉啊！",
            "师父会很高兴的，我们的取经基金又增加了！",
            "哎呀，怎么又花钱了？俺老孙的火眼金睛都看不下去了！",
            "花钱要三思啊，别像猪八戒那样冲动！"
        ));
        
        // 猪八戒的评论模板  
        templates.put("猪八戒", Arrays.asList(
            "嘿嘿，俺也想攒钱，但师父不让买好吃的...",
            "师父说了，要控制欲望！你这样攒钱很对！",
            "哎呀，花钱买好吃的了吗？俺也想要...",
            "俺老猪支持你攒钱！这样就能买更多好吃的了！"
        ));
        
        return templates;
    }

    /**
     * 获取默认评论模板
     */
    private List<String> getDefaultTemplates(String transactionType) {
        if ("transfer_in".equals(transactionType)) {
            return Arrays.asList(
                "不错！又攒了{amount}元！",
                "继续加油！攒钱大业不能停！",
                "好习惯！坚持就是胜利！"
            );
        } else {
            return Arrays.asList(
                "花钱了呢，记得要理性消费哦～",
                "偶尔花钱犒赏自己也是可以的！",
                "下次花钱前记得想想目标！"
            );
        }
    }

    /**
     * 生成欢迎消息
     */
    private String generateWelcomeMessage(BuddyCharacter buddy) {
        List<String> welcomeMessages = Arrays.asList(
            "哈喽！我是" + buddy.getName() + "，很高兴成为你的攒钱搭子！",
            "欢迎来到搭子攒钱大家庭！我是" + buddy.getName() + "，让我们一起加油吧！",
            "新钱包创建成功！" + buddy.getName() + "已就位，准备和你一起攒大钱！",
            "攒钱小分队集合完毕！" + buddy.getName() + "报到，让我们开始这段有趣的攒钱之旅！"
        );
        
        return welcomeMessages.get(new Random().nextInt(welcomeMessages.size()));
    }

    /**
     * 估算语音时长
     */
    private String estimateVoiceDuration(String content) {
        if (content == null || content.isEmpty()) {
            return "3s";
        }
        
        int length = content.length();
        int seconds = Math.max(3, Math.min((int)Math.ceil(length * 0.2f), 15));
        return seconds + "s";
    }

    /**
     * 记录搭子互动
     */
    private void recordBuddyInteraction(Long walletId, Long characterId, Long transactionId, 
                                       Integer interactionType, String content, String voiceUrl) {
        try {
            BuddyInteraction interaction = new BuddyInteraction();
            interaction.setWalletId(walletId);
            interaction.setCharacterId(characterId);
            interaction.setTransactionId(transactionId);
            interaction.setInteractionType(interactionType);
            interaction.setContent(content);
            interaction.setVoiceUrl(voiceUrl);
            interaction.setVoiceDuration(estimateVoiceDuration(content));
            buddyInteractionMapper.insert(interaction);
        } catch (Exception e) {
            logger.error("记录搭子互动失败", e);
        }
    }
}
