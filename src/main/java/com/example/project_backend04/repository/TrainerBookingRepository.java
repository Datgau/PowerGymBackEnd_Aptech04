package com.example.project_backend04.repository;

import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerBookingRepository extends JpaRepository<TrainerBooking, Long> {
    
    // Find bookings by user
    List<TrainerBooking> findByUserOrderByBookingDateDescStartTimeDesc(User user);
    
    // Find bookings by trainer
    List<TrainerBooking> findByTrainerOrderByBookingDateDescStartTimeDesc(User trainer);
    
    // Find upcoming bookings for user
    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.user = :user AND tb.bookingDate >= :currentDate AND tb.status = 'CONFIRMED' ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findUpcomingBookingsByUser(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    // Find upcoming bookings for trainer
    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.trainer = :trainer AND tb.bookingDate >= :currentDate AND tb.status = 'CONFIRMED' ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findUpcomingBookingsByTrainer(@Param("trainer") User trainer, @Param("currentDate") LocalDate currentDate);
    
    // Check for time conflicts for a trainer on a specific date
    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.trainer = :trainer AND tb.bookingDate = :date AND tb.status = 'CONFIRMED' AND ((tb.startTime < :endTime) AND (tb.endTime > :startTime))")
    List<TrainerBooking> findConflictingBookings(@Param("trainer") User trainer, 
                                                @Param("date") LocalDate date, 
                                                @Param("startTime") LocalTime startTime, 
                                                @Param("endTime") LocalTime endTime);
    
    // Find trainer's bookings for a specific date
    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.trainer = :trainer AND tb.bookingDate = :date AND tb.status = 'CONFIRMED' ORDER BY tb.startTime ASC")
    List<TrainerBooking> findTrainerBookingsForDate(@Param("trainer") User trainer, @Param("date") LocalDate date);
    
    // Find booking by bookingId
    Optional<TrainerBooking> findByBookingId(String bookingId);
    
    // Check if user already has booking with trainer on specific date
    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.user = :user AND tb.trainer = :trainer AND tb.bookingDate = :date AND tb.status = 'CONFIRMED'")
    Optional<TrainerBooking> findActiveBookingByUserAndTrainerAndDate(@Param("user") User user, 
                                                                     @Param("trainer") User trainer, 
                                                                     @Param("date") LocalDate date);
    
    // Get trainer's availability for date range
    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.trainer = :trainer AND tb.bookingDate BETWEEN :startDate AND :endDate AND tb.status = 'CONFIRMED' ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findTrainerBookingsInDateRange(@Param("trainer") User trainer, 
                                                       @Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate);
    
    // NEW METHODS for service integration
    
    /**
     * Find bookings by service registration ID
     */
    List<TrainerBooking> findByServiceRegistration_Id(Long serviceRegistrationId);

    /**
     * Find bookings by trainer, date and status
     */
    @Query("SELECT tb FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId " +
           "AND tb.bookingDate = :date " +
           "AND tb.status = :status " +
           "ORDER BY tb.startTime")
    List<TrainerBooking> findByTrainerIdAndBookingDateAndStatus(
        @Param("trainerId") Long trainerId,
        @Param("date") LocalDate date,
        @Param("status") TrainerBooking.BookingStatus status);
    
    /**
     * Find bookings by trainer in date range with specific statuses
     */
    @Query("SELECT tb FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId " +
           "AND tb.bookingDate BETWEEN :fromDate AND :toDate " +
           "AND tb.status IN :statuses " +
           "ORDER BY tb.bookingDate, tb.startTime")
    List<TrainerBooking> findByTrainerIdAndDateRangeAndStatuses(
        @Param("trainerId") Long trainerId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("statuses") List<TrainerBooking.BookingStatus> statuses);
    
    /**
     * Find pending bookings with service information
     */
    @Query("SELECT tb FROM TrainerBooking tb " +
           "LEFT JOIN FETCH tb.serviceRegistration sr " +
           "LEFT JOIN FETCH sr.gymService gs " +
           "WHERE tb.trainer.id = :trainerId AND tb.status = 'PENDING' " +
           "ORDER BY tb.createdAt")
    List<TrainerBooking> findPendingBookingsWithServiceInfo(@Param("trainerId") Long trainerId);
    
    /**
     * Count bookings by trainer, date range and status
     */
    @Query("SELECT COUNT(tb) FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId " +
           "AND tb.bookingDate BETWEEN :fromDate AND :toDate " +
           "AND tb.status = :status")
    Long countByTrainerAndDateRangeAndStatus(
        @Param("trainerId") Long trainerId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("status") TrainerBooking.BookingStatus status);
    
    /**
     * Count all bookings by trainer and date range
     */
    @Query("SELECT COUNT(tb) FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId " +
           "AND tb.bookingDate BETWEEN :fromDate AND :toDate")
    Long countByTrainerAndDateRange(
        @Param("trainerId") Long trainerId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);
    
    /**
     * Find average rating by trainer
     */
    @Query("SELECT AVG(tb.rating) FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId AND tb.rating IS NOT NULL")
    Double findAverageRatingByTrainer(@Param("trainerId") Long trainerId);
    
    /**
     * Find monthly booking statistics
     */
    @Query("SELECT EXTRACT(MONTH FROM tb.bookingDate) as month, COUNT(tb) as count " +
           "FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId " +
           "AND EXTRACT(YEAR FROM tb.bookingDate) = :year " +
           "AND tb.status = 'COMPLETED' " +
           "GROUP BY EXTRACT(MONTH FROM tb.bookingDate) " +
           "ORDER BY month")
    List<Object[]> findMonthlyBookingStatistics(
        @Param("trainerId") Long trainerId, 
        @Param("year") int year);
    
    /**
     * Find bookings with full details including service registration
     */
    @Query("SELECT tb FROM TrainerBooking tb " +
           "JOIN FETCH tb.user u " +
           "JOIN FETCH tb.trainer t " +
           "LEFT JOIN FETCH tb.serviceRegistration sr " +
           "LEFT JOIN FETCH sr.gymService gs " +
           "WHERE tb.id = :bookingId")
    Optional<TrainerBooking> findByIdWithFullDetails(@Param("bookingId") Long bookingId);
    
    /**
     * Find conflicting bookings excluding specific booking
     */
    @Query("SELECT tb FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId " +
           "AND tb.bookingDate = :date " +
           "AND tb.status = 'CONFIRMED' " +
           "AND tb.id != :excludeBookingId " +
           "AND ((tb.startTime < :endTime) AND (tb.endTime > :startTime))")
    List<TrainerBooking> findConflictingBookingsExcluding(
        @Param("trainerId") Long trainerId,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("excludeBookingId") Long excludeBookingId);
    
    /**
     * Find conflicting bookings by trainer ID
     */
    @Query("SELECT tb FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId " +
           "AND tb.bookingDate = :date " +
           "AND tb.status = 'CONFIRMED' " +
           "AND ((tb.startTime < :endTime) AND (tb.endTime > :startTime))")
    List<TrainerBooking> findConflictingBookingsByTrainerId(
        @Param("trainerId") Long trainerId,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime);
    
    /**
     * Find bookings by service registration with trainer and service details
     */
    @Query("SELECT tb FROM TrainerBooking tb " +
           "JOIN FETCH tb.trainer t " +
           "JOIN FETCH tb.serviceRegistration sr " +
           "JOIN FETCH sr.gymService gs " +
           "WHERE sr.id = :serviceRegistrationId " +
           "ORDER BY tb.bookingDate DESC, tb.startTime DESC")
    List<TrainerBooking> findByServiceRegistrationWithDetails(@Param("serviceRegistrationId") Long serviceRegistrationId);
}