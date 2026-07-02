package com.mytravel.album;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "album")
@Getter
@Setter
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 */
    @Column(nullable = false)
    private Long userId;

    /** 关联地区（城市/区） */
    @Column(nullable = false)
    private Long regionId;

    /** 相册标题 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 相册描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 封面图片 URL */
    @Column(length = 500)
    private String coverImage;

    /** 可见性: 0=所有人可见  1=仅自己可见  2=白名单可见  3=黑名单不可见 */
    @Column(nullable = false)
    private Integer visibility = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 相册内的照片列表（不持久化，由 Service 填充） */
    @Transient
    private List<AlbumPhoto> photos;

    /** 照片数量（不持久化，列表查询时填充） */
    @Transient
    private Long photoCount;

    /** 可见性用户列表（不持久化，查询隐私设置时填充） */
    @Transient
    private List<AlbumVisibilityUser> visibilityUsers;

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
