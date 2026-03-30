package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Booking;
import com.example.project_backend04.entity.ClassSchedule;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    int countByUserAndStatus(User user, BookingStatus status);

    List<Booking> findByUser(User user);

    Optional<Booking> findByBookingId(String bookingId);

    List<Booking> findByClassSchedule(ClassSchedule classSchedule);

    boolean existsByUserAndClassSchedule(User user, ClassSchedule classSchedule);
}
