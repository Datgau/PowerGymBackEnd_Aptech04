package com.example.project_backend04.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateImportReceiptRequest {
    
    @NotBlank(message = "Supplier name is required")
    private String supplierName;
    
    private String notes;
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ImportReceiptItemRequest> items;
    
    @NotBlank(message = "Password is required for update")
    private String password;
}
