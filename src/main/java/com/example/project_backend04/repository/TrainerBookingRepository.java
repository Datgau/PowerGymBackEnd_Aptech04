package com.example.project_backend04.repository;

import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
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
    
    // ── Conflict detection ──────────────────────────────────────────────────────

    /**
     * Kiểm tra trainer bị trùng lịch (PENDING hoặc CONFIRMED).
     * Sử dụng khi tạo / xác nhận booking mới.
     */
    @Query("""
SELECT tb FROM TrainerBooking tb
WHERE tb.trainer.id = :trainerId
AND tb.bookingDate = :date
AND tb.status IN :statuses
AND (tb.startTime < :endTime AND tb.endTime > :startTime)
""")
    List<TrainerBooking> findConflictingBookingsForTrainer(
            @Param("trainerId") Long trainerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<BookingStatus> statuses
    );
    /**
     * Kiểm tra USER bị trùng lịch (phòng trường hợp một người đặt hai trainer cùng giờ).
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "WHERE tb.user.id = :userId "
         + "AND tb.bookingDate = :date "
         + "AND tb.status IN ('PENDING', 'CONFIRMED') "
         + "AND (tb.startTime < :endTime AND tb.endTime > :startTime)")
    List<TrainerBooking> findConflictingBookingsForUser(
        @Param("userId")    Long userId,
        @Param("date")      LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime")   LocalTime endTime);

    /**
     * Legacy - giữ lại để không break code cũ (chỉ check CONFIRMED).
     * Nên dùng findConflictingBookingsForTrainer thay thế.
     */
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
        @Param("status") BookingStatus status);
    
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
        @Param("statuses") List<BookingStatus> statuses);
    
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
        @Param("status") BookingStatus status);
    
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
    @Query("""
        SELECT EXTRACT(MONTH FROM tb.bookingDate), COUNT(tb)
        FROM TrainerBooking tb
        WHERE tb.trainer.id = :trainerId
        AND EXTRACT(YEAR FROM tb.bookingDate) = :year
        AND tb.status = :status
        GROUP BY EXTRACT(MONTH FROM tb.bookingDate)
        ORDER BY EXTRACT(MONTH FROM tb.bookingDate)
""")
    List<Object[]> findMonthlyBookingStatistics(
            @Param("trainerId") Long trainerId,
            @Param("year") int year,
            @Param("status") BookingStatus status
    );
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
     * Kiểm tra trainer trùng lịch khi reschedule, loại trừ booking hiện tại.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "WHERE tb.trainer.id = :trainerId "
         + "AND tb.bookingDate = :date "
         + "AND tb.status IN :statuses "
         + "AND tb.id <> :excludeBookingId "
         + "AND (tb.startTime < :endTime AND tb.endTime > :startTime)")
    List<TrainerBooking> findConflictingBookingsExcluding(
        @Param("trainerId")        Long trainerId,
        @Param("date")             LocalDate date,
        @Param("startTime")        LocalTime startTime,
        @Param("endTime")          LocalTime endTime,
        @Param("excludeBookingId") Long excludeBookingId);
    
    /**
     * Legacy - giữ lại để không break code cũ.
     * Nên dùng findConflictingBookingsForTrainer thay thế.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "WHERE tb.trainer.id = :trainerId "
         + "AND tb.bookingDate = :date "
         + "AND tb.status = 'CONFIRMED' "
         + "AND (tb.startTime < :endTime AND tb.endTime > :startTime)")
    List<TrainerBooking> findConflictingBookingsByTrainerId(
        @Param("trainerId") Long trainerId,
        @Param("date")      LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime")   LocalTime endTime);
    
    /**
     * Tìm booking theo service registration (trainer có thể null → LEFT JOIN).
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "LEFT JOIN FETCH tb.trainer t "
         + "JOIN FETCH tb.serviceRegistration sr "
         + "JOIN FETCH sr.gymService gs "
         + "WHERE sr.id = :serviceRegistrationId "
         + "ORDER BY tb.bookingDate DESC, tb.startTime DESC")
    List<TrainerBooking> findByServiceRegistrationWithDetails(
        @Param("serviceRegistrationId") Long serviceRegistrationId);

    // ── Unassigned bookings (admin assignment) ──────────────────────────────────

    /**
     * Tìm tất cả booking chưa được gán trainer (trainer IS NULL).
     * Admin dùng để xắp xếp lịch cho khách hàng không chọn trainer.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "LEFT JOIN FETCH tb.serviceRegistration sr "
         + "LEFT JOIN FETCH sr.gymService gs "
         + "WHERE tb.trainer IS NULL AND tb.status = 'PENDING' "
         + "ORDER BY tb.createdAt ASC")
    List<TrainerBooking> findUnassignedPendingBookings();

    /**
     * Tìm booking chưa có trainer, lọc theo service category.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "LEFT JOIN FETCH tb.serviceRegistration sr "
         + "LEFT JOIN FETCH sr.gymService gs "
         + "LEFT JOIN FETCH gs.category cat "
         + "WHERE tb.trainer IS NULL "
         + "AND tb.status = 'PENDING' "
         + "AND (:categoryId IS NULL OR cat.id = :categoryId) "
         + "ORDER BY tb.createdAt ASC")
    List<TrainerBooking> findUnassignedPendingBookingsByCategory(
        @Param("categoryId") Long categoryId);

    // ── Rejected bookings ───────────────────────────────────────────────────────

    /**
     * Tìm tất cả booking bị trainer từ chối.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "JOIN FETCH tb.trainer t "
         + "WHERE tb.status = 'REJECTED' "
         + "ORDER BY tb.rejectedAt DESC")
    List<TrainerBooking> findAllRejectedBookings();

    /** Booking bị reject của một trainer cụ thể (thống kê / dashboard trainer). */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "WHERE tb.trainer.id = :trainerId AND tb.status = 'REJECTED' "
         + "ORDER BY tb.rejectedAt DESC")
    List<TrainerBooking> findRejectedBookingsByTrainer(@Param("trainerId") Long trainerId);

    /** Booking bị reject của một user (hiển thị cho user biết). */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "WHERE tb.user.id = :userId AND tb.status = 'REJECTED' "
         + "ORDER BY tb.rejectedAt DESC")
    List<TrainerBooking> findRejectedBookingsByUser(@Param("userId") Long userId);

    // ── Admin-assigned bookings ─────────────────────────────────────────────────

    /**
     * Tìm tất cả booking do Admin chủ động gán trainer.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "JOIN FETCH tb.trainer t "
         + "WHERE tb.isAssignedByAdmin = true "
         + "ORDER BY tb.updatedAt DESC")
    List<TrainerBooking> findAdminAssignedBookings();

    // ── Trainer dashboard ───────────────────────────────────────────────────────

    /**
     * Tất cả PENDING booking có trainer được gán → trainer cần xác nhận.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "LEFT JOIN FETCH tb.serviceRegistration sr "
         + "LEFT JOIN FETCH sr.gymService gs "
         + "WHERE tb.trainer.id = :trainerId AND tb.status = 'PENDING' "
         + "ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findPendingBookingsForTrainer(@Param("trainerId") Long trainerId);

    /**
     * Upcoming CONFIRMED bookings của trainer kể từ hôm nay.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "WHERE tb.trainer.id = :trainerId "
         + "AND tb.bookingDate >= :fromDate "
         + "AND tb.status = 'CONFIRMED' "
         + "ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findUpcomingConfirmedForTrainer(
        @Param("trainerId") Long trainerId,
        @Param("fromDate")  LocalDate fromDate);

    // ── User "My Bookings" ──────────────────────────────────────────────────────

    /**
     * Tìm tất cả booking của user theo status (dùng cho My Bookings page).
     * Nếu status = null thì trả về tất cả.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "LEFT JOIN FETCH tb.trainer t "
         + "LEFT JOIN FETCH tb.serviceRegistration sr "
         + "LEFT JOIN FETCH sr.gymService gs "
         + "WHERE tb.user.id = :userId "
         + "AND (:status IS NULL OR tb.status = :status) "
         + "ORDER BY tb.bookingDate DESC, tb.startTime DESC")
    List<TrainerBooking> findByUserIdAndOptionalStatus(
        @Param("userId") Long userId,
        @Param("status") BookingStatus status);
}
