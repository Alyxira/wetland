package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class PostCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
