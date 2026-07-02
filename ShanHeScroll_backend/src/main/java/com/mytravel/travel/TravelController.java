package com.mytravel.travel;

import com.mytravel.common.ApiResponse;
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
            @RequestParam(required = false, defaultValue = "default") String sortBy) {
        return ApiResponse.ok(travelService.getAttractionsByRegionId(regionId, sortBy));
    }

    @GetMapping("/attractions/{id}")
    public ApiResponse<Attraction> getAttraction(@PathVariable Long id) {
        return ApiResponse.ok(travelService.getAttractionById(id));
    }
}
