package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.UserCommentsResponse;
import com.example.demo.dto.UserPasswordRequest;
import com.example.demo.dto.UserProfileRequest;
import com.example.demo.dto.UserSettingsRequest;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.support.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/users", "/user"})
public class UserController {

    private final UserService userService;
    private final AuthenticatedUserService authenticatedUserService;

    public UserController(UserService userService, AuthenticatedUserService authenticatedUserService) {
        this.userService = userService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping({"/me", "/profile"})
    public ResponseEntity<AuthResponse> getProfile(@RequestHeader("Authorization") String authHeader) {
        User user = authenticatedUserService.requireUser(authHeader);
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @GetMapping("/me/comments")
    public ResponseEntity<UserCommentsResponse> getMyComments(@RequestHeader("Authorization") String authHeader) {
        User user = authenticatedUserService.requireUser(authHeader);
        return ResponseEntity.ok(userService.getUserComments(user.getId()));
    }

    @PutMapping({"/me", "/profile"})
    public ResponseEntity<AuthResponse> updateProfile(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody UserProfileRequest request
    ) {
        User user = authenticatedUserService.requireUser(authHeader);
        return ResponseEntity.ok(userService.updateProfile(user.getId(), request));
    }

    @PutMapping({"/me/password", "/password"})
    public ResponseEntity<AuthResponse> updatePassword(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody UserPasswordRequest request
    ) {
        User user = authenticatedUserService.requireUser(authHeader);
        return ResponseEntity.ok(userService.updatePassword(user.getId(), request));
    }

    @PutMapping({"/me/settings", "/settings"})
    public ResponseEntity<AuthResponse> updateSettings(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody UserSettingsRequest request
    ) {
        User user = authenticatedUserService.requireUser(authHeader);
        return ResponseEntity.ok(userService.updateSettings(user.getId(), request));
    }
}
