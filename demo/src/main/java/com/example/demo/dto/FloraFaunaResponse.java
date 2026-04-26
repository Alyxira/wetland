package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FloraFaunaResponse {

    private boolean success;
    private String message;
    private FloraFaunaData floraFauna;
    private List<FloraFaunaData> floraFaunas;

    public FloraFaunaResponse() {
    }

    public FloraFaunaResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public FloraFaunaResponse(boolean success, String message, FloraFaunaData floraFauna) {
        this.success = success;
        this.message = message;
        this.floraFauna = floraFauna;
    }

    public FloraFaunaResponse(boolean success, String message, List<FloraFaunaData> floraFaunas) {
        this.success = success;
        this.message = message;
        this.floraFaunas = floraFaunas;
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

    public FloraFaunaData getFloraFauna() {
        return floraFauna;
    }

    public void setFloraFauna(FloraFaunaData floraFauna) {
        this.floraFauna = floraFauna;
    }

    public List<FloraFaunaData> getFloraFaunas() {
        return floraFaunas;
    }

    public void setFloraFaunas(List<FloraFaunaData> floraFaunas) {
        this.floraFaunas = floraFaunas;
    }

    public static class FloraFaunaData {
        private Long id;
        private String wetlandId;
        private String wetlandName;
        private String name;
        private String description;
        private String imagePath;
        private Boolean active;
        private LocalDateTime createdTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getWetlandId() {
            return wetlandId;
        }

        public void setWetlandId(String wetlandId) {
            this.wetlandId = wetlandId;
        }

        public String getWetlandName() {
            return wetlandName;
        }

        public void setWetlandName(String wetlandName) {
            this.wetlandName = wetlandName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
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
