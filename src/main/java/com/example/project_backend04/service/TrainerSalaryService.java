package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.ServiceSalaryDetail;
import com.example.project_backend04.dto.response.TrainerSalaryResponse;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.exception.InvalidRoleException;
import com.example.project_backend04.exception.TrainerNotFoundException;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ITrainerSalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerSalaryService implements ITrainerSalaryService {

    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final UserRepository userRepository;
    private final com.example.project_backend04.repository.GymServiceRepository gymServiceRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "trainerSalary", key = "#trainerId")
    public TrainerSalaryResponse calculateTotalSalary(Long trainerId) {
        // Validate trainer exists using UserRepository
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new TrainerNotFoundException(trainerId));
        if (!trainer.isTrainer()) {
            throw new InvalidRoleException(trainerId);
        }
        List<ServiceSalaryDetail> serviceBreakdown = calculateSalaryBreakdown(trainerId);

        BigDecimal totalSalary = serviceBreakdown.stream()
            .map(ServiceSalaryDetail::getSalaryAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, java.math.RoundingMode.HALF_UP);
        
        // Build TrainerSalaryResponse with trainerId, trainerName, totalSalary, serviceBreakdown, and current timestamp
        return TrainerSalaryResponse.builder()
            .trainerId(trainerId)
            .trainerName(trainer.getFullName())
            .totalSalary(totalSalary)
            .serviceBreakdown(serviceBreakdown)
            .calculatedAt(LocalDateTime.now())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSalaryForService(Long trainerId, Long serviceId) {
        // Query active registrations using findActiveRegistrationsByTrainerAndService
        List<com.example.project_backend04.entity.ServiceRegistration> registrations = 
            serviceRegistrationRepository.findActiveRegistrationsByTrainerAndService(trainerId, serviceId);
        
        // Return 0.00 if no registrations found
        if (registrations.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        
        // Count students (size of registrations list)
        int studentCount = registrations.size();
        
        // Get trainerPercentage from gymService
        com.example.project_backend04.entity.GymService gymService = registrations.get(0).getGymService();
        BigDecimal trainerPercentage = gymService.getTrainerPercentage();
        
        // Calculate total price by iterating registrations and summing payment amounts
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (com.example.project_backend04.entity.ServiceRegistration registration : registrations) {
            // Use paymentOrder.amount if available, otherwise use gymService.price
            if (registration.getPaymentOrder() != null && registration.getPaymentOrder().getAmount() != null) {
                totalPrice = totalPrice.add(BigDecimal.valueOf(registration.getPaymentOrder().getAmount()));
            } else {
                totalPrice = totalPrice.add(gymService.getPrice());
            }
        }
        
        // Calculate average price: totalPrice / studentCount
        BigDecimal averagePrice = totalPrice.divide(BigDecimal.valueOf(studentCount), 2, java.math.RoundingMode.HALF_UP);
        
        // Apply formula: salary = averagePrice × trainerPercentage × studentCount
        BigDecimal salary = averagePrice.multiply(trainerPercentage).multiply(BigDecimal.valueOf(studentCount));
        
        // Round result to 2 decimal places using HALF_UP rounding mode
        return salary.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSalaryDetail> calculateSalaryBreakdown(Long trainerId) {
        // Query distinct services using findDistinctServicesByTrainerId
        List<com.example.project_backend04.entity.GymService> services = 
            serviceRegistrationRepository.findDistinctServicesByTrainerId(trainerId);
        
        // Build list of ServiceSalaryDetail objects
        List<ServiceSalaryDetail> breakdown = new java.util.ArrayList<>();
        
        // For each service, call calculateSalaryForService
        for (com.example.project_backend04.entity.GymService service : services) {
            BigDecimal salaryAmount = calculateSalaryForService(trainerId, service.getId());
            
            // Exclude services where salary is zero (no students)
            if (salaryAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Get active registrations to count students
                List<com.example.project_backend04.entity.ServiceRegistration> registrations = 
                    serviceRegistrationRepository.findActiveRegistrationsByTrainerAndService(trainerId, service.getId());
                
                // Calculate average service price
                BigDecimal totalPrice = BigDecimal.ZERO;
                for (com.example.project_backend04.entity.ServiceRegistration registration : registrations) {
                    if (registration.getPaymentOrder() != null && registration.getPaymentOrder().getAmount() != null) {
                        totalPrice = totalPrice.add(BigDecimal.valueOf(registration.getPaymentOrder().getAmount()));
                    } else {
                        totalPrice = totalPrice.add(service.getPrice());
                    }
                }
                BigDecimal averagePrice = totalPrice.divide(BigDecimal.valueOf(registrations.size()), 2, java.math.RoundingMode.HALF_UP);
                
                // Build ServiceSalaryDetail object with service info and calculated salary
                ServiceSalaryDetail detail = ServiceSalaryDetail.builder()
                    .serviceId(service.getId())
                    .serviceName(service.getName())
                    .studentCount(registrations.size())
                    .servicePrice(averagePrice)
                    .trainerPercentage(service.getTrainerPercentage())
                    .salaryAmount(salaryAmount)
                    .build();
                
                breakdown.add(detail);
            }
        }
        
        // Return List<ServiceSalaryDetail>
        return breakdown;
    }
    
    @Override
    @Transactional
    public void addSalaryToTrainer(Long trainerId, Long serviceId, Long paymentAmount) {
        // Find trainer
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new TrainerNotFoundException(trainerId));
        
        // Validate user has TRAINER role
        if (!trainer.isTrainer()) {
            throw new InvalidRoleException(trainerId);
        }
        
        // Find service to get trainer percentage
        com.example.project_backend04.entity.GymService service = gymServiceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("Service not found with id: " + serviceId));
        
        // Calculate salary: paymentAmount * trainerPercentage
        BigDecimal trainerPercentage = service.getTrainerPercentage();
        BigDecimal salaryToAdd = BigDecimal.valueOf(paymentAmount)
            .multiply(trainerPercentage)
            .setScale(2, java.math.RoundingMode.HALF_UP);
        
        // Add to trainer's salary balance
        BigDecimal currentBalance = trainer.getSalaryBalance() != null ? trainer.getSalaryBalance() : BigDecimal.ZERO;
        trainer.setSalaryBalance(currentBalance.add(salaryToAdd));
        
        // Save trainer
        userRepository.save(trainer);
        
        log.info("Added salary {} to trainer {} (trainerId={}, serviceId={}, paymentAmount={}, percentage={})",
            salaryToAdd, trainer.getFullName(), trainerId, serviceId, paymentAmount, trainerPercentage);
    }
}