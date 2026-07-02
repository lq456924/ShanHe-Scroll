package com.mytravel.travel;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    /** 公开查询：只返回审核通过的 */
    List<Attraction> findByRegionIdAndReviewed(Long regionId, Integer reviewed);

    List<Attraction> findByRegionIdAndReviewed(Long regionId, Integer reviewed, Sort sort);

    /** 按提交者查询（用户查看自己提交的） */
    List<Attraction> findBySubmitterIdOrderByReviewedAsc(Long submitterId);

    /** 按审核状态查询（管理员用） */
    List<Attraction> findByReviewedOrderByIdDesc(Integer reviewed);
}
