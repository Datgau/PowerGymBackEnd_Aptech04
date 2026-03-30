package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.request.MembershipPackage.CreateMembershipPackageDto;
import com.example.project_backend04.dto.request.MembershipPackage.UpdateMembershipPackageDto;
import com.example.project_backend04.entity.MembershipPackage;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-30T17:55:07+0700",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
@Component
public class MembershipPackageMapperImpl implements MembershipPackageMapper {

    @Override
    public MembershipPackage toEntity(CreateMembershipPackageDto dto) {
        if ( dto == null ) {
            return null;
        }

        MembershipPackage membershipPackage = new MembershipPackage();

        membershipPackage.setName( dto.getName() );
        membershipPackage.setDescription( dto.getDescription() );
        membershipPackage.setDuration( dto.getDuration() );
        membershipPackage.setPrice( dto.getPrice() );
        membershipPackage.setOriginalPrice( dto.getOriginalPrice() );
        membershipPackage.setDiscount( dto.getDiscount() );
        List<String> list = dto.getFeatures();
        if ( list != null ) {
            membershipPackage.setFeatures( new ArrayList<String>( list ) );
        }
        membershipPackage.setIsPopular( dto.getIsPopular() );
        membershipPackage.setIsActive( dto.getIsActive() );
        membershipPackage.setColor( dto.getColor() );

        return membershipPackage;
    }

    @Override
    public void updateEntityFromDto(UpdateMembershipPackageDto dto, MembershipPackage entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getPackageId() != null ) {
            entity.setPackageId( dto.getPackageId() );
        }
        if ( dto.getName() != null ) {
            entity.setName( dto.getName() );
        }
        if ( dto.getDescription() != null ) {
            entity.setDescription( dto.getDescription() );
        }
        if ( dto.getDuration() != null ) {
            entity.setDuration( dto.getDuration() );
        }
        if ( dto.getPrice() != null ) {
            entity.setPrice( dto.getPrice() );
        }
        if ( dto.getOriginalPrice() != null ) {
            entity.setOriginalPrice( dto.getOriginalPrice() );
        }
        if ( dto.getDiscount() != null ) {
            entity.setDiscount( dto.getDiscount() );
        }
        if ( entity.getFeatures() != null ) {
            List<String> list = dto.getFeatures();
            if ( list != null ) {
                entity.getFeatures().clear();
                entity.getFeatures().addAll( list );
            }
        }
        else {
            List<String> list = dto.getFeatures();
            if ( list != null ) {
                entity.setFeatures( new ArrayList<String>( list ) );
            }
        }
        if ( dto.getIsPopular() != null ) {
            entity.setIsPopular( dto.getIsPopular() );
        }
        if ( dto.getIsActive() != null ) {
            entity.setIsActive( dto.getIsActive() );
        }
        if ( dto.getColor() != null ) {
            entity.setColor( dto.getColor() );
        }
    }
}
