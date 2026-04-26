package com.example.demo.common;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiErrorResponse {

    private final boolean success = false;
    private final String message;
    private final LocalDateTime timestamp;
    private final Map<String, String> errors;

    public ApiErrorResponse(String message) {
        this(message, Map.of());
    }

    public ApiErrorResponse(String message, Map<String, String> errors) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
