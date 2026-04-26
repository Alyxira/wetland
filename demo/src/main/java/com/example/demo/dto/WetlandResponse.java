package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public class WetlandResponse {

    private boolean success;
    private String message;
    private WetlandData wetland;
    private List<WetlandData> wetlands;

    public WetlandResponse() {
    }

    public WetlandResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public WetlandResponse(boolean success, String message, WetlandData wetland) {
        this.success = success;
        this.message = message;
        this.wetland = wetland;
    }

    public WetlandResponse(boolean success, String message, List<WetlandData> wetlands) {
        this.success = success;
        this.message = message;
        this.wetlands = wetlands;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WetlandData getWetland() {
        return wetland;
    }

    public void setWetland(WetlandData wetland) {
        this.wetland = wetland;
    }

    public List<WetlandData> getWetlands() {
        return wetlands;
    }

    public void setWetlands(List<WetlandData> wetlands) {
        this.wetlands = wetlands;
    }

    public static class WetlandData {
        private Long id;
        private String wetlandName;
        private String imagePath;
        private String coordinateRange;
        private String description;
        private String floraFaunaInfo;
        private String tags;
        private Boolean active;
        private LocalDateTime createdTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public LocalDateTime getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(LocalDateTime createdTime) {
            this.createdTime = createdTime;
        }
    }
}
