package com.example.demo.repository;

import com.example.demo.entity.HistoricalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoricalDataRepository extends JpaRepository<HistoricalData, Long> {
    
    List<HistoricalData> findByRegionNameOrderByDataDateAsc(String regionName);

    List<HistoricalData> findByRegionNameAndDataDateBetweenOrderByDataDateAsc(
        String regionName, LocalDateTime startDate, LocalDateTime endDate
    );

    List<HistoricalData> findByRegionIdOrderByDataDateAsc(Long regionId);

    List<HistoricalData> findByRegionIdAndDataDateBetweenOrderByDataDateAsc(
        Long regionId, LocalDateTime startDate, LocalDateTime endDate
    );

    List<HistoricalData> findByRegionIdAndTiffImageIdAndDataDateBetweenOrderByDataDateAsc(
        Long regionId, Long tiffImageId, LocalDateTime startDate, LocalDateTime endDate
    );

    List<HistoricalData> findByWetlandIdOrderByDataDateDesc(Long wetlandId);
    
    @Query("SELECT h FROM HistoricalData h WHERE " +
           "h.dataDate BETWEEN :startDate AND :endDate " +
           "ORDER BY h.dataDate ASC")
    List<HistoricalData> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT h FROM HistoricalData h WHERE " +
           "h.centerLon BETWEEN :minLon AND :maxLon AND " +
           "h.centerLat BETWEEN :minLat AND :maxLat " +
           "ORDER BY h.dataDate ASC")
    List<HistoricalData> findByLocation(
        @Param("minLon") BigDecimal minLon,
        @Param("maxLon") BigDecimal maxLon,
        @Param("minLat") BigDecimal minLat,
        @Param("maxLat") BigDecimal maxLat);
    
    @Query("SELECT DISTINCT h.regionName FROM HistoricalData h ORDER BY h.regionName ASC")
    List<String> findAllRegionNames();

    @Modifying
    @Query("UPDATE HistoricalData h SET h.region = null WHERE h.region.id = :regionId")
    int clearRegionByRegionId(@Param("regionId") Long regionId);

    boolean existsByTiffImageIdAndRegionIdAndDataSource(Long tiffImageId, Long regionId, String dataSource);

    boolean existsByTiffImageIdAndRegionNameAndDataSource(Long tiffImageId, String regionName, String dataSource);
}

