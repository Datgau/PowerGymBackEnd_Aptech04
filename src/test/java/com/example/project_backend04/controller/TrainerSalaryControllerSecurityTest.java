package com.example.project_backend04.controller;

import com.example.project_backend04.dto.response.TrainerSalaryResponse;
import com.example.project_backend04.service.TrainerSalaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrainerSalaryController.class)
@Import(com.example.project_backend04.config.SecurityConfig.class)
class TrainerSalaryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainerSalaryService trainerSalaryService;

    @Test
    @WithAnonymousUser
    void getTrainerSalary_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/trainers/1/salary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "trainer1", roles = {"TRAINER"})
    void getTrainerSalary_trainerAccessingOwnSalary_shouldReturn200() throws Exception {
        TrainerSalaryResponse response = TrainerSalaryResponse.builder()
                .trainerId(1L)
                .trainerName("Trainer 1")
                .totalSalary(BigDecimal.valueOf(1000.00))
                .serviceBreakdown(Collections.emptyList())
                .calculatedAt(LocalDateTime.now())
                .build();

        when(trainerSalaryService.calculateTotalSalary(1L)).thenReturn(response);

        mockMvc.perform(get("/api/trainers/1/salary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "trainer1", roles = {"TRAINER"})
    void getTrainerSalary_trainerAccessingAnotherTrainerSalary_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/trainers/2/salary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getTrainerSalary_adminAccessingAnySalary_shouldReturn200() throws Exception {
        TrainerSalaryResponse response = TrainerSalaryResponse.builder()
                .trainerId(1L)
                .trainerName("Trainer 1")
                .totalSalary(BigDecimal.valueOf(1000.00))
                .serviceBreakdown(Collections.emptyList())
                .calculatedAt(LocalDateTime.now())
                .build();

        when(trainerSalaryService.calculateTotalSalary(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/trainers/1/salary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getTrainerSalary_regularUserAccessingSalary_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/trainers/1/salary"))
                .andExpect(status().isForbidden());
    }
}
