package com.mytravel.album.controller;

import com.mytravel.common.ApiResponse;
import com.mytravel.album.Album;
import com.mytravel.album.AlbumPhoto;
import com.mytravel.album.service.AlbumService;
import com.mytravel.album.dto.CreateAlbumRequest;
import com.mytravel.album.dto.AddPhotoRequest;
import com.mytravel.album.dto.UpdatePrivacyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    /** 创建相册 */
    @PostMapping
    public ApiResponse<Album> create(@RequestBody CreateAlbumRequest request,
                                     Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.ok(albumService.createAlbum(
                userId, request.getRegionId(), request.getTitle(),
                request.getDescription(), request.getCoverImage()));
    }

    /** 我的相册列表 */
    @GetMapping
    public ApiResponse<List<Album>> listMine(Authentication authentication) {
        return ApiResponse.ok(albumService.getMyAlbums(getUserId(authentication)));
    }

    /** 查看某人相册列表（受隐私控制） */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<Album>> listByUser(@PathVariable Long userId,
                                               Authentication authentication) {
        Long viewerId = getViewerId(authentication);
        return ApiResponse.ok(albumService.getUserAlbums(userId, viewerId));
    }

    /** 相册详情（受隐私控制） */
    @GetMapping("/{id}")
    public ApiResponse<Album> detail(@PathVariable Long id,
                                     Authentication authentication) {
        Long viewerId = getViewerId(authentication);
        return ApiResponse.ok(albumService.getAlbumDetail(id, viewerId));
    }

    /** 修改相册 */
    @PutMapping("/{id}")
    public ApiResponse<Album> update(@PathVariable Long id,
                                     @RequestBody CreateAlbumRequest request,
                                     Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.ok(albumService.updateAlbum(
                userId, id, request.getTitle(),
                request.getDescription(), request.getCoverImage()));
    }

    /** 删除相册 */
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id,
                                      Authentication authentication) {
        albumService.deleteAlbum(getUserId(authentication), id);
        return ApiResponse.ok("相册已删除");
    }

    /** 添加照片 */
    @PostMapping("/{id}/photo")
    public ApiResponse<AlbumPhoto> addPhoto(@PathVariable Long id,
                                            @RequestBody AddPhotoRequest request,
                                            Authentication authentication) {
        return ApiResponse.ok(albumService.addPhoto(
                getUserId(authentication), id, request.getUrl(), request.getDescription()));
    }

    /** 更新照片描述 */
    @PutMapping("/{albumId}/photo/{photoId}")
    public ApiResponse<AlbumPhoto> updatePhotoDesc(@PathVariable Long albumId,
                                                    @PathVariable Long photoId,
                                                    @RequestBody AddPhotoRequest request,
                                                    Authentication authentication) {
        return ApiResponse.ok(albumService.updatePhotoDesc(
                getUserId(authentication), albumId, photoId, request.getDescription()));
    }

    /** 删除照片 */
    @DeleteMapping("/{albumId}/photo/{photoId}")
    public ApiResponse<String> removePhoto(@PathVariable Long albumId,
                                           @PathVariable Long photoId,
                                           Authentication authentication) {
        albumService.removePhoto(getUserId(authentication), albumId, photoId);
        return ApiResponse.ok("照片已删除");
    }

    // ==================== 隐私设置 ====================

    /** 查询相册隐私设置 */
    @GetMapping("/{id}/privacy")
    public ApiResponse<Album> getPrivacy(@PathVariable Long id,
                                         Authentication authentication) {
        return ApiResponse.ok(albumService.getPrivacy(getUserId(authentication), id));
    }

    /** 设置单个相册隐私 */
    @PutMapping("/{id}/privacy")
    public ApiResponse<String> setPrivacy(@PathVariable Long id,
                                          @RequestBody UpdatePrivacyRequest request,
                                          Authentication authentication) {
        albumService.setPrivacy(getUserId(authentication), id,
                request.getVisibility(), request.getUserIds());
        return ApiResponse.ok("隐私设置已更新");
    }

    /** 一键设置所有相册隐私 */
    @PutMapping("/privacy/batch")
    public ApiResponse<String> setPrivacyBatch(@RequestBody UpdatePrivacyRequest request,
                                               Authentication authentication) {
        albumService.setPrivacyBatch(getUserId(authentication),
                request.getVisibility(), request.getUserIds());
        return ApiResponse.ok("所有相册的隐私设置已更新");
    }

    // ==================== 辅助方法 ====================

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getDetails();
    }

    private Long getViewerId(Authentication authentication) {
        return authentication != null ? (Long) authentication.getDetails() : null;
    }
}
