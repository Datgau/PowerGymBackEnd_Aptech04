package com.example.project_backend04.repository;

import com.example.project_backend04.entity.ClassSchedule;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    
    List<ClassSchedule> findByDateAndStatus(LocalDate date, ClassSchedule.ScheduleStatus status);
    
    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.date >= :startDate AND cs.date <= :endDate AND cs.status = :status ORDER BY cs.date ASC, cs.startTime ASC")
    List<ClassSchedule> findByDateRangeAndStatus(@Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate, 
                                                @Param("status") ClassSchedule.ScheduleStatus status);
    
    List<ClassSchedule> findByGymServiceAndStatus(GymService gymService, ClassSchedule.ScheduleStatus status);
    
    List<ClassSchedule> findByInstructorAndStatus(Instructor instructor, ClassSchedule.ScheduleStatus status);
    
    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.date = :date AND cs.instructor = :instructor AND cs.status = 'SCHEDULED'")
    List<ClassSchedule> findByDateAndInstructor(@Param("date") LocalDate date, @Param("instructor") Instructor instructor);
    
    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.status = 'SCHEDULED' AND cs.currentParticipants < cs.maxParticipants AND CONCAT(cs.date, ' ', cs.startTime) > :currentDateTime ORDER BY cs.date ASC, cs.startTime ASC")
    List<ClassSchedule> findAvailableSchedules(@Param("currentDateTime") LocalDateTime currentDateTime);
    
    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.date >= :currentDate AND cs.status = 'SCHEDULED' ORDER BY cs.date ASC, cs.startTime ASC")
    List<ClassSchedule> findUpcomingSchedules(@Param("currentDate") LocalDate currentDate);
    
    Optional<ClassSchedule> findByIdAndStatus(Long id, ClassSchedule.ScheduleStatus status);
}