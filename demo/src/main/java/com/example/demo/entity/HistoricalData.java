package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "historical_data",
    indexes = {
        @Index(name = "idx_historical_region_date", columnList = "region_name,data_date"),
        @Index(name = "idx_historical_date", columnList = "data_date"),
        @Index(name = "idx_historical_location", columnList = "center_lon,center_lat"),
        @Index(name = "idx_historical_source", columnList = "data_source")
    }
)
public class HistoricalData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tiff_image_id")
    private TiffImage tiffImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "wetland_id")
    private Long wetlandId;
    
    @Column(name = "region_name", nullable = false, length = 128)
    private String regionName;
    
    @Column(name = "center_lon", nullable = false, precision = 18, scale = 6)
    private BigDecimal centerLon;
    
    @Column(name = "center_lat", nullable = false, precision = 18, scale = 6)
    private BigDecimal centerLat;
    
    @Column(name = "data_date", nullable = false)
    private LocalDateTime dataDate;
    
    @Column(name = "chla_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal chlaValue;
    
    @Column(name = "spm_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal spmValue;
    
    @Column(name = "turbidity_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal turbidityValue;
    
    @Column(name = "data_source", length = 64)
    private String dataSource;
    
    @Column(name = "record_time", nullable = false, updatable = false)
    private LocalDateTime recordTime;
    
    @PrePersist
    protected void onCreate() {
        recordTime = LocalDateTime.now();
    }
    
    public HistoricalData() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TiffImage getTiffImage() { return tiffImage; }
    public void setTiffImage(TiffImage tiffImage) { this.tiffImage = tiffImage; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public Long getWetlandId() { return wetlandId; }
    public void setWetlandId(Long wetlandId) { this.wetlandId = wetlandId; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public BigDecimal getCenterLon() { return centerLon; }
    public void setCenterLon(BigDecimal centerLon) { this.centerLon = centerLon; }
    public BigDecimal getCenterLat() { return centerLat; }
    public void setCenterLat(BigDecimal centerLat) { this.centerLat = centerLat; }
    public LocalDateTime getDataDate() { return dataDate; }
    public void setDataDate(LocalDateTime dataDate) { this.dataDate = dataDate; }
    public BigDecimal getChlaValue() { return chlaValue; }
    public void setChlaValue(BigDecimal chlaValue) { this.chlaValue = chlaValue; }
    public BigDecimal getSpmValue() { return spmValue; }
    public void setSpmValue(BigDecimal spmValue) { this.spmValue = spmValue; }
    public BigDecimal getTurbidityValue() { return turbidityValue; }
    public void setTurbidityValue(BigDecimal turbidityValue) { this.turbidityValue = turbidityValue; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
}
