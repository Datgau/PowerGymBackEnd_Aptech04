package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Trainer.TrainerBookingResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerScheduleResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerStatisticsResponse;
import com.example.project_backend04.entity.Role;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.mapper.TrainerBookingMapper;
import com.example.project_backend04.repository.RoleRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrainerManagementService {
    
    private final TrainerBookingRepository trainerBookingRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public TrainerScheduleResponse getTrainerSchedule(Long trainerId, LocalDate fromDate, LocalDate toDate) {
        User trainer = findTrainerById(trainerId);
        
        List<TrainerBooking> bookings = trainerBookingRepository
            .findTrainerBookingsInDateRange(trainer, fromDate, toDate);
        Map<LocalDate, List<TrainerBooking>> bookingsByDate = bookings.stream()
            .collect(Collectors.groupingBy(TrainerBooking::getBookingDate));
        List<TrainerScheduleResponse.DailySchedule> dailySchedules = new ArrayList<>();
        LocalDate currentDate = fromDate;
        
        while (!currentDate.isAfter(toDate)) {
            List<TrainerBooking> dayBookings = bookingsByDate.getOrDefault(currentDate, new ArrayList<>());
            TrainerScheduleResponse.DailySchedule dailySchedule = createDailySchedule(currentDate, dayBookings);
            dailySchedules.add(dailySchedule);
            currentDate = currentDate.plusDays(1);
        }
        int totalBookings = bookings.size();
        int confirmedBookings = (int) bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .count();
        int pendingBookings = (int) bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.PENDING)
            .count();
        int completedBookings = (int) bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .count();
        List<String> availableDays = getAvailableDays(dailySchedules);
        LocalTime earliestStart = getEarliestStartTime(bookings);
        LocalTime latestEnd = getLatestEndTime(bookings);
        double averageBookingsPerDay = totalBookings / (double) dailySchedules.size();
        
        return TrainerScheduleResponse.builder()
            .trainerId(trainer.getId())
            .trainerName(trainer.getFullName())
            .trainerEmail(trainer.getEmail())
            .trainerPhone(trainer.getPhoneNumber())
            .trainerAvatar(trainer.getAvatar())
            .isActive(trainer.getIsActive())
            .fromDate(fromDate)
            .toDate(toDate)
            .dailySchedules(dailySchedules)
            .totalBookings(totalBookings)
            .confirmedBookings(confirmedBookings)
            .pendingBookings(pendingBookings)
            .completedBookings(completedBookings)
            .availableDays(availableDays)
            .earliestStart(earliestStart)
            .latestEnd(latestEnd)
            .averageBookingsPerDay(averageBookingsPerDay)
            .build();
    }
    public List<TrainerBookingResponse> getPendingBookingRequests(Long trainerId) {
        User trainer = findTrainerById(trainerId);
        
        List<TrainerBooking> pendingBookings = trainerBookingRepository
            .findPendingBookingsWithServiceInfo(trainerId);
        
        return pendingBookings.stream()
                .map(TrainerBookingMapper::toResponse)
                .collect(Collectors.toList());
    }
    public TrainerStatisticsResponse getTrainerStatistics(Long trainerId, LocalDate fromDate, LocalDate toDate) {
        User trainer = findTrainerById(trainerId);
        List<TrainerBooking> bookings = trainerBookingRepository
            .findTrainerBookingsInDateRange(trainer, fromDate, toDate);
        int totalBookings = bookings.size();
        int confirmedBookings = (int) bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .count();
        int completedBookings = (int) bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .count();
        int cancelledBookings = (int) bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
            .count();
        int pendingBookings = (int) bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.PENDING)
            .count();
        int rejectedBookings = (int) bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
            .count();
        
        // Calculate rates
        double completionRate = confirmedBookings > 0 ? (completedBookings / (double) confirmedBookings) * 100 : 0;
        double confirmationRate = totalBookings > 0 ? (confirmedBookings / (double) totalBookings) * 100 : 0;

        Double averageRating = trainerBookingRepository.findAverageRatingByTrainer(trainerId);
        averageRating = averageRating != null ? averageRating : 0.0;
        
        int totalRatings = (int) bookings.stream()
            .filter(b -> b.getRating() != null)
            .count();

        int totalDays = (int) fromDate.datesUntil(toDate.plusDays(1)).count();
        double averageBookingsPerDay = totalBookings / (double) totalDays;

        Map<String, Integer> serviceBreakdown = bookings.stream()
            .filter(b -> b.getServiceRegistration() != null && b.getServiceRegistration().getGymService() != null)
            .collect(Collectors.groupingBy(
                b -> b.getServiceRegistration().getGymService().getName(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        Map<String, Integer> bookingsByDayOfWeek = bookings.stream()
            .collect(Collectors.groupingBy(
                b -> b.getBookingDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        Map<Integer, Integer> bookingsByHour = bookings.stream()
            .collect(Collectors.groupingBy(
                b -> b.getStartTime().getHour(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Client statistics
        Set<Long> uniqueClientIds = bookings.stream()
            .map(b -> b.getUser().getId())
            .collect(Collectors.toSet());
        int uniqueClients = uniqueClientIds.size();
        
        return TrainerStatisticsResponse.builder()
            .trainerId(trainer.getId())
            .trainerName(trainer.getFullName())
            .trainerEmail(trainer.getEmail())
            .isActive(trainer.getIsActive())
            .fromDate(fromDate)
            .toDate(toDate)
            .totalDays(totalDays)
            .totalBookings(totalBookings)
            .confirmedBookings(confirmedBookings)
            .completedBookings(completedBookings)
            .cancelledBookings(cancelledBookings)
            .pendingBookings(pendingBookings)
            .rejectedBookings(rejectedBookings)
            .completionRate(completionRate)
            .confirmationRate(confirmationRate)
            .averageRating(averageRating)
            .totalRatings(totalRatings)
            .averageBookingsPerDay(averageBookingsPerDay)
            .serviceBreakdown(serviceBreakdown)
            .bookingsByDayOfWeek(bookingsByDayOfWeek)
            .bookingsByHour(bookingsByHour)
            .uniqueClients(uniqueClients)
            .totalRevenue(0)
            .averageRevenuePerBooking(0)
            .serviceRevenue(new HashMap<>())
            .totalWorkingHours(0)
            .averageSessionDuration(0)
            .returningClients(0)
            .clientRetentionRate(0)
            .topClients(new ArrayList<>())
            .monthlyTrends(new ArrayList<>())
            .build();
    }

    /**
     * Get overview of all trainers for a specific date
     */
    public List<TrainerScheduleResponse> getTrainersOverview(LocalDate date) {
        Role trainerRole = roleRepository.findRoleByName("TRAINER")
            .orElseThrow(() -> new RuntimeException("TRAINER role not found"));
        
        List<User> trainers = userRepository.findByRoleAndIsActiveTrueOrderByCreateDateDesc(trainerRole);
        
        return trainers.stream()
            .map(trainer -> getTrainerSchedule(trainer.getId(), date, date))
            .collect(Collectors.toList());
    }

    public List<TrainerStatisticsResponse> getWorkloadSummary(LocalDate fromDate, LocalDate toDate) {
        Role trainerRole = roleRepository.findRoleByName("TRAINER")
            .orElseThrow(() -> new RuntimeException("TRAINER role not found"));
        
        List<User> trainers = userRepository.findByRoleAndIsActiveTrueOrderByCreateDateDesc(trainerRole);
        
        return trainers.stream()
            .map(trainer -> getTrainerStatistics(trainer.getId(), fromDate, toDate))
            .sorted((s1, s2) -> Double.compare(s2.getAverageBookingsPerDay(), s1.getAverageBookingsPerDay()))
            .collect(Collectors.toList());
    }

    // Helper methods
    private User findTrainerById(Long trainerId) {
        User trainer = userRepository.findById(trainerId)
            .orElseThrow(() -> new RuntimeException("Trainer not found"));
        
        if (trainer.getRole() == null || !"TRAINER".equals(trainer.getRole().getName())) {
            throw new RuntimeException("User is not a trainer");
        }
        
        return trainer;
    }

    private TrainerScheduleResponse.DailySchedule createDailySchedule(LocalDate date, List<TrainerBooking> dayBookings) {
        // Sort bookings by start time
        dayBookings.sort(Comparator.comparing(TrainerBooking::getStartTime));
        
        // Create booking slots
        List<TrainerScheduleResponse.DailySchedule.BookingSlot> bookingSlots = dayBookings.stream()
            .map(this::mapToBookingSlot)
            .collect(Collectors.toList());
        
        // Generate available slots (simplified - between bookings)
        List<TrainerScheduleResponse.DailySchedule.AvailableSlot> availableSlots = 
            generateAvailableSlots(dayBookings);
        
        // Check for conflicts
        boolean hasConflicts = hasTimeConflicts(dayBookings);
        
        return TrainerScheduleResponse.DailySchedule.builder()
            .date(date)
            .dayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
            .bookings(bookingSlots)
            .availableSlots(availableSlots)
            .totalBookings(dayBookings.size())
            .hasConflicts(hasConflicts)
            .build();
    }

    private TrainerScheduleResponse.DailySchedule.BookingSlot mapToBookingSlot(TrainerBooking booking) {
        return TrainerScheduleResponse.DailySchedule.BookingSlot.builder()
            .bookingId(booking.getId())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .clientName(booking.getUser().getFullName())
            .clientPhone(booking.getUser().getPhoneNumber())
            .serviceName(booking.getServiceRegistration() != null && 
                        booking.getServiceRegistration().getGymService() != null ?
                        booking.getServiceRegistration().getGymService().getName() : "Direct Booking")
            .status(booking.getStatus().name())
            .notes(booking.getNotes())
            .isServiceLinked(booking.getServiceRegistration() != null)
            .serviceRegistrationId(booking.getServiceRegistration() != null ? 
                                 booking.getServiceRegistration().getId() : null)
            .build();
    }

    private List<TrainerScheduleResponse.DailySchedule.AvailableSlot> generateAvailableSlots(List<TrainerBooking> dayBookings) {
        List<TrainerScheduleResponse.DailySchedule.AvailableSlot> availableSlots = new ArrayList<>();
        
        // Simplified: assume working hours 8:00 - 20:00
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(20, 0);
        
        if (dayBookings.isEmpty()) {
            availableSlots.add(TrainerScheduleResponse.DailySchedule.AvailableSlot.builder()
                .startTime(workStart)
                .endTime(workEnd)
                .durationMinutes(720) // 12 hours
                .isRecommended(true)
                .build());
            return availableSlots;
        }
        
        // Find gaps between bookings
        LocalTime currentTime = workStart;
        for (TrainerBooking booking : dayBookings) {
            if (currentTime.isBefore(booking.getStartTime())) {
                availableSlots.add(TrainerScheduleResponse.DailySchedule.AvailableSlot.builder()
                    .startTime(currentTime)
                    .endTime(booking.getStartTime())
                    .durationMinutes((int) java.time.Duration.between(currentTime, booking.getStartTime()).toMinutes())
                    .isRecommended(java.time.Duration.between(currentTime, booking.getStartTime()).toMinutes() >= 60)
                    .build());
            }
            currentTime = booking.getEndTime().isAfter(currentTime) ? booking.getEndTime() : currentTime;
        }
        
        // Add slot after last booking if there's time left
        if (currentTime.isBefore(workEnd)) {
            availableSlots.add(TrainerScheduleResponse.DailySchedule.AvailableSlot.builder()
                .startTime(currentTime)
                .endTime(workEnd)
                .durationMinutes((int) java.time.Duration.between(currentTime, workEnd).toMinutes())
                .isRecommended(java.time.Duration.between(currentTime, workEnd).toMinutes() >= 60)
                .build());
        }
        
        return availableSlots;
    }

    private boolean hasTimeConflicts(List<TrainerBooking> dayBookings) {
        for (int i = 0; i < dayBookings.size() - 1; i++) {
            TrainerBooking current = dayBookings.get(i);
            TrainerBooking next = dayBookings.get(i + 1);
            
            if (current.getEndTime().isAfter(next.getStartTime())) {
                return true;
            }
        }
        return false;
    }

    private List<String> getAvailableDays(List<TrainerScheduleResponse.DailySchedule> dailySchedules) {
        return dailySchedules.stream()
            .filter(schedule -> !schedule.getAvailableSlots().isEmpty())
            .map(schedule -> schedule.getDate().getDayOfWeek().name())
            .distinct()
            .collect(Collectors.toList());
    }

    private LocalTime getEarliestStartTime(List<TrainerBooking> bookings) {
        return bookings.stream()
            .map(TrainerBooking::getStartTime)
            .min(LocalTime::compareTo)
            .orElse(LocalTime.of(8, 0));
    }

    private LocalTime getLatestEndTime(List<TrainerBooking> bookings) {
        return bookings.stream()
            .map(TrainerBooking::getEndTime)
            .max(LocalTime::compareTo)
            .orElse(LocalTime.of(20, 0));
    }
}