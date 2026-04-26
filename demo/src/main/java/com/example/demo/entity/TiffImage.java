package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "tiff_image",
    indexes = {
        @Index(name = "idx_tiff_acquisition_date", columnList = "acquisition_date"),
        @Index(name = "idx_tiff_upload_time", columnList = "upload_time"),
        @Index(name = "idx_tiff_bbox_min", columnList = "min_lon,min_lat"),
        @Index(name = "idx_tiff_bbox_max", columnList = "max_lon,max_lat")
    }
)
public class TiffImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String fileName;
    
    @Column(nullable = false, length = 500)
    private String filePath;
    
    @Column(nullable = false)
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "wetland_id")
    private Long wetlandId;

    @Column(name = "acquisition_date")
    private LocalDateTime acquisitionDate;

    @Column(name = "min_lon", precision = 18, scale = 6)
    private BigDecimal minLon;
    @Column(name = "max_lon", precision = 18, scale = 6)
    private BigDecimal maxLon;
    @Column(name = "min_lat", precision = 18, scale = 6)
    private BigDecimal minLat;
    @Column(name = "max_lat", precision = 18, scale = 6)
    private BigDecimal maxLat;
    
    private Integer width;
    private Integer height;
    @Column(name = "band_count")
    private Integer bandCount;
    
    @Column(length = 64)
    private String crs;

    @Column(name = "bounds_crs", length = 64)
    private String boundsCrs;
    
    @Column(name = "upload_time", nullable = false, updatable = false)
    private LocalDateTime uploadTime;
    
    @Column(length = 500)
    private String description;
    
    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }
    
    public TiffImage() {}
    
    public TiffImage(String fileName, String filePath, Long fileSize) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public Long getWetlandId() { return wetlandId; }
    public void setWetlandId(Long wetlandId) { this.wetlandId = wetlandId; }
    public LocalDateTime getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDateTime acquisitionDate) { this.acquisitionDate = acquisitionDate; }
    public BigDecimal getMinLon() { return minLon; }
    public void setMinLon(BigDecimal minLon) { this.minLon = minLon; }
    public BigDecimal getMaxLon() { return maxLon; }
    public void setMaxLon(BigDecimal maxLon) { this.maxLon = maxLon; }
    public BigDecimal getMinLat() { return minLat; }
    public void setMinLat(BigDecimal minLat) { this.minLat = minLat; }
    public BigDecimal getMaxLat() { return maxLat; }
    public void setMaxLat(BigDecimal maxLat) { this.maxLat = maxLat; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getBandCount() { return bandCount; }
    public void setBandCount(Integer bandCount) { this.bandCount = bandCount; }
    public String getCrs() { return crs; }
    public void setCrs(String crs) { this.crs = crs; }
    public String getBoundsCrs() { return boundsCrs; }
    public void setBoundsCrs(String boundsCrs) { this.boundsCrs = boundsCrs; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
