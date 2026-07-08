package com.mytravel.travel.repository;
import com.mytravel.travel.AttractionImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttractionImageRepository extends JpaRepository<AttractionImage, Long> {

    List<AttractionImage> findByAttractionIdOrderBySortOrder(Long attractionId);
}
