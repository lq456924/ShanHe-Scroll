package com.mytravel.bottle;

import com.mytravel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bottle")
@RequiredArgsConstructor
public class DriftBottleController {

    private final DriftBottleService bottleService;

    /** 发送漂流瓶 */
    @PostMapping("/send")
    public ApiResponse<DriftBottle> send(@RequestBody SendBottleRequest request,
                                         Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.ok(bottleService.sendBottle(
                userId, request.getTextContent(),
                request.getImageUrl(), request.getIsAnonymous()));
    }

    /** 随机拾取一个漂流瓶 */
    @GetMapping("/pick")
    public ApiResponse<DriftBottle> pick(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.ok(bottleService.pickBottle(userId));
    }

    /** 点赞 */
    @PostMapping("/{id}/like")
    public ApiResponse<String> like(@PathVariable Long id,
                                    Authentication authentication) {
        bottleService.likeBottle(getUserId(authentication), id);
        return ApiResponse.ok("点赞成功");
    }

    /** 删除自己发出的漂流瓶 */
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id,
                                      Authentication authentication) {
        bottleService.deleteBottle(getUserId(authentication), id);
        return ApiResponse.ok("漂流瓶已删除");
    }

    /** 取消点赞 */
    @DeleteMapping("/{id}/like")
    public ApiResponse<String> unlike(@PathVariable Long id,
                                      Authentication authentication) {
        bottleService.unlikeBottle(getUserId(authentication), id);
        return ApiResponse.ok("已取消点赞");
    }

    /** 我的漂流瓶 */
    @GetMapping("/my")
    public ApiResponse<List<DriftBottle>> myBottles(Authentication authentication) {
        return ApiResponse.ok(bottleService.getMyBottles(getUserId(authentication)));
    }

    /** 我点赞过的漂流瓶 */
    @GetMapping("/liked")
    public ApiResponse<List<DriftBottle>> myLikedBottles(Authentication authentication) {
        return ApiResponse.ok(bottleService.getMyLikedBottles(getUserId(authentication)));
    }

    /** 举报漂流瓶 */
    @PostMapping("/{id}/report")
    public ApiResponse<String> report(@PathVariable Long id,
                                       Authentication authentication) {
        bottleService.reportBottle(getUserId(authentication), id);
        return ApiResponse.ok("已举报，等待审核处理");
    }

    /** 漂流瓶详情（含是否已点赞） */
    @GetMapping("/{id}")
    public ApiResponse<BottleDetailResponse> detail(@PathVariable Long id,
                                                    Authentication authentication) {
        Long userId = getUserId(authentication);
        DriftBottle bottle = bottleService.getBottleDetail(id);
        boolean liked = bottleService.isLikedByUser(id, userId);

        BottleDetailResponse resp = new BottleDetailResponse();
        resp.setBottle(bottle);
        resp.setLiked(liked);
        return ApiResponse.ok(resp);
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getDetails();
    }
}
