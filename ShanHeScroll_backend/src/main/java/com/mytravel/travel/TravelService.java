package com.mytravel.travel;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelService {

    private final RegionRepository regionRepository;
    private final AttractionRepository attractionRepository;
    private final AttractionImageRepository attractionImageRepository;

    public List<Region> getRegionsByLevel(Integer level) {
        return regionRepository.findByLevelOrderBySortOrder(level);
    }

    public List<Region> getRegionsByParentId(Long parentId) {
        return regionRepository.findByParentIdOrderBySortOrder(parentId);
    }

    /** 公开查询：只返回审核通过的景点 */
    public List<Attraction> getAttractionsByRegionId(Long regionId, String sortBy) {
        if ("likes".equals(sortBy)) {
            return attractionRepository.findByRegionIdAndReviewed(regionId, 1,
                    Sort.by(Sort.Direction.DESC, "likeCount"));
        }
        return attractionRepository.findByRegionIdAndReviewed(regionId, 1);
    }

    public Attraction getAttractionById(Long id) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("景点不存在"));
        attraction.setImages(attractionImageRepository.findByAttractionIdOrderBySortOrder(id));
        return attraction;
    }

    public List<AttractionImage> getAttractionImages(Long attractionId) {
        return attractionImageRepository.findByAttractionIdOrderBySortOrder(attractionId);
    }

    /** 用户提交景点（默认未审核） */
    public Attraction submitAttraction(Long userId, Attraction data) {
        Attraction a = new Attraction();
        a.setRegionId(data.getRegionId());
        a.setName(data.getName());
        a.setDescription(data.getDescription());
        a.setImage(data.getImage());
        a.setAddress(data.getAddress());
        a.setRating(data.getRating());
        a.setRecommendMonth(data.getRecommendMonth());
        a.setTips(data.getTips());
        a.setReviewed(0);      // 默认未审核
        a.setSubmitterId(userId);
        a.setLikeCount(0);
        return attractionRepository.save(a);
    }

    /** 我提交的景点列表 */
    public List<Attraction> getMySubmissions(Long userId) {
        return attractionRepository.findBySubmitterIdOrderByReviewedAsc(userId);
    }

    // ========== 审核（管理员用） ==========

    public List<Attraction> getPendingAttractions() {
        return attractionRepository.findByReviewedOrderByIdDesc(0);
    }

    public void approveAttraction(Long id) {
        Attraction a = attractionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("景点不存在"));
        a.setReviewed(1);
        attractionRepository.save(a);
    }

    public void rejectAttraction(Long id) {
        Attraction a = attractionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("景点不存在"));
        a.setReviewed(2);
        attractionRepository.save(a);
    }
}
