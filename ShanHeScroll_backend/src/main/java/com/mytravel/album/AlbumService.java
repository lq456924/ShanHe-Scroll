package com.mytravel.album;

import com.mytravel.common.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumPhotoRepository albumPhotoRepository;
    private final AlbumVisibilityUserRepository visibilityUserRepository;
    private final FileService fileService;

    // ==================== 相册 CRUD ====================

    public Album createAlbum(Long userId, Long regionId, String title,
                             String description, String coverImage) {
        if (title == null || title.isBlank()) {
            throw new RuntimeException("相册标题不能为空");
        }

        Album album = new Album();
        album.setUserId(userId);
        album.setRegionId(regionId);
        album.setTitle(title);
        album.setDescription(description);
        album.setCoverImage(coverImage);
        album.setVisibility(0); // 默认公开

        return albumRepository.save(album);
    }

    /** 我的相册（全部，不过滤） */
    public List<Album> getMyAlbums(Long userId) {
        List<Album> albums = albumRepository.findByUserIdOrderByCreatedAtDesc(userId);
        albums.forEach(a -> a.setPhotoCount(albumPhotoRepository.countByAlbumId(a.getId())));
        return albums;
    }

    /** 查看某用户的相册（受隐私控制） */
    public List<Album> getUserAlbums(Long targetUserId, Long viewerId) {
        List<Album> all = albumRepository.findByUserIdOrderByCreatedAtDesc(targetUserId);
        all.forEach(a -> a.setPhotoCount(albumPhotoRepository.countByAlbumId(a.getId())));

        // 查看自己的相册 → 全部返回
        if (viewerId != null && viewerId.equals(targetUserId)) {
            return all;
        }

        // 非所有者 → 过滤不可见相册
        return all.stream()
                .filter(a -> canView(a, viewerId))
                .toList();
    }

    /** 相册详情（受隐私控制） */
    public Album getAlbumDetail(Long albumId, Long viewerId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));

        // 所有者始终可见
        if (viewerId == null || !viewerId.equals(album.getUserId())) {
            if (!canView(album, viewerId)) {
                throw new RuntimeException("该相册未公开");
            }
        }

        album.setPhotos(albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(albumId));
        return album;
    }

    public Album updateAlbum(Long userId, Long albumId, String title,
                             String description, String coverImage) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此相册");
        }

        if (title != null && !title.isBlank()) album.setTitle(title);
        if (description != null) album.setDescription(description);
        if (coverImage != null) album.setCoverImage(coverImage);

        return albumRepository.save(album);
    }

    @Transactional
    public void deleteAlbum(Long userId, Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此相册");
        }

        List<AlbumPhoto> photos = albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(albumId);
        for (AlbumPhoto photo : photos) {
            fileService.delete(photo.getUrl());
        }

        albumPhotoRepository.deleteByAlbumId(albumId);
        visibilityUserRepository.deleteByAlbumId(albumId);
        albumRepository.delete(album);
    }

    // ==================== 照片管理 ====================

    public AlbumPhoto addPhoto(Long userId, Long albumId, String url, String description) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此相册");
        }

        List<AlbumPhoto> existing = albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(albumId);

        AlbumPhoto photo = new AlbumPhoto();
        photo.setAlbumId(albumId);
        photo.setUrl(url);
        photo.setDescription(description);
        photo.setSortOrder(existing.size());

        return albumPhotoRepository.save(photo);
    }

    public void removePhoto(Long userId, Long albumId, Long photoId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此相册");
        }

        AlbumPhoto photo = albumPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("照片不存在"));
        if (!photo.getAlbumId().equals(albumId)) {
            throw new RuntimeException("照片不属于此相册");
        }

        fileService.delete(photo.getUrl());
        albumPhotoRepository.delete(photo);
    }

    /** 更新照片描述 */
    public AlbumPhoto updatePhotoDesc(Long userId, Long albumId, Long photoId, String description) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此相册");
        }
        AlbumPhoto photo = albumPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("照片不存在"));
        if (!photo.getAlbumId().equals(albumId)) {
            throw new RuntimeException("照片不属于此相册");
        }
        photo.setDescription(description);
        return albumPhotoRepository.save(photo);
    }

    // ==================== 隐私设置 ====================

    /** 查询相册的隐私设置 */
    public Album getPrivacy(Long userId, Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看隐私设置");
        }
        album.setVisibilityUsers(visibilityUserRepository.findByAlbumId(albumId));
        return album;
    }

    /** 对单个相册设置隐私 */
    @Transactional
    public void setPrivacy(Long userId, Long albumId, Integer visibility, List<Long> userIds) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改隐私设置");
        }
        if (visibility < 0 || visibility > 3) {
            throw new RuntimeException("无效的可见性类型，请使用 0-3");
        }

        album.setVisibility(visibility);
        albumRepository.save(album);

        // 重建白名单/黑名单
        visibilityUserRepository.deleteByAlbumId(albumId);
        if ((visibility == 2 || visibility == 3) && userIds != null) {
            for (Long uid : userIds) {
                AlbumVisibilityUser vu = new AlbumVisibilityUser();
                vu.setAlbumId(albumId);
                vu.setUserId(uid);
                vu.setType(visibility == 2 ? 0 : 1); // 0=白名单 1=黑名单
                visibilityUserRepository.save(vu);
            }
        }
    }

    /** 一键对所有相册设置隐私 */
    @Transactional
    public void setPrivacyBatch(Long userId, Integer visibility, List<Long> userIds) {
        if (visibility < 0 || visibility > 3) {
            throw new RuntimeException("无效的可见性类型，请使用 0-3");
        }

        List<Album> albums = albumRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Album album : albums) {
            album.setVisibility(visibility);
            albumRepository.save(album);

            visibilityUserRepository.deleteByAlbumId(album.getId());
            if ((visibility == 2 || visibility == 3) && userIds != null) {
                for (Long uid : userIds) {
                    AlbumVisibilityUser vu = new AlbumVisibilityUser();
                    vu.setAlbumId(album.getId());
                    vu.setUserId(uid);
                    vu.setType(visibility == 2 ? 0 : 1);
                    visibilityUserRepository.save(vu);
                }
            }
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 判断 viewer 是否可以查看该相册。
     */
    private boolean canView(Album album, Long viewerId) {
        int v = album.getVisibility() != null ? album.getVisibility() : 0;

        return switch (v) {
            case 0 -> true; // 所有人可见
            case 1 -> false; // 仅自己可见（非所有者已在此前过滤）
            case 2 -> { // 白名单
                if (viewerId == null) yield false;
                List<AlbumVisibilityUser> list = visibilityUserRepository.findByAlbumId(album.getId());
                yield list.stream().anyMatch(u -> u.getType() == 0 && u.getUserId().equals(viewerId));
            }
            case 3 -> { // 黑名单
                if (viewerId == null) yield true;
                List<AlbumVisibilityUser> list = visibilityUserRepository.findByAlbumId(album.getId());
                yield list.stream().noneMatch(u -> u.getType() == 1 && u.getUserId().equals(viewerId));
            }
            default -> false;
        };
    }
}
