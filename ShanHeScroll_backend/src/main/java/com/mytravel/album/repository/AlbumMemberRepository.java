package com.mytravel.album.repository;

import com.mytravel.album.AlbumMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumMemberRepository extends JpaRepository<AlbumMember, Long> {

    List<AlbumMember> findByAlbumId(Long albumId);

    Optional<AlbumMember> findByAlbumIdAndUserId(Long albumId, Long userId);

    /** 用户已加入的共同相册成员记录 */
    List<AlbumMember> findByUserIdAndStatus(Long userId, Integer status);

    long countByAlbumIdAndStatus(Long albumId, Integer status);

    void deleteByAlbumId(Long albumId);
}
