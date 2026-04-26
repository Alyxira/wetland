package com.example.demo.repository;

import com.example.demo.entity.TiffImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TiffImageRepository extends JpaRepository<TiffImage, Long> {
    
    @Query("SELECT t FROM TiffImage t LEFT JOIN t.region r " +
           "WHERE r.regionName = :regionName ORDER BY t.uploadTime DESC")
    List<TiffImage> findByRegionNameOrderByUploadTimeDesc(@Param("regionName") String regionName);
    
    List<TiffImage> findByAcquisitionDateBetweenOrderByAcquisitionDateAsc(
        LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT t FROM TiffImage t WHERE " +
           "t.minLon <= :maxLon AND t.maxLon >= :minLon AND " +
           "t.minLat <= :maxLat AND t.maxLat >= :minLat " +
           "ORDER BY t.acquisitionDate DESC")
    List<TiffImage> findByBoundingBox(
        @Param("minLon") BigDecimal minLon,
        @Param("maxLon") BigDecimal maxLon,
        @Param("minLat") BigDecimal minLat,
        @Param("maxLat") BigDecimal maxLat);
    
    @Query("SELECT DISTINCT r.regionName FROM TiffImage t JOIN t.region r ORDER BY r.regionName ASC")
    List<String> findAllRegions();

    @Modifying
    @Query("UPDATE TiffImage t SET t.region = null WHERE t.region.id = :regionId")
    int clearRegionByRegionId(@Param("regionId") Long regionId);
    
    List<TiffImage> findAllByOrderByUploadTimeDesc();

    List<TiffImage> findByWetlandIdOrderByAcquisitionDateDescUploadTimeDesc(Long wetlandId);

    Optional<TiffImage> findByFilePath(String filePath);
}

