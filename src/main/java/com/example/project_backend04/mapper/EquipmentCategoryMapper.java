package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.request.EquipmentCategory.CreateEquipmentCategoryDto;
import com.example.project_backend04.entity.EquipmentCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EquipmentCategoryMapper {
    EquipmentCategory toEntity(CreateEquipmentCategoryDto dto);
    void updateEntityFromDto(CreateEquipmentCategoryDto dto, @MappingTarget EquipmentCategory category);
}