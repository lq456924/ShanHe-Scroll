package com.mytravel.travel.repository;
import com.mytravel.travel.Region;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findByLevelOrderBySortOrder(Integer level);

    List<Region> findByParentIdOrderBySortOrder(Long parentId);

    /** 按名称模糊搜索城市（level=3） */
    List<Region> findByLevelAndNameContaining(Integer level, String name);

    long countByLevel(Integer level);
}
