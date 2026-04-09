package com.example.project_backend04.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateImportReceiptRequest {
    @NotBlank(message = "Supplier name is required")
    private String supplierName;
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
    
    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<ImportReceiptItemRequest> items;
}
