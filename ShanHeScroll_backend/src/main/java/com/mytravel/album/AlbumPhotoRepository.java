package com.mytravel.album;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumPhotoRepository extends JpaRepository<AlbumPhoto, Long> {

    /** 某相册的所有照片，按排序字段升序 */
    List<AlbumPhoto> findByAlbumIdOrderBySortOrderAsc(Long albumId);

    /** 某相册的照片数量 */
    long countByAlbumId(Long albumId);

    /** 删除某相册的所有照片 */
    void deleteByAlbumId(Long albumId);
}
