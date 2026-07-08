package com.mytravel.travel.service;
import com.mytravel.album.repository.AlbumRepository;
import com.mytravel.travel.Region;
import com.mytravel.travel.AttractionImage;
import com.mytravel.travel.Attraction;
import com.mytravel.travel.dto.SearchResult;
import com.mytravel.travel.dto.SiteStats;
import com.mytravel.travel.repository.RegionRepository;
import com.mytravel.travel.repository.AttractionRepository;
import com.mytravel.travel.repository.AttractionImageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelService {

    private final RegionRepository regionRepository;
    private final AttractionRepository attractionRepository;
    private final AttractionImageRepository attractionImageRepository;
    private final AlbumRepository albumRepository;

    public List<Region> getRegionsByLevel(Integer level) {
        return regionRepository.findByLevelOrderBySortOrder(level);
    }

    public List<Region> getRegionsByParentId(Long parentId) {
        return regionRepository.findByParentIdOrderBySortOrder(parentId);
    }

    /** 公开查询：只返回审核通过的景点。category 为空时查全部分类。 */
    public List<Attraction> getAttractionsByRegionId(Long regionId, String sortBy, String category) {
        boolean hasCategory = category != null && !category.isEmpty();
        if ("likes".equals(sortBy)) {
            return hasCategory
                    ? attractionRepository.findByRegionIdAndReviewedAndCategory(regionId, 1, category,
                            Sort.by(Sort.Direction.DESC, "likeCount"))
                    : attractionRepository.findByRegionIdAndReviewed(regionId, 1,
                            Sort.by(Sort.Direction.DESC, "likeCount"));
        }
        return hasCategory
                ? attractionRepository.findByRegionIdAndReviewedAndCategory(regionId, 1, category)
                : attractionRepository.findByRegionIdAndReviewed(regionId, 1);
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

    /** 用户提交景点（默认未审核，提交前去重） */
    public Attraction submitAttraction(Long userId, Attraction data) {
        if (attractionRepository.existsByRegionIdAndName(data.getRegionId(), data.getName())) {
            throw new RuntimeException("该城市已存在同名景点，请勿重复提交");
        }
        Attraction a = new Attraction();
        a.setRegionId(data.getRegionId());
        a.setName(data.getName());
        a.setDescription(data.getDescription());
        a.setImage(data.getImage());
        a.setAddress(data.getAddress());
        a.setRating(data.getRating());
        a.setRecommendMonth(data.getRecommendMonth());
        a.setTips(data.getTips());
        a.setCategory(data.getCategory() != null ? data.getCategory() : "attraction");
        a.setReviewed(0);      // 默认未审核
        a.setSubmitterId(userId);
        a.setLikeCount(0);
        return attractionRepository.save(a);
    }

    /** 我提交的景点列表 */
    public List<Attraction> getMySubmissions(Long userId) {
        return attractionRepository.findBySubmitterIdOrderByReviewedAsc(userId);
    }

    // ========== 搜索 ==========

    public List<SearchResult> search(String keyword, String category) {
        List<SearchResult> results = new ArrayList<>();
        String kw = keyword.trim();
        if (kw.isEmpty()) return results;

        boolean hasCategory = category != null && !category.isEmpty();

        // 1. 搜索城市（level=3 的 region）
        List<Region> cityMatches = regionRepository.findByLevelAndNameContaining(3, kw);
        List<Long> parentIds = cityMatches.stream()
                .map(Region::getParentId).distinct().toList();
        Map<Long, String> parentNameMap = regionRepository.findAllById(parentIds).stream()
                .collect(Collectors.toMap(Region::getId, Region::getName));

        for (Region city : cityMatches) {
            SearchResult r = new SearchResult();
            r.setType("city");
            r.setId(city.getId());
            r.setName(city.getName());
            r.setRegionId(city.getId());
            r.setRegionName(city.getName());
            r.setParentId(city.getParentId());
            r.setParentName(parentNameMap.getOrDefault(city.getParentId(), ""));
            results.add(r);
        }

        // 2. 搜索景点/打卡项（审核通过，支持分类过滤）
        List<Attraction> attractionMatches = hasCategory
                ? attractionRepository.findByReviewedAndNameContainingAndCategory(1, kw, category)
                : attractionRepository.findByReviewedAndNameContaining(1, kw);
        if (!attractionMatches.isEmpty()) {
            List<Long> cityIds = attractionMatches.stream()
                    .map(Attraction::getRegionId).distinct().toList();
            Map<Long, Region> cityMap = regionRepository.findAllById(cityIds).stream()
                    .collect(Collectors.toMap(Region::getId, r -> r));
            List<Long> pIds = cityMap.values().stream()
                    .map(Region::getParentId).distinct().toList();
            Map<Long, String> provNameMap = regionRepository.findAllById(pIds).stream()
                    .collect(Collectors.toMap(Region::getId, Region::getName));

            for (Attraction a : attractionMatches) {
                Region city = cityMap.get(a.getRegionId());
                SearchResult r = new SearchResult();
                r.setType("attraction");
                r.setId(a.getId());
                r.setName(a.getName());
                r.setCategory(a.getCategory());
                if (city != null) {
                    r.setRegionId(city.getId());
                    r.setRegionName(city.getName());
                    r.setParentId(city.getParentId());
                    r.setParentName(provNameMap.getOrDefault(city.getParentId(), ""));
                }
                results.add(r);
            }
        }

        return results;
    }

    // ========== 统计 ==========

    public SiteStats getStats() {
        SiteStats s = new SiteStats();
        s.setProvinces(regionRepository.countByLevel(2));
        s.setCities(regionRepository.countByLevel(3));
        s.setAttractions(attractionRepository.countByReviewed(1));
        s.setAlbums(albumRepository.count());
        return s;
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
