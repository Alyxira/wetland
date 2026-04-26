package com.example.demo.service;

import com.example.demo.dto.PostRequest;
import com.example.demo.dto.PostResponse;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final Path postsUploadDir;

    public PostService(
        PostRepository postRepository,
        CommentRepository commentRepository,
        @Value("${app.posts.images.dir:${user.dir}/src/uploads/posts}") String postsImagesDir
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postsUploadDir = Paths.get(postsImagesDir).toAbsolutePath().normalize();
    }

    @Transactional
    public PostResponse createPost(User user, PostRequest request, MultipartFile imageFile) {
        validatePostRequest(request);
        Post post = new Post(user, request.getTitle().trim(), request.getContent().trim(), request.getTag().trim());
        post.setImage(storePostImage(imageFile, request.getImage()));

        Post savedPost = postRepository.save(post);
        List<String> imagePaths = loadPostImagesByIds(List.of(savedPost.getId())).get(savedPost.getId());
        return new PostResponse(true, "帖子发布成功", convertToPostData(savedPost, true, imagePaths));
    }

    @Transactional(readOnly = true)
    public PostResponse getPosts() {
        List<Post> postEntities = postRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, List<String>> postImagesMap = loadPostImagesForPosts(postEntities);
        List<PostResponse.PostData> posts = postEntities.stream()
            .map(post -> convertToPostData(post, false, postImagesMap.get(post.getId())))
            .toList();
        return new PostResponse(true, "获取帖子列表成功", posts);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId) {
        Post post = getRequiredPost(postId);
        List<String> imagePaths = loadPostImagesByIds(List.of(postId)).get(postId);
        PostResponse.PostData postData = convertToPostData(post, true, imagePaths);
        List<PostResponse.CommentData> comments = commentRepository.findByPost_IdOrderByCreatedAtDesc(postId).stream()
            .map(this::convertToCommentData)
            .toList();
        postData.setComments(comments);
        return new PostResponse(true, "获取帖子详情成功", postData);
    }

    @Transactional
    public PostResponse addComment(User user, Long postId, String content) {
        Post post = getRequiredPost(postId);
        Comment comment = new Comment(post, user, content.trim());
        commentRepository.save(comment);
        post.incrementComments();
        postRepository.save(post);
        return new PostResponse(true, "评论发布成功");
    }

    @Transactional
    public PostResponse likePost(Long postId) {
        Post post = getRequiredPost(postId);
        post.incrementLikes();
        postRepository.save(post);
        List<String> imagePaths = loadPostImagesByIds(List.of(postId)).get(postId);
        return new PostResponse(true, "点赞成功", convertToPostData(post, false, imagePaths));
    }

    @Transactional(readOnly = true)
    public PostResponse getPostsByTag(String tag) {
        List<Post> postEntities = postRepository.findByTagOrderByCreatedAtDesc(tag);
        Map<Long, List<String>> postImagesMap = loadPostImagesForPosts(postEntities);
        List<PostResponse.PostData> posts = postEntities.stream()
            .map(post -> convertToPostData(post, false, postImagesMap.get(post.getId())))
            .toList();
        return new PostResponse(true, "获取帖子列表成功", posts);
    }

    private Post getRequiredPost(Long postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "帖子不存在"));
    }

    private void validatePostRequest(PostRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "帖子内容不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题不能为空");
        }
        if (request.getTitle().trim().length() > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题长度不能超过200个字符");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容不能为空");
        }
        if (!StringUtils.hasText(request.getTag())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标签不能为空");
        }
        if (request.getTag().trim().length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标签长度不能超过50个字符");
        }
    }

    private String storePostImage(MultipartFile imageFile, String rawImage) {
        if (imageFile == null || imageFile.isEmpty()) {
            return StringUtils.hasText(rawImage) ? rawImage.trim() : null;
        }

        String originalFilename = imageFile.getOriginalFilename();
        String extension = resolveFileExtension(originalFilename);
        if (!isAllowedImageExtension(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "仅支持上传 jpg、jpeg、png、gif、webp 图片");
        }

        try {
            Files.createDirectories(postsUploadDir);
            String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "."
                + extension;
            Path targetFile = postsUploadDir.resolve(fileName).normalize();
            Files.copy(imageFile.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/posts/" + fileName;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "帖子图片保存失败");
        }
    }

    private String resolveFileExtension(String filename) {
        String safeName = StringUtils.hasText(filename) ? filename.trim() : "";
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == safeName.length() - 1) {
            return "";
        }
        return safeName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedImageExtension(String extension) {
        return List.of("jpg", "jpeg", "png", "gif", "webp").contains(extension);
    }

    private PostResponse.PostData convertToPostData(Post post, boolean includeComments, List<String> imagePaths) {
        PostResponse.PostData postData = new PostResponse.PostData();
        postData.setId(post.getId());
        postData.setUserId(post.getUser().getId());
        postData.setAuthor(post.getUser().getUsername());
        postData.setAvatar(post.getUser().getAvatar());
        postData.setTitle(post.getTitle());
        postData.setContent(post.getContent());
        List<String> mergedImages = mergeImages(post.getImage(), imagePaths);
        postData.setImage(mergedImages.isEmpty() ? null : mergedImages.get(0));
        postData.setImages(mergedImages);
        postData.setTag(post.getTag());
        postData.setLikes(post.getLikesCount());
        postData.setLiked(false);
        postData.setTime(formatTime(post.getCreatedAt()));
        if (!includeComments) {
            postData.setComments(List.of());
        }
        return postData;
    }

    private Map<Long, List<String>> loadPostImagesForPosts(List<Post> posts) {
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return loadPostImagesByIds(postIds);
    }

    private Map<Long, List<String>> loadPostImagesByIds(List<Long> postIds) {
        Map<Long, List<String>> result = new LinkedHashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return result;
        }

        for (PostRepository.PostImageRow row : postRepository.findPostImageRowsByPostIds(postIds)) {
            if (row.getPostId() == null) {
                continue;
            }
            result.computeIfAbsent(row.getPostId(), key -> new ArrayList<>());
            if (StringUtils.hasText(row.getImagePath())) {
                result.get(row.getPostId()).add(row.getImagePath().trim());
            }
        }
        return result;
    }

    private List<String> mergeImages(String legacyImage, List<String> imagePaths) {
        List<String> merged = new ArrayList<>();
        if (imagePaths != null) {
            for (String imagePath : imagePaths) {
                if (StringUtils.hasText(imagePath) && !merged.contains(imagePath.trim())) {
                    merged.add(imagePath.trim());
                }
            }
        }
        if (StringUtils.hasText(legacyImage) && !merged.contains(legacyImage.trim())) {
            merged.add(legacyImage.trim());
        }
        return merged;
    }

    private PostResponse.CommentData convertToCommentData(Comment comment) {
        PostResponse.CommentData commentData = new PostResponse.CommentData();
        commentData.setId(comment.getId());
        commentData.setUserId(comment.getUser().getId());
        commentData.setUser(comment.getUser().getUsername());
        commentData.setText(comment.getContent());
        commentData.setTime(formatTime(comment.getCreatedAt()));
        return commentData;
    }

    private String formatTime(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(time, now);
        long hours = ChronoUnit.HOURS.between(time, now);
        long days = ChronoUnit.DAYS.between(time, now);

        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        if (hours < 24) {
            return hours + "小时前";
        }
        if (days < 7) {
            return days + "天前";
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
