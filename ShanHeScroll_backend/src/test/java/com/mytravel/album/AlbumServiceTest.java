package com.mytravel.album;

import com.mytravel.album.repository.AlbumRepository;
import com.mytravel.album.repository.AlbumPhotoRepository;
import com.mytravel.album.repository.AlbumVisibilityUserRepository;
import com.mytravel.album.service.AlbumService;
import com.mytravel.common.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private AlbumPhotoRepository albumPhotoRepository;

    @Mock
    private AlbumVisibilityUserRepository visibilityUserRepository;

    @Mock
    private FileService fileService;

    private AlbumService albumService;

    @BeforeEach
    void setUp() {
        albumService = new AlbumService(albumRepository, albumPhotoRepository,
                visibilityUserRepository, fileService);
    }

    // ---- 创建相册 ----

    @Test
    void shouldCreateAlbumWithDefaultPublic() {
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        Album album = albumService.createAlbum(1L, 110000L, "北京之旅",
                "一次难忘的旅行", "/uploads/cover.jpg");

        assertEquals(1L, album.getId());
        assertEquals(0, album.getVisibility()); // 默认公开
    }

    @Test
    void shouldRejectEmptyTitle() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                albumService.createAlbum(1L, 110000L, "", null, null));
        assertTrue(ex.getMessage().contains("标题不能为空"));
    }

    // ---- 隐私：公开可见 ----

    @Test
    void ownerCanAlwaysSeeOwnAlbum() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);
        album.setVisibility(1); // 仅自己可见

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(1L)).thenReturn(List.of());

        Album result = albumService.getAlbumDetail(1L, 1L); // 所有者查看
        assertNotNull(result);
    }

    @Test
    void publicAlbumVisibleToAnyone() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);
        album.setVisibility(0); // 公开

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(1L)).thenReturn(List.of());

        Album result = albumService.getAlbumDetail(1L, 99L); // 路人查看
        assertNotNull(result);
    }

    @Test
    void privateAlbumHiddenFromOthers() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);
        album.setVisibility(1); // 仅自己

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));

        assertThrows(RuntimeException.class,
                () -> albumService.getAlbumDetail(1L, 99L)); // 路人查看
    }

    // ---- 隐私：白名单 ----

    @Test
    void whitelistUserCanView() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);
        album.setVisibility(2); // 白名单

        AlbumVisibilityUser vu = new AlbumVisibilityUser();
        vu.setUserId(99L);
        vu.setType(0); // 白名单

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(visibilityUserRepository.findByAlbumId(1L)).thenReturn(List.of(vu));
        when(albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(1L)).thenReturn(List.of());

        Album result = albumService.getAlbumDetail(1L, 99L); // 白名单用户
        assertNotNull(result);
    }

    @Test
    void nonWhitelistUserCannotView() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);
        album.setVisibility(2);

        AlbumVisibilityUser vu = new AlbumVisibilityUser();
        vu.setUserId(42L);
        vu.setType(0);

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(visibilityUserRepository.findByAlbumId(1L)).thenReturn(List.of(vu));

        assertThrows(RuntimeException.class,
                () -> albumService.getAlbumDetail(1L, 99L)); // 不在白名单
    }

    // ---- 隐私：黑名单 ----

    @Test
    void blacklistedUserCannotView() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);
        album.setVisibility(3); // 黑名单

        AlbumVisibilityUser vu = new AlbumVisibilityUser();
        vu.setUserId(99L);
        vu.setType(1); // 黑名单

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(visibilityUserRepository.findByAlbumId(1L)).thenReturn(List.of(vu));

        assertThrows(RuntimeException.class,
                () -> albumService.getAlbumDetail(1L, 99L)); // 被拉黑
    }

    @Test
    void nonBlacklistedUserCanView() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);
        album.setVisibility(3);

        AlbumVisibilityUser vu = new AlbumVisibilityUser();
        vu.setUserId(42L);
        vu.setType(1); // 拉黑的是别人

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(visibilityUserRepository.findByAlbumId(1L)).thenReturn(List.of(vu));
        when(albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(1L)).thenReturn(List.of());

        Album result = albumService.getAlbumDetail(1L, 99L); // 未被拉黑
        assertNotNull(result);
    }

    // ---- 权限校验 ----

    @Test
    void shouldRejectUpdateByNonOwner() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(42L);

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                albumService.updateAlbum(99L, 1L, "new title", null, null));
        assertTrue(ex.getMessage().contains("无权修改"));
    }

    // ---- 删除级联 ----

    @Test
    void shouldDeleteAlbumAndPhotosAndVisibility() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);

        AlbumPhoto photo = new AlbumPhoto();
        photo.setId(10L);
        photo.setUrl("/uploads/test.jpg");

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(albumPhotoRepository.findByAlbumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(photo));

        albumService.deleteAlbum(1L, 1L);

        verify(fileService).delete("/uploads/test.jpg");
        verify(albumPhotoRepository).deleteByAlbumId(1L);
        verify(visibilityUserRepository).deleteByAlbumId(1L);
        verify(albumRepository).delete(album);
    }

    // ---- 隐私设置 ----

    @Test
    void shouldSetWhitelistPrivacy() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);
        album.setVisibility(0);

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));

        albumService.setPrivacy(1L, 1L, 2, List.of(100L, 200L));

        assertEquals(2, album.getVisibility());
        verify(visibilityUserRepository).deleteByAlbumId(1L);
        verify(visibilityUserRepository, times(2)).save(any(AlbumVisibilityUser.class));
    }

    @Test
    void shouldSetPrivacyBatch() {
        Album a1 = new Album(); a1.setId(1L); a1.setUserId(1L);
        Album a2 = new Album(); a2.setId(2L); a2.setUserId(1L);

        when(albumRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(a1, a2));

        albumService.setPrivacyBatch(1L, 1, null); // 全部改为仅自己

        assertEquals(1, a1.getVisibility());
        assertEquals(1, a2.getVisibility());
        verify(visibilityUserRepository, times(2)).deleteByAlbumId(anyLong());
    }

    @Test
    void shouldRejectInvalidVisibility() {
        Album album = new Album();
        album.setId(1L);
        album.setUserId(1L);

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                albumService.setPrivacy(1L, 1L, 9, null));
        assertTrue(ex.getMessage().contains("无效的可见性类型"));
    }
}
