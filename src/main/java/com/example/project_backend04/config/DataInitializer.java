package com.example.project_backend04.config;

import com.example.project_backend04.entity.Role;
import com.example.project_backend04.entity.ServiceCategory;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.AuthRepository;
import com.example.project_backend04.repository.RoleRepository;
import com.example.project_backend04.repository.ServiceCategoryRepository;
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
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initRolesAndAdmin() {
        return args -> {
//            initializeServiceCategories();

            Role userRole = roleRepository.findRoleByName("USER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("USER");
                        return roleRepository.save(role);
                    });


            Role adminRole = roleRepository.findRoleByName("ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ADMIN");
                        return roleRepository.save(role);
                    });

            Role trainerRole = roleRepository.findRoleByName("TRAINER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("TRAINER");
                        role.setDescription("Trainer role for gym instructors");
                        return roleRepository.save(role);
                    });

            Role staffRole = roleRepository.findRoleByName("STAFF")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("STAFF");
                        role.setDescription("Staff role for gym employees");
                        return roleRepository.save(role);
                    });


            if (authRepository.findByEmail("admin@gmail.com").isEmpty()) {
                User admin = new User();
                admin.setEmail("admin@gmail.com");
                admin.setPassword(passwordEncoder.encode("Admin01+"));
                admin.setRole(adminRole);

                authRepository.save(admin);
                System.out.println("Admin user created");
            }
        };
    }

//    private void initializeServiceCategories() {
//        createCategoryIfNotExists("PERSONAL_TRAINER", "Personal Trainer", "One-on-one personal training sessions", "", "#FF6B35", 1);
//        createCategoryIfNotExists("BOXING", "Boxing", "Boxing training and classes", "", "#E74C3C", 2);
//        createCategoryIfNotExists("YOGA", "Yoga", "Yoga classes and meditation", "", "#27AE60", 3);
//        createCategoryIfNotExists("CARDIO", "Cardio", "Cardiovascular training and exercises", "", "#F39C12", 4);
//        createCategoryIfNotExists("OTHER", "Other", "Other fitness services", "", "#9B59B6", 5);
//    }

//    private void createCategoryIfNotExists(String name, String displayName, String description, String icon, String color, int sortOrder) {
//        if (!serviceCategoryRepository.existsByNameIgnoreCase(name)) {
//            ServiceCategory category = new ServiceCategory();
//            category.setName(name);
//            category.setDisplayName(displayName);
//            category.setDescription(description);
//            category.setIcon(icon);
//            category.setColor(color);
//            category.setSortOrder(sortOrder);
//            category.setIsActive(true);
//            serviceCategoryRepository.save(category);
//            System.out.println("Created service category: " + displayName);
//        }
//    }
}