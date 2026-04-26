package com.example.demo.controller;

import com.example.demo.dto.CommentRequest;
import com.example.demo.dto.PostCommentRequest;
import com.example.demo.dto.PostRequest;
import com.example.demo.dto.PostResponse;
import com.example.demo.entity.User;
import com.example.demo.service.PostService;
import com.example.demo.support.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final AuthenticatedUserService authenticatedUserService;

    public PostController(PostService postService, AuthenticatedUserService authenticatedUserService) {
        this.postService = postService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<PostResponse> getPosts(@RequestParam(required = false) String tag) {
        if (tag != null && !tag.isBlank()) {
            return ResponseEntity.ok(postService.getPostsByTag(tag));
        }
        return ResponseEntity.ok(postService.getPosts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam("title") String title,
        @RequestParam("content") String content,
        @RequestParam("tag") String tag,
        @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        User user = authenticatedUserService.requireUser(authHeader);
        PostRequest request = new PostRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setTag(tag);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(user, request, image));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<PostResponse> addCommentToPost(
        @PathVariable Long id,
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody PostCommentRequest request
    ) {
        User user = authenticatedUserService.requireUser(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.addComment(user, id, request.getContent()));
    }

    @PostMapping("/comment")
    public ResponseEntity<PostResponse> addCommentLegacy(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody CommentRequest request
    ) {
        User user = authenticatedUserService.requireUser(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.addComment(user, request.getPostId(), request.getContent()));
    }

    @PostMapping({"/{id}/likes", "/{id}/like"})
    public ResponseEntity<PostResponse> likePost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.likePost(id));
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<PostResponse> getPostsByTag(@PathVariable String tag) {
        return ResponseEntity.ok(postService.getPostsByTag(tag));
    }
}
