package com.mytravel.bottle.repository;
import com.mytravel.bottle.DriftBottle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriftBottleRepository extends JpaRepository<DriftBottle, Long> {

    /** 随机获取一个审核通过、未被拾取的漂流瓶 */
    @Query(value = "SELECT * FROM drift_bottle WHERE status = 1 ORDER BY RAND() LIMIT 1",
           nativeQuery = true)
    Optional<DriftBottle> findRandomApproved();

    /** 我的漂流瓶（按时间倒序） */
    List<DriftBottle> findBySenderIdOrderByCreatedAtDesc(Long senderId);

    /** 审核通过的漂流瓶数量 */
    long countByStatus(Integer status);

    /** 按状态查询（审核列表用） */
    List<DriftBottle> findByStatusOrderByCreatedAtDesc(Integer status);

    /** 查询某用户点赞过的所有漂流瓶 */
    @Query(value = "SELECT b.* FROM drift_bottle b JOIN bottle_like l ON b.id = l.bottle_id WHERE l.user_id = ?1 ORDER BY l.created_at DESC", nativeQuery = true)
    List<DriftBottle> findLikedByUserId(Long userId);
}
