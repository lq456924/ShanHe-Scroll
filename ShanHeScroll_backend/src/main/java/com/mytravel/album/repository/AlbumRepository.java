package com.mytravel.album.repository;
import com.mytravel.album.Album;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    /** 某用户的所有相册，按创建时间倒序 */
    List<Album> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 某用户在某地区的相册 */
    List<Album> findByUserIdAndRegionIdOrderByCreatedAtDesc(Long userId, Long regionId);
}
