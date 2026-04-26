package com.example.demo.dto;

import java.util.List;

public class UserCommentsResponse {

    private boolean success;
    private String message;
    private List<UserCommentData> comments;

    public UserCommentsResponse() {
    }

    public UserCommentsResponse(boolean success, String message, List<UserCommentData> comments) {
        this.success = success;
        this.message = message;
        this.comments = comments;
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

    public List<UserCommentData> getComments() {
        return comments;
    }

    public void setComments(List<UserCommentData> comments) {
        this.comments = comments;
    }

    public static class UserCommentData {
        private Long id;
        private Long postId;
        private String postTitle;
        private String postImage;
        private String content;
        private String time;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getPostId() {
            return postId;
        }

        public void setPostId(Long postId) {
            this.postId = postId;
        }

        public String getPostTitle() {
            return postTitle;
        }

        public void setPostTitle(String postTitle) {
            this.postTitle = postTitle;
        }

        public String getPostImage() {
            return postImage;
        }

        public void setPostImage(String postImage) {
            this.postImage = postImage;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
