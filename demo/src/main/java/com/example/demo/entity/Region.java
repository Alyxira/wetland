package com.example.demo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "region",
    indexes = {
        @Index(name = "idx_region_name", columnList = "region_name")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_region_code", columnNames = "region_code")
    }
)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_code", nullable = false, length = 64)
    private String regionCode;

    @Column(name = "region_name", nullable = false, length = 128)
    private String regionName;

    @Column(name = "wetland_id")
    private Long wetlandId;

    @Column(length = 255)
    private String description;

    @Column(name = "geometry_type", length = 32)
    private String geometryType;

    @Column(name = "geometry_json", columnDefinition = "json")
    private String geometryJson;

    @Column(name = "min_lon", precision = 18, scale = 6)
    private BigDecimal minLon;

    @Column(name = "max_lon", precision = 18, scale = 6)
    private BigDecimal maxLon;

    @Column(name = "min_lat", precision = 18, scale = 6)
    private BigDecimal minLat;

    @Column(name = "max_lat", precision = 18, scale = 6)
    private BigDecimal maxLat;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public Long getWetlandId() { return wetlandId; }
    public void setWetlandId(Long wetlandId) { this.wetlandId = wetlandId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getGeometryType() { return geometryType; }
    public void setGeometryType(String geometryType) { this.geometryType = geometryType; }
    public String getGeometryJson() { return geometryJson; }
    public void setGeometryJson(String geometryJson) { this.geometryJson = geometryJson; }
    public BigDecimal getMinLon() { return minLon; }
    public void setMinLon(BigDecimal minLon) { this.minLon = minLon; }
    public BigDecimal getMaxLon() { return maxLon; }
    public void setMaxLon(BigDecimal maxLon) { this.maxLon = maxLon; }
    public BigDecimal getMinLat() { return minLat; }
    public void setMinLat(BigDecimal minLat) { this.minLat = minLat; }
    public BigDecimal getMaxLat() { return maxLat; }
    public void setMaxLat(BigDecimal maxLat) { this.maxLat = maxLat; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
