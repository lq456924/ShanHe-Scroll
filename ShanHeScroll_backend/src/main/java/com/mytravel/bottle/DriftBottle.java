package com.mytravel.bottle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "drift_bottle")
@Getter
@Setter
public class DriftBottle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发送者用户ID */
    @Column(nullable = false)
    private Long senderId;

    /** 是否匿名: 0=实名  1=匿名 */
    @Column(nullable = false)
    private Integer isAnonymous = 0;

    /** 文字内容 */
    @Column(columnDefinition = "TEXT")
    private String textContent;

    /** 图片URL */
    @Column(length = 500)
    private String imageUrl;

    /** 状态: 0=待审核  1=审核通过(可被拾取)  2=审核拒绝  3=已被拾取 */
    @Column(nullable = false)
    private Integer status = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 点赞数（不持久化，查询时填充） */
    @Transient
    private Long likeCount;

    /** 发送者信息（不持久化，拾取时按需填充） */
    @Transient
    private SenderInfo sender;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** 拾取时展示的发送者信息 */
    @Getter
    @Setter
    public static class SenderInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
    }
}
