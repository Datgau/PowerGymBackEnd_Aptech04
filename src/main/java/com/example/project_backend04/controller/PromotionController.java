package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.ApplyPromotionRequest;
import com.example.project_backend04.dto.response.ApplyPromotionResponse;
import com.example.project_backend04.dto.response.PromotionResponse;
import com.example.project_backend04.security.CustomUserDetails;
import com.example.project_backend04.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {
    
    private final PromotionService promotionService;
    
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getActivePromotions() {
        List<PromotionResponse> promotions = promotionService.getActivePromotions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", promotions);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/featured")
    public ResponseEntity<Map<String, Object>> getFeaturedPromotions() {
        List<PromotionResponse> promotions = promotionService.getFeaturedPromotions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", promotions);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/apply")
    public ResponseEntity<ApplyPromotionResponse> applyPromotion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ApplyPromotionRequest request) {
        
        Long userId = userDetails.getId();
        ApplyPromotionResponse response = promotionService.validateAndCalculatePromotion(userId, request);
        
        return ResponseEntity.ok(response);
    }
}
