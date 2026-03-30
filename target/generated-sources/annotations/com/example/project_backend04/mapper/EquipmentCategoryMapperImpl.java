package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.request.EquipmentCategory.CreateEquipmentCategoryDto;
import com.example.project_backend04.entity.EquipmentCategory;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-30T17:55:07+0700",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
@Component
public class EquipmentCategoryMapperImpl implements EquipmentCategoryMapper {

    @Override
    public EquipmentCategory toEntity(CreateEquipmentCategoryDto dto) {
        if ( dto == null ) {
            return null;
        }

        EquipmentCategory equipmentCategory = new EquipmentCategory();

        equipmentCategory.setName( dto.getName() );
        equipmentCategory.setDescription( dto.getDescription() );
        equipmentCategory.setIsActive( dto.getIsActive() );

        return equipmentCategory;
    }

    @Override
    public void updateEntityFromDto(CreateEquipmentCategoryDto dto, EquipmentCategory category) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getName() != null ) {
            category.setName( dto.getName() );
        }
        if ( dto.getDescription() != null ) {
            category.setDescription( dto.getDescription() );
        }
        if ( dto.getIsActive() != null ) {
            category.setIsActive( dto.getIsActive() );
        }
    }
}
