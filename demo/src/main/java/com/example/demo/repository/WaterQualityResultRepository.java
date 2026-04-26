package com.example.demo.repository;

import com.example.demo.entity.WaterQualityResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WaterQualityResultRepository extends JpaRepository<WaterQualityResult, Long> {
    
    List<WaterQualityResult> findByTiffImageIdOrderByCreateTimeDesc(Long tiffImageId);
    
    @Query("SELECT w FROM WaterQualityResult w WHERE " +
           "w.minLon <= :maxLon AND w.maxLon >= :minLon AND " +
           "w.minLat <= :maxLat AND w.maxLat >= :minLat " +
           "ORDER BY w.createTime DESC")
    List<WaterQualityResult> findByBoundingBox(
        @Param("minLon") BigDecimal minLon,
        @Param("maxLon") BigDecimal maxLon,
        @Param("minLat") BigDecimal minLat,
        @Param("maxLat") BigDecimal maxLat);
    
    @Query("SELECT w FROM WaterQualityResult w JOIN w.tiffImage t WHERE " +
           "t.acquisitionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.acquisitionDate ASC")
    List<WaterQualityResult> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    List<WaterQualityResult> findByWetlandIdOrderByCreateTimeDesc(Long wetlandId);

    @Modifying
    @Query("UPDATE WaterQualityResult w SET w.region = null WHERE w.region.id = :regionId")
    int clearRegionByRegionId(@Param("regionId") Long regionId);
}

