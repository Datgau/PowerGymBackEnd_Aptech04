package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.CreatePromotionRequest;
import com.example.project_backend04.dto.request.UpdatePromotionRequest;
import com.example.project_backend04.dto.response.PromotionResponse;
import com.example.project_backend04.entity.Promotion;
import com.example.project_backend04.mapper.PromotionMapper;
import com.example.project_backend04.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {
    
    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllPromotions() {
        List<PromotionResponse> promotions = promotionRepository.findAll()
                .stream()
                .map(promotionMapper::toResponse)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", promotions);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPromotionById(@PathVariable Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", promotionMapper.toResponse(promotion));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createPromotion(@RequestBody CreatePromotionRequest request) {
        // Check if code already exists
        if (promotionRepository.findByCode(request.getCode()).isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Promotion code already exists");
            return ResponseEntity.badRequest().body(response);
        }
        
        Promotion promotion = Promotion.builder()
                .code(request.getCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .discountPercentage(request.getType() == com.example.project_backend04.enums.PromotionType.PERCENTAGE_DISCOUNT ? request.getDiscountValue() : null)
                .discountAmount(request.getType() == com.example.project_backend04.enums.PromotionType.FIXED_AMOUNT_DISCOUNT ? request.getDiscountValue() : null)
                .minPurchaseAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .usageLimit(request.getUsageLimit())
                .usageCount(0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .build();
        
        Promotion savedPromotion = promotionRepository.save(promotion);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Promotion created successfully");
        response.put("data", promotionMapper.toResponse(savedPromotion));
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updatePromotion(
            @PathVariable Long id,
            @RequestBody UpdatePromotionRequest request) {
        
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        
        if (request.getTitle() != null) promotion.setTitle(request.getTitle());
        if (request.getDescription() != null) promotion.setDescription(request.getDescription());
        if (request.getType() != null) promotion.setType(request.getType());
        if (request.getDiscountValue() != null) {
            if (request.getType() == com.example.project_backend04.enums.PromotionType.PERCENTAGE_DISCOUNT) {
                promotion.setDiscountPercentage(request.getDiscountValue());
                promotion.setDiscountAmount(null);
            } else if (request.getType() == com.example.project_backend04.enums.PromotionType.FIXED_AMOUNT_DISCOUNT) {
                promotion.setDiscountAmount(request.getDiscountValue());
                promotion.setDiscountPercentage(null);
            }
        }
        if (request.getMinOrderAmount() != null) promotion.setMinPurchaseAmount(request.getMinOrderAmount());
        if (request.getMaxDiscountAmount() != null) promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getValidFrom() != null) promotion.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) promotion.setValidUntil(request.getValidUntil());
        if (request.getUsageLimit() != null) promotion.setUsageLimit(request.getUsageLimit());
        if (request.getIsActive() != null) promotion.setIsActive(request.getIsActive());
        if (request.getIsFeatured() != null) promotion.setIsFeatured(request.getIsFeatured());
        
        Promotion updatedPromotion = promotionRepository.save(promotion);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Promotion updated successfully");
        response.put("data", promotionMapper.toResponse(updatedPromotion));
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePromotion(@PathVariable Long id) {
        if (!promotionRepository.existsById(id)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Promotion not found");
            return ResponseEntity.badRequest().body(response);
        }
        
        promotionRepository.deleteById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Promotion deleted successfully");
        
        return ResponseEntity.ok(response);
    }
}
