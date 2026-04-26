package com.example.demo.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private UserInfo user;
    
    public AuthResponse() {}
    
    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public AuthResponse(boolean success, String message, String token, UserInfo user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
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
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public UserInfo getUser() {
        return user;
    }
    
    public void setUser(UserInfo user) {
        this.user = user;
    }
    
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String realName;
        private String bio;
        private String avatar;
        private Boolean notifySystem;
        private Boolean notifyInteraction;
        private Boolean notifyEmail;
        
        public UserInfo() {}
        
        public UserInfo(Long id, String username, String email, String realName, String bio, String avatar, 
                       Boolean notifySystem, Boolean notifyInteraction, Boolean notifyEmail) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.realName = realName;
            this.bio = bio;
            this.avatar = avatar;
            this.notifySystem = notifySystem;
            this.notifyInteraction = notifyInteraction;
            this.notifyEmail = notifyEmail;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getRealName() {
            return realName;
        }
        
        public void setRealName(String realName) {
            this.realName = realName;
        }
        
        public String getBio() {
            return bio;
        }
        
        public void setBio(String bio) {
            this.bio = bio;
        }
        
        public String getAvatar() {
            return avatar;
        }
        
        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
        
        public Boolean getNotifySystem() {
            return notifySystem;
        }
        
        public void setNotifySystem(Boolean notifySystem) {
            this.notifySystem = notifySystem;
        }
        
        public Boolean getNotifyInteraction() {
            return notifyInteraction;
        }
        
        public void setNotifyInteraction(Boolean notifyInteraction) {
            this.notifyInteraction = notifyInteraction;
        }
        
        public Boolean getNotifyEmail() {
            return notifyEmail;
        }
        
        public void setNotifyEmail(Boolean notifyEmail) {
            this.notifyEmail = notifyEmail;
        }
    }
}