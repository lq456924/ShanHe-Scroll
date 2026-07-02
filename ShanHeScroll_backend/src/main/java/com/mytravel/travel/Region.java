package com.mytravel.travel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "region")
@Getter
@Setter
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String image;

    private Integer sortOrder;
}
