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

    /** Lấy tất cả slot đang active của trainer (để hiển thị lịch trên frontend). */
    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId AND twh.isActive = true "
         + "ORDER BY twh.dayOfWeek, twh.slotIndex")
    List<TrainerWorkingHours> findActiveSlotsByTrainer(@Param("trainerId") Long trainerId);

    /** Lấy toàn bộ slot của trainer (kể cả inactive, dùng cho admin quản lý). */
    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "ORDER BY twh.dayOfWeek, twh.slotIndex")
    List<TrainerWorkingHours> findAllSlotsByTrainer(@Param("trainerId") Long trainerId);

    /** Lấy tất cả slot của trainer theo ngày trong tuần. */
    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.dayOfWeek = :dayOfWeek "
         + "AND twh.isActive = true "
         + "ORDER BY twh.slotIndex")
    List<TrainerWorkingHours> findSlotsByTrainerAndDay(
        @Param("trainerId")  Long trainerId,
        @Param("dayOfWeek")  DayOfWeek dayOfWeek);


    /**
     * Kiểm tra xem trainer có slot active cover khoảng giờ [startTime, endTime]
     * vào ngày dayOfWeek không.
     * Dùng khi validate booking: đảm bảo khách chỉ đặt trong giờ trainer làm việc.
     */
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

    /**
     * Kiểm tra trainer có ngày nghỉ vào dayOfWeek không.
     */
    @Query("SELECT COUNT(twh) > 0 FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.dayOfWeek = :dayOfWeek "
         + "AND twh.isDayOff = true "
         + "AND twh.isActive = true")
    boolean isTrainerDayOff(
        @Param("trainerId") Long trainerId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek);

    // ── Tìm slot cụ thể ───────────────────────────────────────────────────────

    /** Tìm slot cụ thể của trainer theo ngày và slotIndex (để tránh duplicate). */
    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.dayOfWeek = :dayOfWeek "
         + "AND twh.slotIndex = :slotIndex")
    Optional<TrainerWorkingHours> findByTrainerAndDayAndSlot(
        @Param("trainerId")  Long trainerId,
        @Param("dayOfWeek")  DayOfWeek dayOfWeek,
        @Param("slotIndex")  Integer slotIndex);

    // ── Truy vấn theo danh sách trainer ───────────────────────────────────────

    /**
     * Lấy lịch làm việc của nhiều trainer cùng lúc.
     * Dùng khi hiển thị trang "Chọn trainer" cho user.
     */
    @Query("SELECT twh FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id IN :trainerIds "
         + "AND twh.isActive = true "
         + "AND twh.isDayOff = false "
         + "ORDER BY twh.trainer.id, twh.dayOfWeek, twh.slotIndex")
    List<TrainerWorkingHours> findActiveSlotsByTrainers(
        @Param("trainerIds") List<Long> trainerIds);

    /** Số slot active của trainer (dùng cho dashboard). */
    @Query("SELECT COUNT(twh) FROM TrainerWorkingHours twh "
         + "WHERE twh.trainer.id = :trainerId "
         + "AND twh.isActive = true "
         + "AND twh.isDayOff = false")
    Long countActiveSlotsByTrainer(@Param("trainerId") Long trainerId);

    /** Xóa tất cả slot của trainer (dùng khi reset lịch). */
    void deleteByTrainer(User trainer);
}
