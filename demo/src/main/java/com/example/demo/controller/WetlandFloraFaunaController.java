package com.example.demo.controller;

import com.example.demo.dto.FloraFaunaResponse;
import com.example.demo.service.WetlandFloraFaunaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WetlandFloraFaunaController {

    private final WetlandFloraFaunaService wetlandFloraFaunaService;

    public WetlandFloraFaunaController(WetlandFloraFaunaService wetlandFloraFaunaService) {
        this.wetlandFloraFaunaService = wetlandFloraFaunaService;
    }

    @GetMapping("/wetlands/{wetlandId}/flora-fauna")
    public ResponseEntity<FloraFaunaResponse> getByWetlandId(@PathVariable("wetlandId") Long wetlandId) {
        return ResponseEntity.ok(wetlandFloraFaunaService.getByWetlandId(wetlandId));
    }

    @GetMapping("/flora-fauna/{id}")
    public ResponseEntity<FloraFaunaResponse> getDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(wetlandFloraFaunaService.getDetail(id));
    }
}
