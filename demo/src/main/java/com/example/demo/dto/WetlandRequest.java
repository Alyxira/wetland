package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WetlandRequest {

    @NotBlank(message = "湿地名称不能为空")
    @Size(max = 100, message = "湿地名称长度不能超过100个字符")
    private String wetlandName;

    @Size(max = 255, message = "图片地址长度不能超过255个字符")
    private String imagePath;

    @Size(max = 200, message = "坐标范围长度不能超过200个字符")
    private String coordinateRange;

    @Size(max = 1000, message = "湿地介绍长度不能超过1000个字符")
    private String description;

    @Size(max = 1000, message = "动植物信息长度不能超过1000个字符")
    private String floraFaunaInfo;

    @Size(max = 100, message = "标签长度不能超过100个字符")
    private String tags;

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
}
