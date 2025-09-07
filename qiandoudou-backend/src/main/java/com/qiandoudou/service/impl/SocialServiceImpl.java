package com.qiandoudou.service.impl;

import com.qiandoudou.entity.PostLike;
import com.qiandoudou.entity.UserFollow;
import com.qiandoudou.entity.PostComment;
import com.qiandoudou.mapper.NotificationMapper;
import com.qiandoudou.mapper.PostLikeMapper;
import com.qiandoudou.mapper.UserFollowMapper;
import com.qiandoudou.mapper.PostCommentMapper;
import com.qiandoudou.mapper.TransactionMapper;
import com.qiandoudou.mapper.WalletMapper;
import com.qiandoudou.mapper.WalletViewMapper;
import com.qiandoudou.service.AiPartnerService;
import com.qiandoudou.entity.WalletView;
import com.qiandoudou.service.SocialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 社交功能服务实现
 */
@Service
public class SocialServiceImpl implements SocialService {
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    @Autowired
    private PostLikeMapper postLikeMapper;
    
    @Autowired
    private UserFollowMapper userFollowMapper;
    
    @Autowired
    private TransactionMapper transactionMapper;
    
    @Autowired
    private WalletMapper walletMapper;
    
    @Autowired
    private PostCommentMapper postCommentMapper;
    
    @Autowired
    private WalletViewMapper walletViewMapper;
    
    @Autowired
    private AiPartnerService aiPartnerService;

    @Override
    public Map<String, Object> getUserSocialStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取真实的粉丝数量
            Integer fansCount = userFollowMapper.getUserFansCount(userId);
            
            // 获取真实的关注数量
            Integer followingCount = userFollowMapper.getUserFollowingCount(userId);
            
            // 获取真实的获赞数量
            Integer likesCount = postLikeMapper.getUserTotalLikes(userId);
            
            // 浏览数使用真实数据，新用户为0（后续可以添加真实统计）
            int viewsCount = 0;
            
            stats.put("fansCount", fansCount != null ? fansCount : 0);
            stats.put("likesCount", likesCount != null ? likesCount : 0);
            stats.put("viewsCount", viewsCount);
            stats.put("followingCount", followingCount != null ? followingCount : 0);
            
