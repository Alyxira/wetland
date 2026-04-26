package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AiChatRequest {

    @NotNull(message = "消息列表不能为空")
    @NotEmpty(message = "消息列表不能为空")
    @Valid
    private List<Message> messages;

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public static class Message {
        @NotEmpty(message = "消息角色不能为空")
        private String role;

        @NotEmpty(message = "消息内容不能为空")
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
