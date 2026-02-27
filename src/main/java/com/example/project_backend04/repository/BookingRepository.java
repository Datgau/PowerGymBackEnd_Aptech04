package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Booking;
import com.example.project_backend04.entity.ClassSchedule;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserOrderByBookingDateDesc(User user);
    
    List<Booking> findByUserAndStatus(User user, Booking.BookingStatus status);
    
    List<Booking> findByClassScheduleAndStatus(ClassSchedule classSchedule, Booking.BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.user = :user AND b.classSchedule.date >= :currentDate AND b.status = 'CONFIRMED' ORDER BY b.classSchedule.date ASC, b.classSchedule.startTime ASC")
    List<Booking> findUpcomingBookings(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT b FROM Booking b WHERE b.user = :user AND b.classSchedule.date < :currentDate ORDER BY b.classSchedule.date DESC")
    List<Booking> findPastBookings(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    Optional<Booking> findByBookingId(String bookingId);
    
    @Query("SELECT b FROM Booking b WHERE b.user = :user AND b.classSchedule = :schedule AND b.status = 'CONFIRMED'")
    Optional<Booking> findActiveBooking(@Param("user") User user, @Param("schedule") ClassSchedule schedule);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.classSchedule = :schedule AND b.status = 'CONFIRMED'")
    Long countConfirmedBookings(@Param("schedule") ClassSchedule schedule);
    
    @Query("SELECT b FROM Booking b WHERE b.classSchedule.date = :date AND b.status = 'CONFIRMED'")
    List<Booking> findBookingsForDate(@Param("date") LocalDate date);
}