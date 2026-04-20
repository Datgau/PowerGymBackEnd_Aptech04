package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.ServiceSalaryDetail;
import com.example.project_backend04.dto.response.TrainerDashboardResponse;
import com.example.project_backend04.dto.response.TrainerSalaryResponse;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.exception.InvalidRoleException;
import com.example.project_backend04.exception.TrainerNotFoundException;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ITrainerSalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerSalaryService implements ITrainerSalaryService {

    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final UserRepository userRepository;
    private final com.example.project_backend04.repository.GymServiceRepository gymServiceRepository;
    private final TrainerBookingRepository trainerBookingRepository;

    @Override
    @Transactional(readOnly = true)
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
        List<com.example.project_backend04.entity.ServiceRegistration> registrations = 
            serviceRegistrationRepository.findActiveRegistrationsByTrainerAndService(trainerId, serviceId);
        
        if (registrations.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }

        BigDecimal totalSalary = BigDecimal.ZERO;
        for (com.example.project_backend04.entity.ServiceRegistration registration : registrations) {
            // Dùng lockedTrainerPercentage nếu có, fallback về gymService.trainerPercentage
            BigDecimal percentage = registration.getLockedTrainerPercentage() != null
                ? registration.getLockedTrainerPercentage()
                : registration.getGymService().getTrainerPercentage();

            BigDecimal amount = (registration.getPaymentOrder() != null && registration.getPaymentOrder().getAmount() != null)
                ? BigDecimal.valueOf(registration.getPaymentOrder().getAmount())
                : registration.getGymService().getPrice();

            totalSalary = totalSalary.add(amount.multiply(percentage));
        }

        return totalSalary.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSalaryDetail> calculateSalaryBreakdown(Long trainerId) {
        List<com.example.project_backend04.entity.GymService> services = 
            serviceRegistrationRepository.findDistinctServicesByTrainerId(trainerId);
        
        List<ServiceSalaryDetail> breakdown = new java.util.ArrayList<>();
        
        for (com.example.project_backend04.entity.GymService service : services) {
            List<com.example.project_backend04.entity.ServiceRegistration> registrations = 
                serviceRegistrationRepository.findActiveRegistrationsByTrainerAndService(trainerId, service.getId());
            
            if (registrations.isEmpty()) continue;

            BigDecimal totalSalary = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (com.example.project_backend04.entity.ServiceRegistration registration : registrations) {
                BigDecimal percentage = registration.getLockedTrainerPercentage() != null
                    ? registration.getLockedTrainerPercentage()
                    : service.getTrainerPercentage();

                BigDecimal amount = (registration.getPaymentOrder() != null && registration.getPaymentOrder().getAmount() != null)
                    ? BigDecimal.valueOf(registration.getPaymentOrder().getAmount())
                    : service.getPrice();

                totalSalary = totalSalary.add(amount.multiply(percentage));
                totalAmount = totalAmount.add(amount);
            }

            totalSalary = totalSalary.setScale(2, java.math.RoundingMode.HALF_UP);
            if (totalSalary.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal averagePrice = totalAmount.divide(
                BigDecimal.valueOf(registrations.size()), 2, java.math.RoundingMode.HALF_UP);

            // Hiển thị % đại diện (từ registration đầu tiên)
            BigDecimal displayPercentage = registrations.get(0).getLockedTrainerPercentage() != null
                ? registrations.get(0).getLockedTrainerPercentage()
                : service.getTrainerPercentage();

            breakdown.add(ServiceSalaryDetail.builder()
                .serviceId(service.getId())
                .serviceName(service.getName())
                .studentCount(registrations.size())
                .servicePrice(averagePrice)
                .trainerPercentage(displayPercentage)
                .salaryAmount(totalSalary)
                .build());
        }
        
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
    
    @Transactional
    public void deductSalaryFromTrainer(Long trainerId, Long serviceId, Long paymentAmount) {
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
        
        // Calculate salary to deduct: paymentAmount * trainerPercentage
        BigDecimal trainerPercentage = service.getTrainerPercentage();
        BigDecimal salaryToDeduct = BigDecimal.valueOf(paymentAmount)
            .multiply(trainerPercentage)
            .setScale(2, java.math.RoundingMode.HALF_UP);
        
        // Deduct from trainer's salary balance
        BigDecimal currentBalance = trainer.getSalaryBalance() != null ? trainer.getSalaryBalance() : BigDecimal.ZERO;
        trainer.setSalaryBalance(currentBalance.subtract(salaryToDeduct));
        
        // Save trainer
        userRepository.save(trainer);
        
        log.info("Deducted salary {} from trainer {} (trainerId={}, serviceId={}, paymentAmount={}, percentage={})",
            salaryToDeduct, trainer.getFullName(), trainerId, serviceId, paymentAmount, trainerPercentage);
    }
    
    /**
     * Get trainer dashboard with statistics and salary (excluding rejected and cancelled bookings)
     */
    @Transactional(readOnly = true)
    public TrainerDashboardResponse getTrainerDashboard(Long trainerId) {
        // Validate trainer exists
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new TrainerNotFoundException(trainerId));
        if (!trainer.isTrainer()) {
            throw new InvalidRoleException(trainerId);
        }
        
        // Get all bookings for this trainer
        List<TrainerBooking> allBookings = trainerBookingRepository.findByTrainerOrderByBookingDateDescStartTimeDesc(trainer);
        
        // Filter out REJECTED and CANCELLED bookings for salary calculation
        List<TrainerBooking> validBookings = allBookings.stream()
            .filter(b -> b.getStatus() != BookingStatus.REJECTED && b.getStatus() != BookingStatus.CANCELLED)
            .collect(Collectors.toList());
        
        // Count unique clients from valid bookings
        java.util.Set<Long> clientIdsFromBookings = validBookings.stream()
            .map(b -> b.getUser().getId())
            .collect(Collectors.toSet());
        
        // Also count clients from ACTIVE registrations without bookings (COUNTER payments confirmed but no booking yet)
        List<com.example.project_backend04.entity.ServiceRegistration> activeRegistrations = 
            serviceRegistrationRepository.findActiveRegistrationsByTrainerId(trainerId);
        
        java.util.Set<Long> clientIdsFromRegistrations = activeRegistrations.stream()
            .filter(reg -> {
                // Only count if registration has SUCCESS payment
                boolean hasSuccessPayment = reg.getPaymentOrder() != null && 
                    reg.getPaymentOrder().getStatus() == com.example.project_backend04.enums.PaymentStatus.SUCCESS;
                return hasSuccessPayment;
            })
            .map(reg -> reg.getUser().getId())
            .collect(Collectors.toSet());
        
        // Combine both sets
        java.util.Set<Long> allClientIds = new java.util.HashSet<>();
        allClientIds.addAll(clientIdsFromBookings);
        allClientIds.addAll(clientIdsFromRegistrations);
        
        Integer totalClients = allClientIds.size();
        
        // Count pending approvals
        Integer pendingApprovals = (int) allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.PENDING)
            .count();
        
        // Count confirmed bookings
        Integer confirmedBookings = (int) validBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .count();
        
        // Count completed bookings
        Integer completedBookings = (int) validBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .count();
        
        // Calculate salary breakdown (only for valid bookings)
        List<ServiceSalaryDetail> serviceBreakdown = calculateSalaryBreakdownExcludingRejectedCancelled(trainerId);
        
        // Calculate total salary
        BigDecimal totalSalary = serviceBreakdown.stream()
            .map(ServiceSalaryDetail::getSalaryAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, java.math.RoundingMode.HALF_UP);
        
        return TrainerDashboardResponse.builder()
            .trainerId(trainerId)
            .trainerName(trainer.getFullName())
            .trainerEmail(trainer.getEmail())
            .trainerAvatar(trainer.getAvatar())
            .totalClients(totalClients)
            .pendingApprovals(pendingApprovals)
            .confirmedBookings(confirmedBookings)
            .completedBookings(completedBookings)
            .totalSalary(totalSalary)
            .currentBalance(trainer.getSalaryBalance() != null ? trainer.getSalaryBalance() : BigDecimal.ZERO)
            .serviceBreakdown(serviceBreakdown)
            .calculatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Calculate salary breakdown excluding rejected and cancelled bookings
     */
    @Transactional(readOnly = true)
    public List<ServiceSalaryDetail> calculateSalaryBreakdownExcludingRejectedCancelled(Long trainerId) {
        // Get all service registrations for this trainer
        List<com.example.project_backend04.entity.GymService> services = 
            serviceRegistrationRepository.findDistinctServicesByTrainerId(trainerId);
        
        List<ServiceSalaryDetail> breakdown = new java.util.ArrayList<>();
        
        for (com.example.project_backend04.entity.GymService service : services) {
            List<com.example.project_backend04.entity.ServiceRegistration> registrations = 
                serviceRegistrationRepository.findActiveRegistrationsByTrainerAndService(trainerId, service.getId());
            
            if (registrations.isEmpty()) continue;
            
            // Filter registrations based on booking status
            List<com.example.project_backend04.entity.ServiceRegistration> validRegistrations = registrations.stream()
                .filter(reg -> {
                    // Get bookings for this registration
                    List<TrainerBooking> bookings = trainerBookingRepository.findByServiceRegistration_Id(reg.getId());
                    
                    // If no bookings exist yet, check if registration is ACTIVE with payment SUCCESS
                    if (bookings.isEmpty()) {
                        boolean isActive = reg.getStatus() == com.example.project_backend04.enums.RegistrationStatus.ACTIVE;
                        boolean hasSuccessPayment = reg.getPaymentOrder() != null && 
                            reg.getPaymentOrder().getStatus() == com.example.project_backend04.enums.PaymentStatus.SUCCESS;
                        return isActive && hasSuccessPayment;
                    }
                    
                    // If bookings exist, check if there's at least one booking that is not rejected or cancelled
                    boolean hasValidBooking = bookings.stream()
                        .anyMatch(b -> b.getStatus() != BookingStatus.REJECTED && b.getStatus() != BookingStatus.CANCELLED);
                    
                    // Also check if all bookings are rejected/cancelled - in that case, don't count
                    boolean allRejectedOrCancelled = bookings.stream()
                        .allMatch(b -> b.getStatus() == BookingStatus.REJECTED || b.getStatus() == BookingStatus.CANCELLED);
                    
                    return hasValidBooking && !allRejectedOrCancelled;
                })
                .collect(Collectors.toList());
            
            if (validRegistrations.isEmpty()) continue;

            BigDecimal totalSalary = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (com.example.project_backend04.entity.ServiceRegistration registration : validRegistrations) {
                BigDecimal percentage = registration.getLockedTrainerPercentage() != null
                    ? registration.getLockedTrainerPercentage()
                    : service.getTrainerPercentage();

                BigDecimal amount = (registration.getPaymentOrder() != null && registration.getPaymentOrder().getAmount() != null)
                    ? BigDecimal.valueOf(registration.getPaymentOrder().getAmount())
                    : service.getPrice();

                totalSalary = totalSalary.add(amount.multiply(percentage));
                totalAmount = totalAmount.add(amount);
            }

            totalSalary = totalSalary.setScale(2, java.math.RoundingMode.HALF_UP);
            if (totalSalary.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal averagePrice = totalAmount.divide(
                BigDecimal.valueOf(validRegistrations.size()), 2, java.math.RoundingMode.HALF_UP);

            // Display percentage from first registration
            BigDecimal displayPercentage = validRegistrations.get(0).getLockedTrainerPercentage() != null
                ? validRegistrations.get(0).getLockedTrainerPercentage()
                : service.getTrainerPercentage();

            breakdown.add(ServiceSalaryDetail.builder()
                .serviceId(service.getId())
                .serviceName(service.getName())
                .studentCount(validRegistrations.size())
                .servicePrice(averagePrice)
                .trainerPercentage(displayPercentage)
                .salaryAmount(totalSalary)
                .build());
        }
        
        return breakdown;
    }
}