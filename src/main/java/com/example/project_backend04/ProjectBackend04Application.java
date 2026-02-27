package com.example.project_backend04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class ProjectBackend04Application {

    @PostConstruct
    public void init() {
        // Set timezone mặc định cho toàn bộ application
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ProjectBackend04Application.class, args);
    }

}
