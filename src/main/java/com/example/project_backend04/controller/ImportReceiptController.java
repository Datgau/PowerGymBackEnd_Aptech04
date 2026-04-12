package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.CreateImportReceiptRequest;
import com.example.project_backend04.dto.request.PasswordVerificationRequest;
import com.example.project_backend04.dto.request.UpdateImportReceiptRequest;
import com.example.project_backend04.dto.response.Product.ImportReceiptDetailResponse;
import com.example.project_backend04.dto.response.Product.ImportReceiptResponse;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.security.CustomUserDetails;
import com.example.project_backend04.service.ImportReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/import-receipts")
@RequiredArgsConstructor
@Slf4j
public class ImportReceiptController {
    
    private final ImportReceiptService importReceiptService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<ImportReceiptResponse>>> getAllImportReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String supplierName
    ) {
               
        Page<ImportReceiptResponse> receipts = importReceiptService.getAllImportReceipts(
                page, size, startDate, endDate, supplierName
        );
        return ResponseEntity.ok(ApiResponse.success(receipts, "Import receipts retrieved successfully"));
    }
   
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ImportReceiptDetailResponse>> getImportReceiptById(@PathVariable Long id) {        
        ImportReceiptDetailResponse receipt = importReceiptService.getImportReceiptById(id);
        return ResponseEntity.ok(ApiResponse.success(receipt, "Import receipt retrieved successfully"));
    }
    

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ImportReceiptResponse>> createImportReceipt(
            @Valid @RequestBody CreateImportReceiptRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.error("POST /api/import-receipts - User is null, authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        User user = userDetails.getUser();
        log.info("POST /api/import-receipts - User: {}", user.getEmail());
        
        ImportReceiptResponse receipt = importReceiptService.createImportReceipt(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(receipt, "Import receipt created successfully"));
    }
    
    /**
     * Update an existing import receipt
     * Requires password verification
     * ADMIN only
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ImportReceiptResponse>> updateImportReceipt(
            @PathVariable Long id,
            @Valid @RequestBody UpdateImportReceiptRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.error("PUT /api/import-receipts/{} - User is null, authentication failed", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        User user = userDetails.getUser();
        log.info("PUT /api/import-receipts/{} - User: {}", id, user.getEmail());
        
        try {
            ImportReceiptResponse receipt = importReceiptService.updateImportReceipt(id, request, user);
            return ResponseEntity.ok(ApiResponse.success(receipt, "Import receipt updated successfully"));
        } catch (IllegalArgumentException e) {
            log.error("PUT /api/import-receipts/{} - Password verification failed", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.FORBIDDEN.value()));
        }
    }
    
    /**
     * Delete an import receipt
     * Requires password verification
     * ADMIN only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteImportReceipt(
            @PathVariable Long id,
            @Valid @RequestBody PasswordVerificationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.error("DELETE /api/import-receipts/{} - User is null, authentication failed", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", HttpStatus.UNAUTHORIZED.value()));
        }
        
        User user = userDetails.getUser();
        log.info("DELETE /api/import-receipts/{} - User: {}", id, user.getEmail());
        
        try {
            importReceiptService.deleteImportReceipt(id, request.getPassword(), user);
            return ResponseEntity.ok(ApiResponse.success(null, "Import receipt deleted successfully"));
        } catch (IllegalArgumentException e) {
            log.error("DELETE /api/import-receipts/{} - Password verification failed", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.FORBIDDEN.value()));
        }
    }
}
