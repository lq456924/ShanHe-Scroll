package com.mytravel.message;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.userId = :userId AND m.isRead = false")
    int markAllAsRead(Long userId);

    /** 检查指定关联对象是否已有某种类型的消息（用于判断是否为重新审核） */
    boolean existsByRelatedIdAndType(Long relatedId, String type);
}
