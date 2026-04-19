package com.example.project_backend04.repository;

import com.example.project_backend04.entity.TrainerLeaveRequest;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrainerLeaveRequestRepository extends JpaRepository<TrainerLeaveRequest, Long> {
    
    /**
     * Find all leave requests for a specific trainer
     */
    List<TrainerLeaveRequest> findByTrainerOrderByCreatedAtDesc(User trainer);
    
    /**
     * Find leave requests by trainer and status
     */
    List<TrainerLeaveRequest> findByTrainerAndStatusOrderByCreatedAtDesc(User trainer, LeaveRequestStatus status);
    
    /**
     * Find all pending leave requests (for admin)
     */
    List<TrainerLeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveRequestStatus status);
    
    /**
     * Find leave requests for a trainer on a specific date
     */
    @Query("SELECT lr FROM TrainerLeaveRequest lr " +
           "WHERE lr.trainer = :trainer " +
           "AND lr.leaveDate = :date " +
           "AND lr.status = :status")
    List<TrainerLeaveRequest> findByTrainerAndDateAndStatus(
        @Param("trainer") User trainer,
        @Param("date") LocalDate date,
        @Param("status") LeaveRequestStatus status
    );
    
    /**
     * Find approved leave requests for a trainer in a date range
     */
    @Query("SELECT lr FROM TrainerLeaveRequest lr " +
           "WHERE lr.trainer.id = :trainerId " +
           "AND lr.leaveDate BETWEEN :fromDate AND :toDate " +
           "AND lr.status = 'APPROVED' " +
           "ORDER BY lr.leaveDate, lr.startTime")
    List<TrainerLeaveRequest> findApprovedLeavesByTrainerAndDateRange(
        @Param("trainerId") Long trainerId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
    
    /**
     * Check if trainer has approved leave on a specific date and time
     */
    @Query("SELECT COUNT(lr) > 0 FROM TrainerLeaveRequest lr " +
           "WHERE lr.trainer.id = :trainerId " +
           "AND lr.leaveDate = :date " +
           "AND lr.status = 'APPROVED' " +
           "AND (lr.leaveType = 'FULL_DAY' " +
           "     OR (lr.startTime <= :endTime AND lr.endTime >= :startTime))")
    boolean hasApprovedLeaveOnDateTime(
        @Param("trainerId") Long trainerId,
        @Param("date") LocalDate date,
        @Param("startTime") java.time.LocalTime startTime,
        @Param("endTime") java.time.LocalTime endTime
    );
}
