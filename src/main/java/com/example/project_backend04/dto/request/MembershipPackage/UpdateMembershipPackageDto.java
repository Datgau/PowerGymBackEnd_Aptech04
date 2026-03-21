package com.example.project_backend04.dto.request.MembershipPackage;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateMembershipPackageDto {

    @NotBlank(message = "Package ID must not be blank")
    @Size(max = 50, message = "Package ID must not exceed 50 characters")
    private String packageId;

    @NotBlank(message = "Package name must not be blank")
    @Size(max = 100, message = "Package name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than 0")
    private Integer duration; // number of days

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price can have up to 8 integer digits and 2 decimal places")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Original price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Original price can have up to 8 integer digits and 2 decimal places")
    private BigDecimal originalPrice;

    @Min(value = 0, message = "Discount must be at least 0")
    @Max(value = 100, message = "Discount must not exceed 100")
    private Integer discount;

    @Size(max = 20, message = "A package can have at most 20 features")
    private List<
            @NotBlank(message = "Feature must not be blank")
            @Size(max = 200, message = "Feature must not exceed 200 characters")
                    String> features;

    private Boolean isPopular = false;

    private Boolean isActive = true;

    @Pattern(
            regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "Color must be a valid hex code (e.g., #FF5733)"
    )
    private String color;
}