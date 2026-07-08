package com.mytravel.travel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "attraction")
@Getter
@Setter
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long regionId;

    @Column(nullable = false, length = 100)
    private String name;

    /** 分类：attraction=景点 food=美食 photo_spot=拍照留影（可扩展） */
    @Column(nullable = false, length = 20)
    private String category = "attraction";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String image;

    @Column(length = 200)
    private String address;

    private BigDecimal rating;

    @Column(length = 100)
    private String recommendMonth;

    @Column(length = 500)
    private String tips;

    private Integer likeCount;

    /** 审核状态：0=未审核  1=审核通过  2=审核拒绝 */
    @Column(nullable = false)
    private Integer reviewed = 0;

    /** 提交者用户ID（用户自主添加时填充，系统预置为null） */
    private Long submitterId;

    @Transient
    private List<AttractionImage> images;
}
