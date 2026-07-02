package com.mytravel.album;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumVisibilityUserRepository extends JpaRepository<AlbumVisibilityUser, Long> {

    /** 某相册的可见性用户列表 */
    List<AlbumVisibilityUser> findByAlbumId(Long albumId);

    /** 删除某相册的所有可见性设置 */
    void deleteByAlbumId(Long albumId);

    /** 某相册的指定类型用户列表 */
    List<AlbumVisibilityUser> findByAlbumIdAndType(Long albumId, Integer type);
}
