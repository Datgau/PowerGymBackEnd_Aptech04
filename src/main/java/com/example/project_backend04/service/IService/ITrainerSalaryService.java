package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.response.ServiceSalaryDetail;
import com.example.project_backend04.dto.response.TrainerSalaryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ITrainerSalaryService {

    TrainerSalaryResponse calculateTotalSalary(Long trainerId);

    BigDecimal calculateSalaryForService(Long trainerId, Long serviceId);

    List<ServiceSalaryDetail> calculateSalaryBreakdown(Long trainerId);
    
    void addSalaryToTrainer(Long trainerId, Long serviceId, Long paymentAmount);
}
