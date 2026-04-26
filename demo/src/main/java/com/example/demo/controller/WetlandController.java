package com.example.demo.controller;

import com.example.demo.dto.WetlandRequest;
import com.example.demo.dto.WetlandResponse;
import com.example.demo.service.WetlandInfoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wetlands")
public class WetlandController {

    private final WetlandInfoService wetlandInfoService;

    public WetlandController(WetlandInfoService wetlandInfoService) {
        this.wetlandInfoService = wetlandInfoService;
    }

    @GetMapping
    public ResponseEntity<WetlandResponse> getWetlands(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(wetlandInfoService.getWetlands(keyword, tag));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WetlandResponse> getWetland(@PathVariable("id") Long id) {
        return ResponseEntity.ok(wetlandInfoService.getWetland(id));
    }

    @PostMapping
    public ResponseEntity<WetlandResponse> createWetland(@Valid @RequestBody WetlandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wetlandInfoService.createWetland(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WetlandResponse> updateWetland(
        @PathVariable("id") Long id,
        @Valid @RequestBody WetlandRequest request
    ) {
        return ResponseEntity.ok(wetlandInfoService.updateWetland(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WetlandResponse> deleteWetland(@PathVariable("id") Long id) {
        return ResponseEntity.ok(wetlandInfoService.deleteWetland(id));
    }
}
