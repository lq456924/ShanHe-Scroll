package com.mytravel.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(length = 500)
    private String avatar;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    private Integer gender;

    private LocalDate birthday;

    @Column(length = 200)
    private String bio;

    @Column(length = 100)
    private String location;

    private Integer status;

    /** 角色: 0=普通用户  1=审核员  2=管理员 */
    @Column(nullable = false)
    private Integer role = 0;

    private Integer tokenVersion;

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
