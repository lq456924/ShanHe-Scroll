package com.mytravel.album.service;
import com.mytravel.album.AlbumMember;
import com.mytravel.album.AlbumPhoto;
import com.mytravel.album.Album;
import com.mytravel.album.AlbumVisibilityUser;
import com.mytravel.album.repository.AlbumMemberRepository;
import com.mytravel.album.repository.AlbumRepository;
import com.mytravel.album.repository.AlbumPhotoRepository;
import com.mytravel.album.repository.AlbumVisibilityUserRepository;

import com.mytravel.common.FileService;
import com.mytravel.message.MessageService;
import com.mytravel.user.User;
import com.mytravel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumPhotoRepository albumPhotoRepository;
    private final AlbumVisibilityUserRepository visibilityUserRepository;
    private final AlbumMemberRepository albumMemberRepository;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final FileService fileService;

    // ==================== 辅助：判断用户是否有管理权限 ====================

    private void checkAlbumAccess(Long userId, Album album) {
        if (album.getUserId().equals(userId)) return; // 所有者
        if (album.getIsShared() != null && album.getIsShared()) {
            boolean isMember = albumMemberRepository
                    .findByAlbumIdAndUserId(album.getId(), userId)
                    .map(m -> m.getStatus() == 1)
                    .orElse(false);
            if (isMember) return;
        }
        throw new RuntimeException("无权操作此相册");
    }

    // ==================== 相册 CRUD ====================

    @Transactional
    public Album createAlbum(Long userId, Long regionId, String title,
                             String description, String coverImage,
                             Boolean isShared, List<Long> inviteeIds) {
        if (title == null || title.isBlank()) {
            throw new RuntimeException("相册标题不能为空");
        }

        Album album = new Album();
        album.setUserId(userId);
        album.setRegionId(regionId);
        album.setTitle(title);
        album.setDescription(description);
        album.setCoverImage(coverImage);
        album.setVisibility(0);
        album.setIsShared(isShared != null && isShared);

        album = albumRepository.save(album);

        // 多人相册：为每个被邀请者创建待确认成员记录 + 发送通知
        if (album.getIsShared() && inviteeIds != null && !inviteeIds.isEmpty()) {
            User owner = userRepository.findById(userId).orElse(null);
            String ownerName = owner != null
                    ? (owner.getNickname() != null ? owner.getNickname() : owner.getUsername())
                    : "用户" + userId;

            for (Long inviteeId : inviteeIds) {
                if (inviteeId.equals(userId)) continue;
                AlbumMember member = new AlbumMember();
                member.setAlbumId(album.getId());
                member.setUserId(inviteeId);
                member.setStatus(0); // PENDING
                albumMemberRepository.save(member);

                messageService.send(inviteeId, "ALBUM_INVITE",
                        ownerName + " 邀请你共同管理相册「" + album.getTitle() + "」",
                        "点击同意后你将可以添加、管理相册中的照片。",
                        album.getId());
            }
        }

        return album;
    }

    /** 我的相册（自己创建的 + 已加入的共同相册） */
    public List<Album> getMyAlbums(Long userId) {
        List<Album> albums = new ArrayList<>(albumRepository.findByUserIdOrderByCreatedAtDesc(userId));

        // 查找用户已加入的共同相册
        List<AlbumMember> memberships = albumMemberRepository.findByUserIdAndStatus(userId, 1);
        for (AlbumMember m : memberships) {
            albumRepository.findById(m.getAlbumId()).ifPresent(album -> {
                if (!album.getUserId().equals(userId)) {
                    albums.add(album);
                }
            });
        }

        albums.forEach(a -> a.setPhotoCount(albumPhotoRepository.countByAlbumId(a.getId())));
        return albums;
    }

    /** 查看某用户的相册（受隐私控制） */
    public List<Album> getUserAlbums(Long targetUserId, Long viewerId) {
        List<Album> all = albumRepository.findByUserIdOrderByCreatedAtDesc(targetUserId);
        all.forEach(a -> a.setPhotoCount(albumPhotoRepository.countByAlbumId(a.getId())));

        if (viewerId != null && viewerId.equals(targetUserId)) {
            return all;
        }

        return all.stream()
                .filter(a -> canView(a, viewerId))
                .toList();
    }

    /** 相册详情（受隐私控制，共同成员可查看） */
    public Album getAlbumDetail(Long albumId, Long viewerId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));

        // 所有者或共同成员始终可见
        if (viewerId != null) {
            if (viewerId.equals(album.getUserId())) {
                // owner
            } else if (album.getIsShared() != null && album.getIsShared()
                    && albumMemberRepository.findByAlbumIdAndUserId(albumId, viewerId)
                    .map(m -> m.getStatus() == 1).orElse(false)) {
                // shared member
            } else if (!canView(album, viewerId)) {
                throw new RuntimeException("该相册未公开");
            }
        } else {
            if (!canView(album, null)) {
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
        checkAlbumAccess(userId, album);

        if (title != null && !title.isBlank()) album.setTitle(title);
        if (description != null) album.setDescription(description);
        if (coverImage != null) album.setCoverImage(coverImage);

        return albumRepository.save(album);
    }

    @Transactional
    public void deleteAlbum(Long userId, Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        checkAlbumAccess(userId, album);

        List<AlbumPhoto> photos = albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(albumId);
        for (AlbumPhoto photo : photos) {
            fileService.delete(photo.getUrl());
        }

        albumPhotoRepository.deleteByAlbumId(albumId);
        visibilityUserRepository.deleteByAlbumId(albumId);
        albumMemberRepository.deleteByAlbumId(albumId);
        albumRepository.delete(album);
    }

    // ==================== 照片管理 ====================

    public AlbumPhoto addPhoto(Long userId, Long albumId, String url, String description) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        checkAlbumAccess(userId, album);

        List<AlbumPhoto> existing = albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(albumId);

        AlbumPhoto photo = new AlbumPhoto();
        photo.setAlbumId(albumId);
        photo.setUrl(url);
        photo.setDescription(description);
        photo.setSortOrder(existing.size());

        return albumPhotoRepository.save(photo);
    }

    @Transactional
    public void removePhoto(Long userId, Long albumId, Long photoId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        checkAlbumAccess(userId, album);

        AlbumPhoto photo = albumPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("照片不存在"));
        if (!photo.getAlbumId().equals(albumId)) {
            throw new RuntimeException("照片不属于此相册");
        }

        fileService.delete(photo.getUrl());
        albumPhotoRepository.delete(photo);
    }

    public AlbumPhoto updatePhotoDesc(Long userId, Long albumId, Long photoId, String description) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        checkAlbumAccess(userId, album);

        AlbumPhoto photo = albumPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("照片不存在"));
        if (!photo.getAlbumId().equals(albumId)) {
            throw new RuntimeException("照片不属于此相册");
        }
        photo.setDescription(description);
        return albumPhotoRepository.save(photo);
    }

    // ==================== 共同相册：邀请/响应 ====================

    /** 被邀请者同意加入 */
    @Transactional
    public void acceptInvitation(Long userId, Long albumId) {
        AlbumMember member = albumMemberRepository
                .findByAlbumIdAndUserId(albumId, userId)
                .orElseThrow(() -> new RuntimeException("未找到该邀请记录"));

        if (member.getStatus() != 0) {
            throw new RuntimeException("该邀请已处理过");
        }

        member.setStatus(1);
        albumMemberRepository.save(member);

        Album album = albumRepository.findById(albumId).orElse(null);
        User acceptor = userRepository.findById(userId).orElse(null);
        String acceptorName = acceptor != null
                ? (acceptor.getNickname() != null ? acceptor.getNickname() : acceptor.getUsername())
                : "用户" + userId;
        String albumTitle = album != null ? album.getTitle() : "未知相册";

        messageService.send(album.getUserId(), "ALBUM_INVITE_ACCEPTED",
                acceptorName + " 已同意加入相册「" + albumTitle + "」",
                "现在你们可以共同管理这个相册了。",
                albumId);
    }

    /** 被邀请者拒绝加入 */
    @Transactional
    public void rejectInvitation(Long userId, Long albumId) {
        AlbumMember member = albumMemberRepository
                .findByAlbumIdAndUserId(albumId, userId)
                .orElseThrow(() -> new RuntimeException("未找到该邀请记录"));

        if (member.getStatus() != 0) {
            throw new RuntimeException("该邀请已处理过");
        }

        member.setStatus(2);
        albumMemberRepository.save(member);

        Album album = albumRepository.findById(albumId).orElse(null);
        User rejecter = userRepository.findById(userId).orElse(null);
        String rejecterName = rejecter != null
                ? (rejecter.getNickname() != null ? rejecter.getNickname() : rejecter.getUsername())
                : "用户" + userId;
        String albumTitle = album != null ? album.getTitle() : "未知相册";

        messageService.send(album.getUserId(), "ALBUM_INVITE_REJECTED",
                rejecterName + " 已拒绝加入相册「" + albumTitle + "」",
                "你可以重新邀请其他人。",
                albumId);
    }

    // ==================== 隐私设置 ====================

    public Album getPrivacy(Long userId, Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("相册不存在"));
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看隐私设置");
        }
        album.setVisibilityUsers(visibilityUserRepository.findByAlbumId(albumId));
        return album;
    }

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

        visibilityUserRepository.deleteByAlbumId(albumId);
        if ((visibility == 2 || visibility == 3) && userIds != null) {
            for (Long uid : userIds) {
                AlbumVisibilityUser vu = new AlbumVisibilityUser();
                vu.setAlbumId(albumId);
                vu.setUserId(uid);
                vu.setType(visibility == 2 ? 0 : 1);
                visibilityUserRepository.save(vu);
            }
        }
    }

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

    private boolean canView(Album album, Long viewerId) {
        // 共同成员也可查看
        if (viewerId != null && album.getIsShared() != null && album.getIsShared()) {
            boolean isMember = albumMemberRepository
                    .findByAlbumIdAndUserId(album.getId(), viewerId)
                    .map(m -> m.getStatus() == 1).orElse(false);
            if (isMember) return true;
        }

        int v = album.getVisibility() != null ? album.getVisibility() : 0;

        return switch (v) {
            case 0 -> true;
            case 1 -> false;
            case 2 -> {
                if (viewerId == null) yield false;
                List<AlbumVisibilityUser> list = visibilityUserRepository.findByAlbumId(album.getId());
                yield list.stream().anyMatch(u -> u.getType() == 0 && u.getUserId().equals(viewerId));
            }
            case 3 -> {
                if (viewerId == null) yield true;
                List<AlbumVisibilityUser> list = visibilityUserRepository.findByAlbumId(album.getId());
                yield list.stream().noneMatch(u -> u.getType() == 1 && u.getUserId().equals(viewerId));
            }
            default -> false;
        };
    }
}
