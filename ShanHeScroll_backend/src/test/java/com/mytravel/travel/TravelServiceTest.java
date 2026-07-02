package com.mytravel.travel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private AttractionRepository attractionRepository;

    @Mock
    private AttractionImageRepository attractionImageRepository;

    // ---- 地区查询 ----

    @Test
    void shouldGetRegionsByLevel() {
        Region beijing = new Region();
        beijing.setId(1L);
        beijing.setName("北京");
        beijing.setLevel(2);

        when(regionRepository.findByLevelOrderBySortOrder(2)).thenReturn(List.of(beijing));

        TravelService service = new TravelService(regionRepository, attractionRepository, attractionImageRepository);
        List<Region> result = service.getRegionsByLevel(2);

        assertEquals(1, result.size());
        assertEquals("北京", result.get(0).getName());
    }

    @Test
    void shouldGetRegionsByParentId() {
        Region chaoyang = new Region();
        chaoyang.setId(100L);
        chaoyang.setName("朝阳区");
        chaoyang.setParentId(1L);

        when(regionRepository.findByParentIdOrderBySortOrder(1L)).thenReturn(List.of(chaoyang));

        TravelService service = new TravelService(regionRepository, attractionRepository, attractionImageRepository);
        List<Region> result = service.getRegionsByParentId(1L);

        assertEquals(1, result.size());
        assertEquals("朝阳区", result.get(0).getName());
    }

    // ---- 景点查询 ----

    @Test
    void shouldGetAttractionsByRegion() {
        Attraction a = new Attraction();
        a.setId(1L);
        a.setName("故宫");
        a.setRegionId(1L);

        when(attractionRepository.findByRegionIdAndReviewed(eq(1L), eq(1))).thenReturn(List.of(a));

        TravelService service = new TravelService(regionRepository, attractionRepository, attractionImageRepository);
        List<Attraction> result = service.getAttractionsByRegionId(1L, "default");

        assertEquals(1, result.size());
        assertEquals("故宫", result.get(0).getName());
    }

    @Test
    void shouldGetAttractionByIdWithImages() {
        Attraction a = new Attraction();
        a.setId(1L);
        a.setName("故宫");

        AttractionImage img = new AttractionImage();
        img.setId(1L);
        img.setUrl("/photos/gugong.jpg");

        when(attractionRepository.findById(1L)).thenReturn(Optional.of(a));
        when(attractionImageRepository.findByAttractionIdOrderBySortOrder(1L))
                .thenReturn(List.of(img));

        TravelService service = new TravelService(regionRepository, attractionRepository, attractionImageRepository);
        Attraction result = service.getAttractionById(1L);

        assertEquals("故宫", result.getName());
        assertEquals(1, result.getImages().size());
    }

    @Test
    void shouldThrowWhenAttractionNotFound() {
        when(attractionRepository.findById(999L)).thenReturn(Optional.empty());

        TravelService service = new TravelService(regionRepository, attractionRepository, attractionImageRepository);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAttractionById(999L));
        assertTrue(ex.getMessage().contains("景点不存在"));
    }
}
