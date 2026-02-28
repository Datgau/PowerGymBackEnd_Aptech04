package com.example.project_backend04.mapper;


import com.example.project_backend04.dto.request.User.CreateUserRequest;
import com.example.project_backend04.dto.request.User.UpdateUserRequest;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "refreshTokenExpiryTime", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "coverPhoto", ignore = true)
    @Mapping(target = "providers", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "followers", ignore = true)
    @Mapping(target = "following", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "checkIns", ignore = true)
    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "refreshTokenExpiryTime", ignore = true)
    @Mapping(target = "providers", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "followers", ignore = true)
    @Mapping(target = "following", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "checkIns", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