            System.out.println("用户 " + userId + " 的真实社交统计: 粉丝" + fansCount + ", 获赞" + likesCount + ", 关注" + followingCount);
            
        } catch (Exception e) {
            System.err.println("获取用户社交统计失败: " + e.getMessage());
            e.printStackTrace();
            
            // 如果出错，返回默认数据
            stats.put("fansCount", 0);
            stats.put("likesCount", 0);
            stats.put("viewsCount", 0);
            stats.put("followingCount", 0);
        }
        
        return stats;
    }

    @Override
    @Transactional
    public void followWallet(Long userId, Long walletId) {
        try {
            // 获取钱包所有者ID
            Long walletOwnerId = this.getWalletOwnerId(walletId);
            if (walletOwnerId == null) {
                throw new RuntimeException("钱包不存在");
            }
            
            if (walletOwnerId.equals(userId)) {
                throw new RuntimeException("不能关注自己的钱包");
            }
            
            // 1. 检查是否已经关注该钱包
            Integer existingFollow = userFollowMapper.checkUserFollowed(userId, walletId);
            if (existingFollow > 0) {
                throw new RuntimeException("已关注该钱包");
            }
            
            // 2. 在user_follows表中插入关注记录
            UserFollow userFollow = new UserFollow();
            userFollow.setFollowerId(userId);
            userFollow.setWalletId(walletId);
            userFollowMapper.insert(userFollow);
            
            // 3. 创建关注通知
            System.out.println("准备创建关注通知: 关注者=" + userId + ", 被关注者=" + walletOwnerId + ", 钱包=" + walletId);
            this.createFollowNotification(userId, walletOwnerId, walletId);
            
            System.out.println("用户 " + userId + " 成功关注了钱包 " + walletId + " 的所有者 " + walletOwnerId);
        } catch (Exception e) {
            System.err.println("关注失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("关注失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void unfollowWallet(Long userId, Long walletId) {
        try {
            // 获取钱包所有者ID
            Long walletOwnerId = this.getWalletOwnerId(walletId);
            if (walletOwnerId == null) {
                throw new RuntimeException("钱包不存在");
            }
            
            // 1. 检查是否已经关注
            Integer existingFollow = userFollowMapper.checkUserFollowed(userId, walletId);
            if (existingFollow == 0) {
                throw new RuntimeException("未关注该钱包");
            }
            
            // 2. 删除关注记录（软删除）
            userFollowMapper.removeFollow(userId, walletId);
            
            System.out.println("用户 " + userId + " 成功取消关注了钱包 " + walletId + " 的所有者 " + walletOwnerId);
        } catch (Exception e) {
            System.err.println("取消关注失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("取消关注失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void likeTransaction(Long userId, Long transactionId) {
        try {
            // 1. 检查是否已经点赞
            Integer existingLike = postLikeMapper.checkUserLiked(transactionId, userId);
            if (existingLike > 0) {
                // 如果已经点赞，则取消点赞
                this.unlikeTransaction(userId, transactionId);
                System.out.println("用户 " + userId + " 取消点赞了交易 " + transactionId);
                return;
            }
            
            // 2. 使用UPSERT方法，原子性地插入或激活点赞记录
            PostLike postLike = new PostLike();
            postLike.setPostId(transactionId);
            postLike.setUserId(userId);
            postLike.setIsAiLike(0);
            postLike.setCreateTime(LocalDateTime.now());
            
            // 使用UPSERT，如果记录已存在则激活它
            int affected = postLikeMapper.upsertLike(postLike);
            
            if (affected > 0) {
                // 3. 创建点赞通知
                this.createLikeNotification(userId, transactionId);
                System.out.println("用户 " + userId + " 成功点赞了交易 " + transactionId);
            } else {
                System.out.println("用户 " + userId + " 点赞交易 " + transactionId + " 无变化");
            }
            
        } catch (Exception e) {
            System.err.println("点赞失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("点赞失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void unlikeTransaction(Long userId, Long transactionId) {
        try {
            // 1. 检查是否已经点赞
            Integer existingLike = postLikeMapper.checkUserLiked(transactionId, userId);
            if (existingLike == 0) {
                throw new RuntimeException("用户未点赞该交易");
            }
            
            // 2. 删除点赞记录（软删除）
            postLikeMapper.removeLike(transactionId, userId);
            
            System.out.println("用户 " + userId + " 成功取消点赞了交易 " + transactionId);
        } catch (Exception e) {
            System.err.println("取消点赞失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("取消点赞失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> commentTransaction(Long userId, Long transactionId, String content) {
        try {
            System.out.println("开始处理评论: userId=" + userId + ", transactionId=" + transactionId + ", content=" + content);
            
            // 1. 在post_comments表中插入评论记录
            PostComment postComment = new PostComment();
            postComment.setPostId(transactionId);
            postComment.setUserId(userId);
            postComment.setContent(content);
            postComment.setIsAiComment(0);
            
            postCommentMapper.insert(postComment);
            System.out.println("评论记录插入成功，ID: " + postComment.getId());
            
            // 2. 创建评论通知
            this.createCommentNotification(userId, transactionId, content);
            
            // 3. 返回新创建的评论信息
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", postComment.getId());
            comment.put("userId", userId);
            comment.put("user_id", userId);
            comment.put("transactionId", transactionId);
            comment.put("post_id", transactionId);
            comment.put("content", content);
            comment.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            comment.put("create_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 添加用户信息（从当前用户获取）
            comment.put("user_nickname", "当前用户"); // 这里应该查询真实用户信息
            comment.put("user_avatar", "");
            
            System.out.println("用户 " + userId + " 成功评论了交易 " + transactionId + ": " + content);
            
            return comment;
        } catch (Exception e) {
            System.err.println("评论失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("评论失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> aiCommentTransaction(Long transactionId, String content, Long aiPartnerId, String voiceUrl) {
        try {
            System.out.println("开始处理AI评论: aiPartnerId=" + aiPartnerId + ", transactionId=" + transactionId + ", content=" + content);
            
            // 获取AI伴侣信息
            com.qiandoudou.entity.AiPartner aiPartner = aiPartnerService.getById(aiPartnerId);
            if (aiPartner == null) {
                throw new RuntimeException("AI伴侣不存在");
            }
            
            // 1. 在post_comments表中插入AI评论记录
            PostComment postComment = new PostComment();
            postComment.setPostId(transactionId);
            postComment.setUserId(aiPartnerId); // 使用AI伴侣ID作为用户ID
            postComment.setContent(content);
            postComment.setIsAiComment(1); // 标记为AI评论
            
            postCommentMapper.insert(postComment);
            System.out.println("AI评论记录插入成功，ID: " + postComment.getId());
            
            // 2. 返回新创建的AI评论信息
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", postComment.getId());
            comment.put("userId", aiPartnerId);
            comment.put("user_id", aiPartnerId);
            comment.put("transactionId", transactionId);
            comment.put("post_id", transactionId);
            comment.put("content", content);
            comment.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            comment.put("create_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            comment.put("isAiComment", true);
            comment.put("voiceUrl", voiceUrl);
            
            // 添加AI伴侣信息
            comment.put("user_nickname", aiPartner.getName());
            comment.put("userName", aiPartner.getName());
            comment.put("user_avatar", aiPartner.getAvatar());
            comment.put("userAvatar", aiPartner.getAvatar());
            
            System.out.println("AI伴侣 " + aiPartner.getName() + " 成功评论了交易 " + transactionId + ": " + content);
            
            return comment;
        } catch (Exception e) {
            System.err.println("AI评论失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("AI评论失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getTransactionComments(Long transactionId) {
        try {
            System.out.println("查询交易 " + transactionId + " 的评论列表");
            
            // 从数据库查询真实的评论数据
            List<Map<String, Object>> comments = postCommentMapper.getTransactionComments(transactionId);
            
            // 处理评论数据格式
            for (Map<String, Object> comment : comments) {
                // 检查是否为AI评论
                Object isAiCommentObj = comment.get("is_ai_comment");
                boolean isAiComment = isAiCommentObj != null && (Integer.valueOf(isAiCommentObj.toString()) == 1);
                comment.put("isAiComment", isAiComment);
                
                if (isAiComment) {
                    // 如果是AI评论，获取AI伴侣信息
                    Long aiPartnerId = Long.valueOf(comment.get("user_id").toString());
                    try {
                        com.qiandoudou.entity.AiPartner aiPartner = aiPartnerService.getById(aiPartnerId);
                        if (aiPartner != null) {
                            comment.put("user_nickname", aiPartner.getName());
                            comment.put("userName", aiPartner.getName());
                            comment.put("user_avatar", aiPartner.getAvatar());
                            comment.put("userAvatar", aiPartner.getAvatar());
                            // 添加语音URL（如果有的话）
                            comment.put("voiceUrl", comment.get("voice_url"));
                        }
                    } catch (Exception e) {
                        System.err.println("获取AI伴侣信息失败: " + e.getMessage());
                    }
                } else {
                    // 普通用户评论，使用原有逻辑
                    comment.put("userName", comment.get("user_nickname"));
                }
                
                // 处理时间格式
                Object createTimeObj = comment.get("create_time");
                if (createTimeObj != null) {
                    comment.put("createTime", createTimeObj.toString());
                }
                
                System.out.println("评论数据: " + comment);
            }
            
            System.out.println("获取交易 " + transactionId + " 的评论，返回 " + comments.size() + " 条真实数据");
            
            return comments;
        } catch (Exception e) {
            System.err.println("查询评论失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getUserInteractionMessages(Long userId, Integer page, Integer pageSize) {
        try {
            // 计算偏移量
            Integer offset = (page - 1) * pageSize;
            
            // 从数据库查询用户的互动消息
            List<Map<String, Object>> messages = notificationMapper.getUserInteractionMessages(userId, offset, pageSize);
            
            // 处理消息数据，添加必要的字段转换
            for (Map<String, Object> message : messages) {
                System.out.println("处理消息数据: " + message);
                
                // 处理消息类型显示
                Object typeObj = message.get("type");
                Integer type = 0;
                if (typeObj instanceof Boolean) {
                    // 如果是Boolean类型，转换为Integer
                    type = ((Boolean) typeObj) ? 1 : 0;
                } else if (typeObj instanceof Integer) {
                    type = (Integer) typeObj;
                } else if (typeObj != null) {
                    // 尝试解析字符串
                    try {
                        type = Integer.parseInt(typeObj.toString());
                    } catch (NumberFormatException e) {
                        type = 0;
                    }
                }
                
                String typeText = "";
                switch (type) {
                    case 1:
                        typeText = "like";
                        break;
                    case 2:
                        typeText = "comment";
                        break;
                    case 3:
                        typeText = "follow";
                        break;
                    default:
                        typeText = "system";
                        break;
                }
                message.put("type", typeText); // 前端使用的是type字段
                
                // 处理用户信息
                Map<String, Object> user = new HashMap<>();
                user.put("id", message.get("sender_id"));
                user.put("nickname", message.get("sender_nickname"));
                user.put("avatar", message.get("sender_avatar"));
                message.put("user", user);
                
                // 处理时间格式
                Object createTimeObj = message.get("create_time");
                if (createTimeObj != null) {
                    String timeStr = createTimeObj.toString();
                    message.put("createdAt", timeStr);
                    message.put("timestamp", timeStr);
                    System.out.println("时间字段: " + timeStr);
                }
                
                // 处理动态图片
                String postImage = (String) message.get("post_image");
                message.put("postImage", postImage);
                
                System.out.println("处理后的消息: type=" + typeText + ", user=" + message.get("user") + ", content=" + message.get("content"));
            }
            
            System.out.println("获取用户 " + userId + " 的互动消息，第 " + page + " 页，返回 " + messages.size() + " 条");
            return messages;
            
        } catch (Exception e) {
            System.err.println("查询互动消息失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public Integer getUnreadMessageCount(Long userId) {
        try {
            // 查询用户未读消息数量
            Integer count = notificationMapper.getUnreadMessageCount(userId);
            System.out.println("用户 " + userId + " 的未读消息数量: " + count);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("查询未读消息数量失败: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void markMessagesAsRead(Long userId) {
        try {
            // 标记用户的所有未读消息为已读
            notificationMapper.markMessagesAsRead(userId);
            System.out.println("用户 " + userId + " 的消息已标记为已读");
        } catch (Exception e) {
            System.err.println("标记消息已读失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建点赞通知
     */
    private void createLikeNotification(Long likerId, Long transactionId) {
        try {
            // 首先需要获取交易信息，找到交易的所有者
            // 这里需要查询transactions表获取交易详情
            // 暂时使用模拟逻辑
            
            // 假设我们能获取到交易所有者ID和相关信息
            Long ownerId = this.getTransactionOwnerId(transactionId);
            if (ownerId != null && !ownerId.equals(likerId)) {
                // 创建点赞通知
                notificationMapper.createInteractionNotification(
                    ownerId,           // 接收者（交易所有者）
                    likerId,           // 发送者（点赞者）
                    1,                 // 类型：1-点赞
                    "收到新点赞",        // 标题
                    "给你点赞",         // 内容
                    null,              // 关联ID
                    null,              // 钱包ID（可选）
                    transactionId,     // 交易ID
                    this.getTransactionImage(transactionId)  // 交易图片
                );
                
                System.out.println("创建点赞通知：用户 " + likerId + " 点赞了用户 " + ownerId + " 的交易 " + transactionId);
            }
        } catch (Exception e) {
            System.err.println("创建点赞通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建评论通知
     */
    private void createCommentNotification(Long commenterId, Long transactionId, String content) {
        try {
            // 获取交易所有者ID
            Long ownerId = this.getTransactionOwnerId(transactionId);
            if (ownerId != null && !ownerId.equals(commenterId)) {
                // 创建评论通知
                notificationMapper.createInteractionNotification(
                    ownerId,           // 接收者（交易所有者）
                    commenterId,       // 发送者（评论者）
                    2,                 // 类型：2-评论
                    "收到新评论",        // 标题
                    content,           // 评论内容
                    null,              // 关联ID
                    null,              // 钱包ID（可选）
                    transactionId,     // 交易ID
                    this.getTransactionImage(transactionId)  // 交易图片
                );
                
                System.out.println("创建评论通知：用户 " + commenterId + " 评论了用户 " + ownerId + " 的交易 " + transactionId);
            }
        } catch (Exception e) {
            System.err.println("创建评论通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建关注通知
     */
    private void createFollowNotification(Long followerId, Long followingId, Long walletId) {
        try {
            System.out.println("开始创建关注通知:");
            System.out.println("  接收者ID: " + followingId);
            System.out.println("  发送者ID: " + followerId);
            System.out.println("  钱包ID: " + walletId);
            
            notificationMapper.createInteractionNotification(
                followingId,       // 接收者（被关注者）
                followerId,        // 发送者（关注者）
                3,                 // 类型：3-关注
                "新关注",           // 标题
                "关注了你",         // 内容
                null,              // 关联ID
                walletId,          // 钱包ID
                null,              // 交易ID
                null               // 动态图片（关注类型无图片）
            );
            
            System.out.println("关注通知创建成功：用户 " + followerId + " 关注了用户 " + followingId);
        } catch (Exception e) {
            System.err.println("创建关注通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取交易所有者ID
     */
    private Long getTransactionOwnerId(Long transactionId) {
        try {
            return transactionMapper.getTransactionOwnerId(transactionId);
        } catch (Exception e) {
            System.err.println("获取交易所有者ID失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取交易图片
     */
    private String getTransactionImage(Long transactionId) {
        try {
            String imageUrl = transactionMapper.getTransactionImage(transactionId);
            return imageUrl != null ? imageUrl : "/images/img/bg.png";
        } catch (Exception e) {
            System.err.println("获取交易图片失败: " + e.getMessage());
            return "/images/img/bg.png";
        }
    }

    /**
     * 获取钱包所有者ID
     */
    private Long getWalletOwnerId(Long walletId) {
        try {
            return walletMapper.getWalletOwnerId(walletId);
        } catch (Exception e) {
            System.err.println("获取钱包所有者ID失败: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Boolean checkUserFollowStatus(Long currentUserId, Long targetUserId) {
        try {
            System.out.println("检查关注状态: currentUserId=" + currentUserId + ", targetUserId=" + targetUserId);
            
            Integer followCount = userFollowMapper.checkUserFollowed(currentUserId, targetUserId);
            System.out.println("查询结果: followCount=" + followCount);
            
            boolean isFollowing = followCount != null && followCount > 0;
            System.out.println("最终关注状态: " + isFollowing);
            
            return isFollowing;
        } catch (Exception e) {
            System.err.println("检查关注状态失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<String, Object> getTransactionSocialData(Long transactionId, Long userId) {
        Map<String, Object> socialData = new HashMap<>();
        
        try {
            // 获取点赞数量
            Integer likeCount = postLikeMapper.getTransactionLikeCount(transactionId);
            
            // 获取评论数量
            Integer commentCount = postCommentMapper.getTransactionCommentCount(transactionId);
            
            // 检查用户是否已点赞（如果提供了userId）
            boolean isLiked = false;
            if (userId != null) {
                Integer userLiked = postLikeMapper.checkUserLiked(transactionId, userId);
                isLiked = userLiked != null && userLiked > 0;
            }
            
            socialData.put("likeCount", likeCount != null ? likeCount : 0);
            socialData.put("commentCount", commentCount != null ? commentCount : 0);
            socialData.put("isLiked", isLiked);
            
            System.out.println("交易 " + transactionId + " 的社交数据: 点赞" + likeCount + ", 评论" + commentCount + ", 用户已点赞" + isLiked);
            
        } catch (Exception e) {
            System.err.println("获取交易社交数据失败: " + e.getMessage());
            e.printStackTrace();
            
            // 返回默认数据
            socialData.put("likeCount", 0);
            socialData.put("commentCount", 0);
            socialData.put("isLiked", false);
        }
        
        return socialData;
    }

    @Override
    public Map<String, Object> getWalletSocialStats(Long walletId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取钱包的粉丝数量
            Integer fansCount = userFollowMapper.getWalletFansCount(walletId);
            
            // 获取钱包的获赞数量
            Integer likesCount = postLikeMapper.getWalletTotalLikes(walletId);
            
            // 获取钱包的浏览数量
            Integer viewsCount = walletViewMapper.getWalletViewCount(walletId);
            
            stats.put("fansCount", fansCount != null ? fansCount : 0);
            stats.put("likesCount", likesCount != null ? likesCount : 0);
            stats.put("viewsCount", viewsCount != null ? viewsCount : 0);
            
            System.out.println("钱包 " + walletId + " 的社交统计: 粉丝" + fansCount + ", 获赞" + likesCount + ", 浏览" + viewsCount);
            
        } catch (Exception e) {
            System.err.println("获取钱包社交统计失败: " + e.getMessage());
            e.printStackTrace();
            
            // 如果出错，返回默认数据
            stats.put("fansCount", 0);
            stats.put("likesCount", 0);
            stats.put("viewsCount", 0);
        }
        
        return stats;
    }

    @Override
    @Transactional
    public void recordWalletView(Long userId, Long walletId) {
        try {
            // 检查是否已经浏览过（同一用户同一钱包只记录一次）
            Integer existingView = walletViewMapper.checkUserViewed(userId, walletId);
            if (existingView > 0) {
                return; // 已经浏览过，不重复记录
            }
            
            // 记录浏览
            WalletView walletView = new WalletView();
            walletView.setUserId(userId);
            walletView.setWalletId(walletId);
            walletViewMapper.insert(walletView);
            
            System.out.println("用户 " + userId + " 浏览了钱包 " + walletId);
        } catch (Exception e) {
            System.err.println("记录钱包浏览失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}