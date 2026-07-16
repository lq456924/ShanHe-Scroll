package com.mytravel.message;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收者用户ID */
    @Column(nullable = false)
    private Long userId;

    /**
     * 消息类型：
     * BAN / UNBAN / BOTTLE_APPROVED / BOTTLE_REJECTED / BOTTLE_REPORTED
     * / BOTTLE_RE_APPROVED / BOTTLE_RE_REJECTED / ATTRACTION_APPROVED / ATTRACTION_REJECTED
     */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** 关联业务对象ID（如漂流瓶ID、打卡点ID），可为空 */
    private Long relatedId;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
