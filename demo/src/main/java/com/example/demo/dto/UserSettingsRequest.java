package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class UserSettingsRequest {

    @NotNull(message = "系统通知设置不能为空")
    private Boolean notifySystem;

    @NotNull(message = "互动通知设置不能为空")
    private Boolean notifyInteraction;

    @NotNull(message = "邮件通知设置不能为空")
    private Boolean notifyEmail;
    
    public UserSettingsRequest() {}
    
    public UserSettingsRequest(Boolean notifySystem, Boolean notifyInteraction, Boolean notifyEmail) {
        this.notifySystem = notifySystem;
        this.notifyInteraction = notifyInteraction;
        this.notifyEmail = notifyEmail;
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
