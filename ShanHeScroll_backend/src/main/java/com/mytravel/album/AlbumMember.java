package com.mytravel.album;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 共同相册成员关系。
 * status: 0=PENDING（待确认） 1=ACCEPTED（已加入） 2=REJECTED（已拒绝）
 */
@Entity
@Table(name = "album_member")
@Getter
@Setter
public class AlbumMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long albumId;

    @Column(nullable = false)
    private Long userId;

    /** 0=待确认  1=已加入  2=已拒绝 */
    @Column(nullable = false)
    private Integer status = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
