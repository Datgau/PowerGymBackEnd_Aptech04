package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.request.MembershipPackage.CreateMembershipPackageDto;
import com.example.project_backend04.dto.request.MembershipPackage.UpdateMembershipPackageDto;
import com.example.project_backend04.entity.MembershipPackage;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MembershipPackageMapper {

    MembershipPackage toEntity(CreateMembershipPackageDto dto);

    void updateEntityFromDto(UpdateMembershipPackageDto dto, @MappingTarget MembershipPackage entity);

}
