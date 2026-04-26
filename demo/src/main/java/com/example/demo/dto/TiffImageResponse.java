package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TiffImageResponse {
    
    private Long id;
    private String fileName;
    private String region;
    private LocalDateTime acquisitionDate;
    private BigDecimal minLon;
    private BigDecimal maxLon;
    private BigDecimal minLat;
    private BigDecimal maxLat;
    private Integer width;
    private Integer height;
    private Integer bandCount;
    private LocalDateTime uploadTime;
    private String description;
    
    public TiffImageResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
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
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
