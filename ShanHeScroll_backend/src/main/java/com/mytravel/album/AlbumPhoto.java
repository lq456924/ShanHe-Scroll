package com.mytravel.album;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "album_photo")
@Getter
@Setter
public class AlbumPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属相册 */
    @Column(nullable = false)
    private Long albumId;

    /** 图片 URL（来自文件上传接口的返回值） */
    @Column(nullable = false, length = 500)
    private String url;

    /** 图片描述 */
    @Column(length = 200)
    private String description;

    /** 排序 */
    private Integer sortOrder;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
