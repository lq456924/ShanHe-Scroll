package com.mytravel.travel.controller;

import com.mytravel.common.ApiResponse;
import com.mytravel.travel.Attraction;
import com.mytravel.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 需要登录的旅游相关接口 */
@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelUserController {

    private final TravelService travelService;

    /** 用户提交景点（默认未审核） */
    @PostMapping("/attractions")
    public ApiResponse<Attraction> submit(@RequestBody Attraction data,
                                          Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.ok(travelService.submitAttraction(userId, data));
    }

    /** 我提交的景点列表 */
    @GetMapping("/my-attractions")
    public ApiResponse<List<Attraction>> mySubmissions(Authentication authentication) {
        return ApiResponse.ok(travelService.getMySubmissions(getUserId(authentication)));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getDetails();
    }
}
