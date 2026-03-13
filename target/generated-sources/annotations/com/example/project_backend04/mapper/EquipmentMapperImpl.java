package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.request.Equipment.CreateEquipmentDto;
import com.example.project_backend04.entity.Equipment;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-13T12:46:22+0700",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.1 (Oracle Corporation)"
)
@Component
public class EquipmentMapperImpl implements EquipmentMapper {

    @Override
    public Equipment toEntity(CreateEquipmentDto dto) {
        if ( dto == null ) {
            return null;
        }

        Equipment equipment = new Equipment();

        equipment.setName( dto.getName() );
        equipment.setDescription( dto.getDescription() );
        equipment.setPrice( dto.getPrice() );
        equipment.setQuantity( dto.getQuantity() );
        equipment.setImage( dto.getImage() );
        equipment.setIsActive( dto.getIsActive() );

        return equipment;
    }

    @Override
    public void updateEntityFromDto(CreateEquipmentDto dto, Equipment equipment) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getName() != null ) {
            equipment.setName( dto.getName() );
        }
        if ( dto.getDescription() != null ) {
            equipment.setDescription( dto.getDescription() );
        }
        if ( dto.getPrice() != null ) {
            equipment.setPrice( dto.getPrice() );
        }
        if ( dto.getQuantity() != null ) {
            equipment.setQuantity( dto.getQuantity() );
        }
        if ( dto.getImage() != null ) {
            equipment.setImage( dto.getImage() );
        }
        if ( dto.getIsActive() != null ) {
            equipment.setIsActive( dto.getIsActive() );
        }
    }
}
