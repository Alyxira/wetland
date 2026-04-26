package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.UserCommentsResponse;
import com.example.demo.dto.UserPasswordRequest;
import com.example.demo.dto.UserProfileRequest;
import com.example.demo.dto.UserSettingsRequest;
import com.example.demo.entity.Comment;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TokenService tokenService;
    private final AuthService authService;

    public UserService(
        UserRepository userRepository,
        CommentRepository commentRepository,
        TokenService tokenService,
        AuthService authService
    ) {
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.tokenService = tokenService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public AuthResponse getProfile(Long userId) {
        User user = getRequiredUser(userId);
        AuthResponse response = new AuthResponse(true, "获取用户信息成功");
        response.setUser(authService.toUserInfo(user));
        return response;
    }

    @Transactional
    public AuthResponse updateProfile(Long userId, UserProfileRequest request) {
        User user = getRequiredUser(userId);
        String username = request.getUsername().trim();

        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已被使用");
        }

        user.setUsername(username);
        user.setRealName(request.getRealName());
        user.setBio(request.getBio());
        user.setAvatar(request.getAvatar());

        User updatedUser = userRepository.save(user);
        return new AuthResponse(true, "个人信息更新成功", null, authService.toUserInfo(updatedUser));
    }

    @Transactional
    public AuthResponse updatePassword(Long userId, UserPasswordRequest request) {
        User user = getRequiredUser(userId);

        if (!BCrypt.checkpw(request.getOldPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "原密码错误");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "两次输入的新密码不一致");
        }

        user.setPassword(BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));
        userRepository.save(user);
        return new AuthResponse(true, "密码更新成功");
    }

    @Transactional
    public AuthResponse updateSettings(Long userId, UserSettingsRequest request) {
        User user = getRequiredUser(userId);

        user.setNotifySystem(request.getNotifySystem());
        user.setNotifyInteraction(request.getNotifyInteraction());
        user.setNotifyEmail(request.getNotifyEmail());

        User updatedUser = userRepository.save(user);
        return new AuthResponse(true, "通知设置更新成功", null, authService.toUserInfo(updatedUser));
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByToken(String token) {
        return tokenService.extractUserId(token)
            .flatMap(userRepository::findById)
            .filter(user -> Boolean.TRUE.equals(user.getActive()));
    }

    @Transactional(readOnly = true)
    public UserCommentsResponse getUserComments(Long userId) {
        User user = getRequiredUser(userId);
        List<UserCommentsResponse.UserCommentData> comments = commentRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(this::toUserCommentData)
            .toList();
        return new UserCommentsResponse(true, "获取用户评论成功", comments);
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    private UserCommentsResponse.UserCommentData toUserCommentData(Comment comment) {
        UserCommentsResponse.UserCommentData data = new UserCommentsResponse.UserCommentData();
        data.setId(comment.getId());
        data.setPostId(comment.getPost().getId());
        data.setPostTitle(comment.getPost().getTitle());
        data.setPostImage(comment.getPost().getImage());
        data.setContent(comment.getContent());
        data.setTime(formatTime(comment.getCreatedAt()));
        return data;
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
