package com.example.project_backend04.event;

import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.enums.RegistrationType;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Component
@RequiredArgsConstructor
@Slf4j
public class CounterRegistrationCleanupScheduler {

    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final TrainerBookingRepository trainerBookingRepository;
    
    private static final int EXPIRY_DAYS = 3;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredCounterRegistrations() {
        try {
            LocalDateTime expiryThreshold = LocalDateTime.now().minusDays(EXPIRY_DAYS);
            List<ServiceRegistration> expiredRegistrations = serviceRegistrationRepository
                .findExpiredCounterRegistrations(expiryThreshold, RegistrationType.COUNTER, RegistrationStatus.PENDING);
            
            if (expiredRegistrations.isEmpty()) {
                log.info("No expired counter registrations found");
                return;
            }
            int cancelledCount = 0;
            for (ServiceRegistration registration : expiredRegistrations) {
                try {
                    registration.setStatus(RegistrationStatus.CANCELLED);
                    registration.setCancelledDate(LocalDateTime.now());
                    registration.setCancellationReason("Tự động hủy: Không thanh toán sau " + EXPIRY_DAYS + " ngày");
                    serviceRegistrationRepository.save(registration);
                    List<TrainerBooking> bookings = trainerBookingRepository
                        .findByServiceRegistration_Id(registration.getId());
                    
                    for (TrainerBooking booking : bookings) {
                        if (booking.getStatus() == BookingStatus.PENDING) {
                            booking.setStatus(BookingStatus.CANCELLED);
                            booking.setCancelledAt(LocalDateTime.now());
                            booking.setCancellationReason("Tự động hủy: Đăng ký không được thanh toán");
                            trainerBookingRepository.save(booking);
                        }
                    }
                    
                    cancelledCount++;
                    log.info("Cancelled registration ID: {} for user: {}", 
                        registration.getId(), registration.getUser().getEmail());
                    
                } catch (Exception e) {
                    log.error("Error cancelling registration ID: {}", registration.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during counter registration cleanup", e);
        }
    }
}
