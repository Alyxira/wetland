package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final String DEFAULT_BIO = "记录湿地光影、季节迁徙与每一次贴近自然的瞬间。";
    private static final String DEFAULT_AVATAR = "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setRealName(request.getUsername().trim());
        user.setBio(DEFAULT_BIO);
        user.setAvatar(DEFAULT_AVATAR);
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));

        User savedUser = userRepository.save(user);
        String token = tokenService.generateToken(savedUser.getId());
        return new AuthResponse(true, "注册成功", token, toUserInfo(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getAccount().trim())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码错误");
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账户已被禁用");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = tokenService.generateToken(user.getId());
        return new AuthResponse(true, "登录成功", token, toUserInfo(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse validateToken(String token) {
        User user = getUserByToken(token);
        return new AuthResponse(true, "Token有效", null, toUserInfo(user));
    }

    @Transactional(readOnly = true)
    public User getUserByToken(String token) {
        Long userId = tokenService.extractUserId(token)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Token无效或已过期"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在或Token无效"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账户已被禁用");
        }
        return user;
    }

    public AuthResponse.UserInfo toUserInfo(User user) {
        return new AuthResponse.UserInfo(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRealName(),
            user.getBio(),
            user.getAvatar(),
            user.getNotifySystem(),
            user.getNotifyInteraction(),
            user.getNotifyEmail()
        );
    }
}
