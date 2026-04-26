package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "water_quality_result",
    indexes = {
        @Index(name = "idx_result_create_time", columnList = "create_time"),
        @Index(name = "idx_result_region", columnList = "region_id"),
        @Index(name = "idx_result_bbox_min", columnList = "min_lon,min_lat"),
        @Index(name = "idx_result_bbox_max", columnList = "max_lon,max_lat"),
        @Index(name = "idx_result_eutrophication", columnList = "eutrophication_level")
    }
)
public class WaterQualityResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tiff_image_id", nullable = false)
    private TiffImage tiffImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "wetland_id")
    private Long wetlandId;
    
    @Column(name = "min_lon", nullable = false, precision = 18, scale = 6)
    private BigDecimal minLon;
    
    @Column(name = "max_lon", nullable = false, precision = 18, scale = 6)
    private BigDecimal maxLon;
    
    @Column(name = "min_lat", nullable = false, precision = 18, scale = 6)
    private BigDecimal minLat;
    
    @Column(name = "max_lat", nullable = false, precision = 18, scale = 6)
    private BigDecimal maxLat;
    
    @Column(name = "geometry_json", columnDefinition = "json")
    private String geometryJson;
    
    @Column(name = "avg_chla", nullable = false, precision = 12, scale = 4)
    private BigDecimal avgChla;
    
    @Column(name = "min_chla", precision = 12, scale = 4)
    private BigDecimal minChla;
    @Column(name = "max_chla", precision = 12, scale = 4)
    private BigDecimal maxChla;
    @Column(name = "std_chla", precision = 12, scale = 4)
    private BigDecimal stdChla;
    
    @Column(name = "avg_spm", nullable = false, precision = 12, scale = 4)
    private BigDecimal avgSpm;
    
    @Column(name = "min_spm", precision = 12, scale = 4)
    private BigDecimal minSpm;
    @Column(name = "max_spm", precision = 12, scale = 4)
    private BigDecimal maxSpm;
    @Column(name = "std_spm", precision = 12, scale = 4)
    private BigDecimal stdSpm;
    
    @Column(name = "avg_turbidity", nullable = false, precision = 12, scale = 4)
    private BigDecimal avgTurbidity;
    
    @Column(name = "min_turbidity", precision = 12, scale = 4)
    private BigDecimal minTurbidity;
    @Column(name = "max_turbidity", precision = 12, scale = 4)
    private BigDecimal maxTurbidity;
    @Column(name = "std_turbidity", precision = 12, scale = 4)
    private BigDecimal stdTurbidity;
    
    @Column(name = "water_pixel_count")
    private Long waterPixelCount;
    @Column(name = "water_area_km2", precision = 12, scale = 4)
    private BigDecimal waterAreaKm2;
    
    @Column(name = "eutrophication_level", length = 32)
    private String eutrophicationLevel;
    
    @Column(name = "result_file_path", length = 500)
    private String resultFilePath;
    
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
    
    public WaterQualityResult() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TiffImage getTiffImage() { return tiffImage; }
    public void setTiffImage(TiffImage tiffImage) { this.tiffImage = tiffImage; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public Long getWetlandId() { return wetlandId; }
    public void setWetlandId(Long wetlandId) { this.wetlandId = wetlandId; }
    public BigDecimal getMinLon() { return minLon; }
    public void setMinLon(BigDecimal minLon) { this.minLon = minLon; }
    public BigDecimal getMaxLon() { return maxLon; }
    public void setMaxLon(BigDecimal maxLon) { this.maxLon = maxLon; }
    public BigDecimal getMinLat() { return minLat; }
    public void setMinLat(BigDecimal minLat) { this.minLat = minLat; }
    public BigDecimal getMaxLat() { return maxLat; }
    public void setMaxLat(BigDecimal maxLat) { this.maxLat = maxLat; }
    public String getGeometryJson() { return geometryJson; }
    public void setGeometryJson(String geometryJson) { this.geometryJson = geometryJson; }
    public BigDecimal getAvgChla() { return avgChla; }
    public void setAvgChla(BigDecimal avgChla) { this.avgChla = avgChla; }
    public BigDecimal getMinChla() { return minChla; }
    public void setMinChla(BigDecimal minChla) { this.minChla = minChla; }
    public BigDecimal getMaxChla() { return maxChla; }
    public void setMaxChla(BigDecimal maxChla) { this.maxChla = maxChla; }
    public BigDecimal getStdChla() { return stdChla; }
    public void setStdChla(BigDecimal stdChla) { this.stdChla = stdChla; }
    public BigDecimal getAvgSpm() { return avgSpm; }
    public void setAvgSpm(BigDecimal avgSpm) { this.avgSpm = avgSpm; }
    public BigDecimal getMinSpm() { return minSpm; }
    public void setMinSpm(BigDecimal minSpm) { this.minSpm = minSpm; }
    public BigDecimal getMaxSpm() { return maxSpm; }
    public void setMaxSpm(BigDecimal maxSpm) { this.maxSpm = maxSpm; }
    public BigDecimal getStdSpm() { return stdSpm; }
    public void setStdSpm(BigDecimal stdSpm) { this.stdSpm = stdSpm; }
    public BigDecimal getAvgTurbidity() { return avgTurbidity; }
    public void setAvgTurbidity(BigDecimal avgTurbidity) { this.avgTurbidity = avgTurbidity; }
    public BigDecimal getMinTurbidity() { return minTurbidity; }
    public void setMinTurbidity(BigDecimal minTurbidity) { this.minTurbidity = minTurbidity; }
    public BigDecimal getMaxTurbidity() { return maxTurbidity; }
    public void setMaxTurbidity(BigDecimal maxTurbidity) { this.maxTurbidity = maxTurbidity; }
    public BigDecimal getStdTurbidity() { return stdTurbidity; }
    public void setStdTurbidity(BigDecimal stdTurbidity) { this.stdTurbidity = stdTurbidity; }
    public Long getWaterPixelCount() { return waterPixelCount; }
    public void setWaterPixelCount(Long waterPixelCount) { this.waterPixelCount = waterPixelCount; }
    public BigDecimal getWaterAreaKm2() { return waterAreaKm2; }
    public void setWaterAreaKm2(BigDecimal waterAreaKm2) { this.waterAreaKm2 = waterAreaKm2; }
    public String getEutrophicationLevel() { return eutrophicationLevel; }
    public void setEutrophicationLevel(String eutrophicationLevel) { this.eutrophicationLevel = eutrophicationLevel; }
    public String getResultFilePath() { return resultFilePath; }
    public void setResultFilePath(String resultFilePath) { this.resultFilePath = resultFilePath; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
