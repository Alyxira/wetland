package com.example.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.service.PythonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pyapi/v1")
@CrossOrigin(origins = "*")
public class InternalPythonController {

    @Autowired
    private PythonService pythonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/invert")
    public ResponseEntity<?> invert(@RequestBody Map<String, Object> request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String responseJson = pythonService.executeUserWaterQualityInversion(requestJson);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(responseJson, Map.class);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> body = new HashMap<>();
            body.put("code", "PYTHON_INVERT_FAILED");
            body.put("message", e.getMessage());
            body.put("requestId", null);
            return ResponseEntity.status(422).body(body);
        }
    }
}

