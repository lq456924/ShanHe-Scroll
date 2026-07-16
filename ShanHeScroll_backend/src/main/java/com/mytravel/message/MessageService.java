package com.mytravel.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    /** 向指定用户发送一条消息 */
    public Message send(Long userId, String type, String title, String content, Long relatedId) {
        Message msg = new Message();
        msg.setUserId(userId);
        msg.setType(type);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRelatedId(relatedId);
        msg.setIsRead(false);
        return messageRepository.save(msg);
    }

    /** 获取用户的消息列表（按时间倒序） */
    public List<Message> getMessages(Long userId) {
        return messageRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** 获取用户未读消息数 */
    public long getUnreadCount(Long userId) {
        return messageRepository.countByUserIdAndIsReadFalse(userId);
    }

    /** 将用户所有未读消息标记为已读 */
    @Transactional
    public int markAllAsRead(Long userId) {
        return messageRepository.markAllAsRead(userId);
    }

    /** 将单条消息标记为已读 */
    @Transactional
    public void markAsRead(Long messageId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            msg.setIsRead(true);
            messageRepository.save(msg);
        });
    }
}
