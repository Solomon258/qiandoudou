package com.qiandoudou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandoudou.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;

import java.util.List;
import java.util.Map;

/**
 * 通知消息 Mapper 接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 获取用户的互动消息列表
     */
    @Select("SELECT n.id, n.user_id, n.sender_id, CAST(n.type AS UNSIGNED) as type, n.title, n.content, " +
            "n.related_id, n.wallet_id, n.transaction_id, n.post_image, " +
            "n.is_read, n.create_time, n.update_time, " +
            "COALESCE(u.nickname, bc.name, ap_sender.name) as sender_nickname, " +
            "COALESCE(u.avatar, bc.avatar, ap_sender.avatar) as sender_avatar, " +
            "w.name as wallet_name, " +
            "t.ai_partner_id, t.ai_partner_name, " +
            "COALESCE(t.ai_partner_avatar, ap_trans.avatar) as ai_partner_avatar " +
            "FROM notifications n " +
            "LEFT JOIN users u ON n.sender_id = u.id " +
            "LEFT JOIN buddy_characters bc ON n.sender_id = bc.id " +
            "LEFT JOIN ai_partners ap_sender ON n.sender_id = ap_sender.id " +
            "LEFT JOIN wallets w ON n.wallet_id = w.id " +
            "LEFT JOIN transactions t ON n.transaction_id = t.id " +
            "LEFT JOIN ai_partners ap_trans ON t.ai_partner_id = ap_trans.id " +
            "WHERE n.user_id = #{userId} AND n.deleted = 0 " +
            "ORDER BY n.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<Map<String, Object>> getUserInteractionMessages(@Param("userId") Long userId,
                                                        @Param("offset") Integer offset,
                                                        @Param("pageSize") Integer pageSize);

    /**
     * 获取用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM notifications " +
            "WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    Integer getUnreadMessageCount(@Param("userId") Long userId);

    /**
     * 标记用户的所有消息为已读
     */
    @Update("UPDATE notifications SET is_read = 1, update_time = NOW() " +
            "WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    void markMessagesAsRead(@Param("userId") Long userId);

    /**
     * 创建互动通知
     */
    @Insert("INSERT INTO notifications (user_id, sender_id, type, title, content, related_id, wallet_id, transaction_id, post_image, is_read, create_time) " +
            "VALUES (#{userId}, #{senderId}, #{type}, #{title}, #{content}, #{relatedId}, #{walletId}, #{transactionId}, #{postImage}, 0, NOW())")
    void createInteractionNotification(@Param("userId") Long userId,
                                     @Param("senderId") Long senderId,
                                     @Param("type") Integer type,
                                     @Param("title") String title,
                                     @Param("content") String content,
                                     @Param("relatedId") Long relatedId,
                                     @Param("walletId") Long walletId,
                                     @Param("transactionId") Long transactionId,
                                     @Param("postImage") String postImage);
}
