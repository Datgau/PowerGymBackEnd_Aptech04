package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface IUserService {
    ApiResponse<UserResponse> uploadUserAvatar(Long userId, MultipartFile avatarFile);

    Optional<User> findByEmail(String username);
    

    Optional<User> findById(Long userId);
}
