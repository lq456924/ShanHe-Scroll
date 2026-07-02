package com.mytravel.bottle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bottle_like",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"bottle_id", "user_id"})})
@Getter
@Setter
public class BottleLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bottleId;

    @Column(nullable = false)
    private Long userId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
