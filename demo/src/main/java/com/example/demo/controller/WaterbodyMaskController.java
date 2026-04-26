package com.example.demo.controller;

import com.example.demo.service.WaterbodyMaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/waterbody")
@CrossOrigin(origins = "*")
public class WaterbodyMaskController {

    private final WaterbodyMaskService waterbodyMaskService;

    public WaterbodyMaskController(WaterbodyMaskService waterbodyMaskService) {
        this.waterbodyMaskService = waterbodyMaskService;
    }

    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        try {
            return ResponseEntity.ok(success(waterbodyMaskService.listFiles()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(error("Failed to list waterbody files: " + e.getMessage()));
        }
    }

    @GetMapping("/all-geojson")
    public ResponseEntity<?> getAllGeoJson() {
        try {
            return ResponseEntity.ok(success(waterbodyMaskService.getAllGeoJson()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(error("Failed to load waterbody GeoJSON data: " + e.getMessage()));
        }
    }

    @GetMapping("/bounds/{filename}")
    public ResponseEntity<?> getBounds(@PathVariable String filename) {
        try {
            return ResponseEntity.ok(success(waterbodyMaskService.getBounds(filename)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(error("Failed to calculate waterbody bounds: " + e.getMessage()));
        }
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return body;
    }
}

