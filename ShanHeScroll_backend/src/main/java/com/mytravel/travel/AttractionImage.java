package com.mytravel.travel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attraction_image")
@Getter
@Setter
public class AttractionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long attractionId;

    @Column(nullable = false, length = 500)
    private String url;

    private Integer sortOrder;
}
