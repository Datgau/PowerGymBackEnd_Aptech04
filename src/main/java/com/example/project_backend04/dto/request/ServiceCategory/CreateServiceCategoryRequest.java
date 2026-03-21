package com.example.project_backend04.dto.request.ServiceCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServiceCategoryRequest {

    @NotBlank(message = "Name không được để trống")
    @Size(min = 2, max = 100, message = "Name phải từ 2-100 ký tự")
    private String name; // PERSONAL_TRAINER, BOXING, etc.

    @NotBlank(message = "Display name không được để trống")
    @Size(min = 2, max = 200, message = "Display name phải từ 2-200 ký tự")
    private String displayName; // "Personal Trainer", "Boxing", etc.

    @Size(max = 1000, message = "Description không được quá 1000 ký tự")
    private String description;

    @Size(max = 100, message = "Icon không được quá 100 ký tự")
    private String icon;

    @Size(max = 7, message = "Color phải là mã hex hợp lệ")
    private String color; // #FF5722


}