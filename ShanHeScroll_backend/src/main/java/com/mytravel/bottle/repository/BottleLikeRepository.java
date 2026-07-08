package com.mytravel.bottle.repository;
import com.mytravel.bottle.BottleLike;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BottleLikeRepository extends JpaRepository<BottleLike, Long> {

    /** 某漂流瓶的点赞数 */
    long countByBottleId(Long bottleId);

    /** 某用户是否已点赞 */
    Optional<BottleLike> findByBottleIdAndUserId(Long bottleId, Long userId);

    /** 某用户是否已点赞 */
    boolean existsByBottleIdAndUserId(Long bottleId, Long userId);

    /** 删除漂流瓶的所有点赞 */
    void deleteByBottleId(Long bottleId);
}
