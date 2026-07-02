package com.mytravel.album;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "album_visibility_user")
@Getter
@Setter
public class AlbumVisibilityUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long albumId;

    @Column(nullable = false)
    private Long userId;

    /** 0=白名单  1=黑名单 */
    @Column(nullable = false)
    private Integer type;
}
