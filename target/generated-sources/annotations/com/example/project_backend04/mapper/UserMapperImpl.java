package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.request.User.CreateUserRequest;
import com.example.project_backend04.dto.request.User.UpdateUserRequest;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-28T16:34:44+0700",
    comments = "version: 1.6.2, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(CreateUserRequest request) {
        if ( request == null ) {
            return null;
        }

        User user = new User();

        user.setEmail( request.getEmail() );
        user.setFullName( request.getFullName() );
        user.setPhoneNumber( request.getPhoneNumber() );

        return user;
    }

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.id( user.getId() );
        userResponse.email( user.getEmail() );
        userResponse.fullName( user.getFullName() );
        userResponse.phoneNumber( user.getPhoneNumber() );
        userResponse.avatar( user.getAvatar() );
        userResponse.bio( user.getBio() );
        userResponse.coverPhoto( user.getCoverPhoto() );
        userResponse.createDate( user.getCreateDate() );
        userResponse.role( user.getRole() );

        return userResponse.build();
    }

    @Override
    public void updateEntityFromRequest(UpdateUserRequest request, User user) {
        if ( request == null ) {
            return;
        }

        if ( request.getEmail() != null ) {
            user.setEmail( request.getEmail() );
        }
        if ( request.getFullName() != null ) {
            user.setFullName( request.getFullName() );
        }
        if ( request.getPhoneNumber() != null ) {
            user.setPhoneNumber( request.getPhoneNumber() );
        }
        if ( request.getAvatar() != null ) {
            user.setAvatar( request.getAvatar() );
        }
    }
}
