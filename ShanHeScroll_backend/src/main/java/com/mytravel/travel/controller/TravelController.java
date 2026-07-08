package com.mytravel.travel.controller;

import com.mytravel.common.ApiResponse;
import com.mytravel.travel.Region;
import com.mytravel.travel.Attraction;
import com.mytravel.travel.dto.SearchResult;
import com.mytravel.travel.dto.SiteStats;
import com.mytravel.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    @GetMapping("/regions")
    public ApiResponse<List<Region>> getRegions(
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Long parentId) {
        if (level != null) {
            return ApiResponse.ok(travelService.getRegionsByLevel(level));
        }
        if (parentId != null) {
            return ApiResponse.ok(travelService.getRegionsByParentId(parentId));
        }
        throw new RuntimeException("请提供 level 或 parentId 参数");
    }

    @GetMapping("/attractions")
    public ApiResponse<List<Attraction>> getAttractions(
            @RequestParam Long regionId,
            @RequestParam(required = false, defaultValue = "default") String sortBy,
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(travelService.getAttractionsByRegionId(regionId, sortBy, category));
    }

    @GetMapping("/stats")
    public ApiResponse<SiteStats> getStats() {
        return ApiResponse.ok(travelService.getStats());
    }

    @GetMapping("/search")
    public ApiResponse<List<SearchResult>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(travelService.search(keyword, category));
    }

    @GetMapping("/attractions/{id}")
    public ApiResponse<Attraction> getAttraction(@PathVariable Long id) {
        return ApiResponse.ok(travelService.getAttractionById(id));
    }
}
