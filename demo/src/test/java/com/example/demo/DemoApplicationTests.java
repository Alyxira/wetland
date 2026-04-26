package com.example.demo;

import com.example.demo.service.TokenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoApplicationTests {

    @Test
    void tokenServiceCanRoundTripUserId() {
        TokenService tokenService = new TokenService();
        String token = tokenService.generateToken(42L);

        assertTrue(token.startsWith("token_"));
        assertTrue(tokenService.extractUserId(token).isPresent());
        assertEquals(42L, tokenService.extractUserId(token).orElseThrow());
    }
}
