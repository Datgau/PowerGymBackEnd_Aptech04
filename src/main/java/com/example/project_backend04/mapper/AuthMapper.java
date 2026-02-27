//package com.example.project_backend04.mapper;
//
//import com.example.project_backend04.entity.PendingUser;
//import com.example.project_backend04.payload.request.RegisterRequest;
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//
//@Mapper(componentModel = "spring")
//public interface AuthMapper {
//
//    @Mapping(target = "otp", ignore = true)
//    @Mapping(target = "otpExpiry", ignore = true)
//    PendingUser toPendingUser(RegisterRequest request);
//}
