package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    public String generateToken(Long userId) {
        return "token_" + UUID.randomUUID() + "_" + userId;
    }

    public Optional<Long> extractUserId(String token) {
        if (token == null || !token.startsWith("token_")) {
            return Optional.empty();
        }

        int lastUnderscore = token.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore == token.length() - 1) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(token.substring(lastUnderscore + 1)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
