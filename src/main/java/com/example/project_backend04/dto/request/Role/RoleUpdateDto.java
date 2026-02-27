package com.example.project_backend04.dto.request.Role;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleUpdateDto {

    @NotNull(message = "ID không được để trống")
    private Long id;

    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;

    private String description;
}
