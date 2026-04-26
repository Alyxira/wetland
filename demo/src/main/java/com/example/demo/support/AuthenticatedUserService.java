package com.example.demo.support;

import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;

    public AuthenticatedUserService(UserService userService) {
        this.userService = userService;
    }

    public User requireUser(String authHeader) {
        String token = extractToken(authHeader);
        return userService.getUserByToken(token)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在或Token无效"));
    }

    public String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token格式错误");
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }
}
