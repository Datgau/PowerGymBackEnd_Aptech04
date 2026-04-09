package com.example.project_backend04.repository;

import com.example.project_backend04.entity.TrainerWorkingHours;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerWorkingHoursRepository extends JpaRepository<TrainerWorkingHours, Long> {

    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId AND twh.isActive = true "
         + "ORDER BY twh.dayOfWeek, twh.slotIndex")
    List<TrainerWorkingHours> findActiveSlotsByTrainer(@Param("trainerId") Long trainerId);

    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "ORDER BY twh.dayOfWeek, twh.slotIndex")
    List<TrainerWorkingHours> findAllSlotsByTrainer(@Param("trainerId") Long trainerId);

    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.dayOfWeek = :dayOfWeek "
         + "AND twh.isActive = true "
         + "ORDER BY twh.slotIndex")
    List<TrainerWorkingHours> findSlotsByTrainerAndDay(
        @Param("trainerId")  Long trainerId,
        @Param("dayOfWeek")  DayOfWeek dayOfWeek);

    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.dayOfWeek = :dayOfWeek "
         + "AND twh.isActive = true "
         + "AND twh.isDayOff = false "
         + "AND twh.startTime <= :startTime "
         + "AND twh.endTime >= :endTime")
    List<TrainerWorkingHours> findCoveringSlots(
        @Param("trainerId")  Long trainerId,
        @Param("dayOfWeek")  DayOfWeek dayOfWeek,
        @Param("startTime")  LocalTime startTime,
        @Param("endTime")    LocalTime endTime);

    @Query("SELECT COUNT(twh) > 0 FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.dayOfWeek = :dayOfWeek "
         + "AND twh.isDayOff = true "
         + "AND twh.isActive = true")
    boolean isTrainerDayOff(
        @Param("trainerId") Long trainerId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.dayOfWeek = :dayOfWeek "
         + "AND twh.slotIndex = :slotIndex")
    Optional<TrainerWorkingHours> findByTrainerAndDayAndSlot(
        @Param("trainerId")  Long trainerId,
        @Param("dayOfWeek")  DayOfWeek dayOfWeek,
        @Param("slotIndex")  Integer slotIndex);

    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id IN :trainerIds "
         + "AND twh.isActive = true "
         + "AND twh.isDayOff = false "
         + "ORDER BY twh.trainer.id, twh.dayOfWeek, twh.slotIndex")
    List<TrainerWorkingHours> findActiveSlotsByTrainers(
        @Param("trainerIds") List<Long> trainerIds);

    @Query("SELECT COUNT(twh) FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.isActive = true "
         + "AND twh.isDayOff = false")
    Long countActiveSlotsByTrainer(@Param("trainerId") Long trainerId);

    void deleteByTrainer(User trainer);
}
