package com.example.project_backend04.repository;

import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // Find by bookingId (String field, not primary key)
    Optional<TrainerBooking> findByBookingId(String bookingId);

    // Find bookings by trainer
    List<TrainerBooking> findByTrainerOrderByBookingDateDescStartTimeDesc(User trainer);

    // Find upcoming bookings for user
    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.user = :user AND tb.bookingDate >= :currentDate AND tb.status = com.example.project_backend04.enums.BookingStatus.CONFIRMED ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findUpcomingBookingsByUser(@Param("user") User user, @Param("currentDate") LocalDate currentDate);

    // Find upcoming bookings for trainer
    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.trainer = :trainer AND tb.bookingDate >= :currentDate AND tb.status = com.example.project_backend04.enums.BookingStatus.CONFIRMED ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findUpcomingBookingsByTrainer(@Param("trainer") User trainer, @Param("currentDate") LocalDate currentDate);

    // ── Conflict detection ──────────────────────────────────────────────────────

    /**
     * Kiểm tra trainer bị trùng lịch (PENDING hoặc CONFIRMED).
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
         + "AND tb.status IN (com.example.project_backend04.enums.BookingStatus.PENDING, com.example.project_backend04.enums.BookingStatus.CONFIRMED) "
         + "AND (tb.startTime < :endTime AND tb.endTime > :startTime)")
    List<TrainerBooking> findConflictingBookingsForUser(
        @Param("userId")    Long userId,
        @Param("date")      LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime")   LocalTime endTime);


    @Query("SELECT tb FROM TrainerBooking tb WHERE tb.trainer = :trainer AND tb.bookingDate BETWEEN :startDate AND :endDate AND tb.status = com.example.project_backend04.enums.BookingStatus.CONFIRMED ORDER BY tb.bookingDate ASC, tb.startTime ASC")
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
           "WHERE tb.trainer.id = :trainerId AND tb.status = com.example.project_backend04.enums.BookingStatus.PENDING " +
           "ORDER BY tb.createdAt")
    List<TrainerBooking> findPendingBookingsWithServiceInfo(@Param("trainerId") Long trainerId);
    


    @Query("SELECT AVG(tb.rating) FROM TrainerBooking tb " +
           "WHERE tb.trainer.id = :trainerId AND tb.rating IS NOT NULL")
    Double findAverageRatingByTrainer(@Param("trainerId") Long trainerId);
    


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
        @Param("excludeBookingId") Long excludeBookingId,
        @Param("statuses")         List<BookingStatus> statuses);
    

    @Query("SELECT tb FROM TrainerBooking tb "
         + "WHERE tb.trainer.id = :trainerId "
         + "AND tb.bookingDate = :date "
         + "AND tb.status = com.example.project_backend04.enums.BookingStatus.CONFIRMED "
         + "AND (tb.startTime < :endTime AND tb.endTime > :startTime)")
    List<TrainerBooking> findConflictingBookingsByTrainerId(
        @Param("trainerId") Long trainerId,
        @Param("date")      LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime")   LocalTime endTime);
    

    /**
     * Tìm tất cả booking chưa được gán trainer (trainer IS NULL).
     * Admin dùng để xắp xếp lịch cho khách hàng không chọn trainer.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "LEFT JOIN FETCH tb.serviceRegistration sr "
         + "LEFT JOIN FETCH sr.gymService gs "
         + "WHERE tb.trainer IS NULL AND tb.status = com.example.project_backend04.enums.BookingStatus.PENDING "
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
         + "AND tb.status = com.example.project_backend04.enums.BookingStatus.PENDING "
         + "AND (:categoryId IS NULL OR cat.id = :categoryId) "
         + "ORDER BY tb.createdAt ASC")
    List<TrainerBooking> findUnassignedPendingBookingsByCategory(
        @Param("categoryId") Long categoryId);


    /**
     * Tìm tất cả booking bị trainer từ chối.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "JOIN FETCH tb.trainer t "
         + "WHERE tb.status = com.example.project_backend04.enums.BookingStatus.REJECTED "
         + "ORDER BY tb.rejectedAt DESC")
    List<TrainerBooking> findAllRejectedBookings();



    /**
     * Chỉ hiển thị bookings có payment status = SUCCESS.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "LEFT JOIN FETCH tb.serviceRegistration sr "
         + "LEFT JOIN FETCH sr.gymService gs "
         + "LEFT JOIN FETCH tb.paymentOrder po "
         + "WHERE tb.trainer.id = :trainerId "
         + "AND tb.status = com.example.project_backend04.enums.BookingStatus.PENDING "
         + "AND po.status = 'SUCCESS' "
         + "ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findPendingBookingsForTrainer(@Param("trainerId") Long trainerId);

    /**
     * Upcoming CONFIRMED bookings của trainer kể từ hôm nay.
     */
    @Query("SELECT tb FROM TrainerBooking tb "
         + "JOIN FETCH tb.user u "
         + "WHERE tb.trainer.id = :trainerId "
         + "AND tb.bookingDate >= :fromDate "
         + "AND tb.status = com.example.project_backend04.enums.BookingStatus.CONFIRMED "
         + "ORDER BY tb.bookingDate ASC, tb.startTime ASC")
    List<TrainerBooking> findUpcomingConfirmedForTrainer(
        @Param("trainerId") Long trainerId,
        @Param("fromDate")  LocalDate fromDate);


    /**
     * Tìm tất cả booking của user theo status (dùng cho My Bookings page).
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
