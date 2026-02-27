package com.example.project_backend04.config;

import com.example.project_backend04.entity.Role;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.AuthRepository;
import com.example.project_backend04.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AuthRepository authRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initRolesAndAdmin() {
        return args -> {
            if (roleRepository.findRoleByName("USER").isEmpty()) {
                Role userRole = new Role();
                userRole.setName("USER");
                roleRepository.save(userRole);
                System.out.println("Role USER created");
            }

            if (roleRepository.findRoleByName("ADMIN").isEmpty()) {
                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                roleRepository.save(adminRole);
                System.out.println("Role ADMIN created");
            }

            if (authRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@gmail.com");
                admin.setPassword(passwordEncoder.encode("Admin01+"));
                Role adminRole = roleRepository.findRoleByName("ADMIN").get();
                admin.setRole(adminRole);
                authRepository.save(admin);
                System.out.println("Admin user created");
            }
        };
    }
}
