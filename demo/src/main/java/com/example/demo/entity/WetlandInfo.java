package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 湿地信息实体类
 * 对应数据库表：wetland_info（与之前SQL脚本的WetlandInfo表映射）
 */
@Entity
@Table(name = "\"wetlandInfo\"")
public class WetlandInfo {

    // 主键ID（自增，对应SQL的IDENTITY(1,1)）
    @Id
    @Column(name = "\"WetlandID\"")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wetlandId; // 对应SQL的WetlandID，驼峰命名符合Java规范

    // 湿地名称（非空，长度100）
    @Column(name = "\"WetlandName\"", nullable = false, length = 100)
    private String wetlandName;

    // 图片路径（本地路径/URL，长度255）
    @Column(name = "\"ImagePath\"", length = 255)
    private String imagePath;

    // 地理坐标范围（长度200）
    @Column(name = "\"CoordinateRange\"", length = 200)
    private String coordinateRange;

    // 湿地介绍（长文本，对应SQL的NVARCHAR(1000)）
    @Column(name = "\"Description\"", length = 1000)
    private String description;

    // 动植物信息（长文本，对应SQL的NVARCHAR(1000)）
    @Column(name = "\"FloraFaunaInfo\"", length = 1000)
    private String floraFaunaInfo;

    // 标签（如：内陆/河流/长江，长度100）
    @Column(name = "\"Tags\"", length = 100)
    private String tags;

    // 创建时间（非空，自动填充）
    @Column(name = "\"CreateTime\"", nullable = false)
    private LocalDateTime createdTime;

    // 是否有效（逻辑删除标记，默认true）
    @Column(name = "\"IsActive\"", nullable = false)
    private Boolean active = true;

    // 新增/修改时自动填充时间（与User类的PrePersist/PreUpdate逻辑一致）
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }

    // 空参构造（JPA必须）
    public WetlandInfo() {}

    // 核心字段构造函数（方便快速创建对象）
    public WetlandInfo(String wetlandName, String coordinateRange, String description, String floraFaunaInfo, String tags) {
        this.wetlandName = wetlandName;
        this.coordinateRange = coordinateRange;
        this.description = description;
        this.floraFaunaInfo = floraFaunaInfo;
        this.tags = tags;
        this.active = true;
    }

    // ========== Getter/Setter 方法（与User类格式完全一致） ==========
    public Long getWetlandId() {
        return wetlandId;
    }

    public void setWetlandId(Long wetlandId) {
        this.wetlandId = wetlandId;
    }

    public String getWetlandName() {
        return wetlandName;
    }

    public void setWetlandName(String wetlandName) {
        this.wetlandName = wetlandName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getCoordinateRange() {
        return coordinateRange;
    }

    public void setCoordinateRange(String coordinateRange) {
        this.coordinateRange = coordinateRange;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFloraFaunaInfo() {
        return floraFaunaInfo;
    }

    public void setFloraFaunaInfo(String floraFaunaInfo) {
        this.floraFaunaInfo = floraFaunaInfo;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
