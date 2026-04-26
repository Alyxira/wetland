package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponse {
    private boolean success;
    private String message;
    private PostData post;
    private List<PostData> posts;
    
    public PostResponse() {}
    
    public PostResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public PostResponse(boolean success, String message, PostData post) {
        this.success = success;
        this.message = message;
        this.post = post;
    }
    
    public PostResponse(boolean success, String message, List<PostData> posts) {
        this.success = success;
        this.message = message;
        this.posts = posts;
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
    
    public PostData getPost() {
        return post;
    }
    
    public void setPost(PostData post) {
        this.post = post;
    }
    
    public List<PostData> getPosts() {
        return posts;
    }
    
    public void setPosts(List<PostData> posts) {
        this.posts = posts;
    }
    
    public static class PostData {
        private Long id;
        private Long userId;
        private String author;
        private String avatar;
        private String title;
        private String content;
        private String image;
        private List<String> images;
        private String tag;
        private int likes;
        private boolean isLiked;
        private List<CommentData> comments;
        private String time;
        
        public PostData() {}
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getAuthor() {
            return author;
        }
        
        public void setAuthor(String author) {
            this.author = author;
        }
        
        public String getAvatar() {
            return avatar;
        }
        
        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getImage() {
            return image;
        }
        
        public void setImage(String image) {
            this.image = image;
        }

        public List<String> getImages() {
            return images;
        }

        public void setImages(List<String> images) {
            this.images = images;
        }
        
        public String getTag() {
            return tag;
        }
        
        public void setTag(String tag) {
            this.tag = tag;
        }
        
        public int getLikes() {
            return likes;
        }
        
        public void setLikes(int likes) {
            this.likes = likes;
        }
        
        public boolean isLiked() {
            return isLiked;
        }
        
        public void setLiked(boolean liked) {
            isLiked = liked;
        }
        
        public List<CommentData> getComments() {
            return comments;
        }
        
        public void setComments(List<CommentData> comments) {
            this.comments = comments;
        }
        
        public String getTime() {
            return time;
        }
        
        public void setTime(String time) {
            this.time = time;
        }
    }
    
    public static class CommentData {
        private Long id;
        private Long userId;
        private String user;
        private String text;
        private String time;
        
        public CommentData() {}
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getUser() {
            return user;
        }
        
        public void setUser(String user) {
            this.user = user;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public String getTime() {
            return time;
        }
        
        public void setTime(String time) {
            this.time = time;
        }
    }
}
