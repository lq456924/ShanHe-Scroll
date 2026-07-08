package com.mytravel.travel.repository;
import com.mytravel.travel.Attraction;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    // ===== 无分类过滤（兼容旧逻辑，查全部） =====

    /** 公开查询：只返回审核通过的 */
    List<Attraction> findByRegionIdAndReviewed(Long regionId, Integer reviewed);

    List<Attraction> findByRegionIdAndReviewed(Long regionId, Integer reviewed, Sort sort);

    /** 按提交者查询（用户查看自己提交的） */
    List<Attraction> findBySubmitterIdOrderByReviewedAsc(Long submitterId);

    /** 按审核状态查询（管理员用） */
    List<Attraction> findByReviewedOrderByIdDesc(Integer reviewed);

    /** 按名称模糊搜索已审核通过的景点 */
    List<Attraction> findByReviewedAndNameContaining(Integer reviewed, String name);

    long countByReviewed(Integer reviewed);

    /** 检查同城市下是否已有同名景点 */
    boolean existsByRegionIdAndName(Long regionId, String name);

    // ===== 带分类过滤 =====

    List<Attraction> findByRegionIdAndReviewedAndCategory(Long regionId, Integer reviewed, String category);

    List<Attraction> findByRegionIdAndReviewedAndCategory(Long regionId, Integer reviewed, String category, Sort sort);

    List<Attraction> findByReviewedAndNameContainingAndCategory(Integer reviewed, String name, String category);

    List<Attraction> findByReviewedAndCategory(Integer reviewed, String category);

    long countByReviewedAndCategory(Integer reviewed, String category);
}
