package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.request.Equipment.CreateEquipmentDto;
import com.example.project_backend04.entity.Equipment;
import com.example.project_backend04.entity.EquipmentCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EquipmentMapper {
    
    @Mapping(target = "category", ignore = true)
    Equipment toEntity(CreateEquipmentDto dto);
    
    @Mapping(target = "category", ignore = true)
    void updateEntityFromDto(CreateEquipmentDto dto, @MappingTarget Equipment equipment);
    
    default EquipmentCategory mapCategoryId(Long categoryId) {
        if (categoryId == null) return null;
        EquipmentCategory category = new EquipmentCategory();
        category.setId(categoryId);
        return category;
    }
}