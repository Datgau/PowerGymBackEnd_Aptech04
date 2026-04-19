package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.TrainerWorkingHours.SaveWorkingHoursRequest;
import com.example.project_backend04.dto.response.TrainerWorkingHours.TrainerScheduleResponse;
import com.example.project_backend04.dto.response.TrainerWorkingHours.TrainerScheduleResponse.SlotInfo;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.TrainerWorkingHours;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.enums.SlotStatus;
import com.example.project_backend04.repository.TrainerBookingRepository;
import com.example.project_backend04.repository.TrainerWorkingHoursRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.service.IService.ITrainerWorkingHoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrainerWorkingHoursService implements ITrainerWorkingHoursService {

    private final TrainerWorkingHoursRepository workingHoursRepo;
    private final TrainerBookingRepository bookingRepo;
    private final UserRepository userRepo;

    @Override
    public TrainerScheduleResponse saveWeeklySchedule(Long trainerId,
                                                       SaveWorkingHoursRequest req) {
        User trainer = loadTrainer(trainerId);

        // Deactivate tất cả slot cũ
        List<TrainerWorkingHours> existing = workingHoursRepo.findAllSlotsByTrainer(trainerId);
        existing.forEach(s -> s.setIsActive(false));
        workingHoursRepo.saveAll(existing);

        // Upsert các slot mới
        AtomicInteger slotIdx = new AtomicInteger(0);
        List<TrainerWorkingHours> newSlots = req.getSlots().stream().map(s -> {
            // Tìm slot cũ cùng ngày + index để upsert
            return workingHoursRepo
                    .findByTrainerAndDayAndSlot(trainerId, s.getDayOfWeek(), slotIdx.getAndIncrement())
                    .map(existing2 -> {
                        existing2.setStartTime(s.getStartTime());
                        existing2.setEndTime(s.getEndTime());
                        existing2.setIsDayOff(Boolean.TRUE.equals(s.getIsDayOff()));
                        existing2.setNote(s.getNote());
                        existing2.setIsActive(true);
                        return existing2;
                    })
                    .orElseGet(() -> TrainerWorkingHours.builder()
                            .trainer(trainer)
                            .dayOfWeek(s.getDayOfWeek())
                            .startTime(Boolean.TRUE.equals(s.getIsDayOff()) ? null : s.getStartTime())
                            .endTime(Boolean.TRUE.equals(s.getIsDayOff()) ? null : s.getEndTime())
                            .slotIndex(slotIdx.get() - 1)
                            .isDayOff(Boolean.TRUE.equals(s.getIsDayOff()))
                            .isActive(true)
                            .note(s.getNote())
                            .build());
        }).collect(Collectors.toList());

        workingHoursRepo.saveAll(newSlots);
        log.info("Saved {} working hour slots for trainer {}", newSlots.size(), trainerId);
        return buildWeeklyResponse(trainer, workingHoursRepo.findActiveSlotsByTrainer(trainerId));
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerScheduleResponse getWeeklySchedule(Long trainerId) {
        User trainer = loadTrainer(trainerId);
        List<TrainerWorkingHours> slots = workingHoursRepo.findActiveSlotsByTrainer(trainerId);
        return buildWeeklyResponse(trainer, slots);
    }

    @Override
    public void markDayOff(Long trainerId, DayOfWeek dayOfWeek, boolean isDayOff) {
        List<TrainerWorkingHours> daySlots = workingHoursRepo
                .findSlotsByTrainerAndDay(trainerId, dayOfWeek);
        daySlots.forEach(s -> s.setIsDayOff(isDayOff));
        workingHoursRepo.saveAll(daySlots);
        log.info("Trainer {} — {} isDayOff={}", trainerId, dayOfWeek, isDayOff);
    }

    @Override
    public void toggleSlot(Long slotId, boolean isActive) {
        TrainerWorkingHours slot = workingHoursRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy slot: " + slotId));
        slot.setIsActive(isActive);
        workingHoursRepo.save(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerScheduleResponse getDailyAvailability(Long trainerId, LocalDate date) {
        User trainer = loadTrainer(trainerId);
        DayOfWeek dow = date.getDayOfWeek();

        // Load working hours cho ngày đó
        List<TrainerWorkingHours> workSlots =
                workingHoursRepo.findSlotsByTrainerAndDay(trainerId, dow);
        List<TrainerBooking> dayBookings = bookingRepo.findByTrainerIdAndDateRangeAndStatuses(
                trainerId, date, date,
                List.of(BookingStatus.PENDING,
                        BookingStatus.CONFIRMED));

        List<SlotInfo> slots = workSlots.stream().map(ws -> {
            if (Boolean.TRUE.equals(ws.getIsDayOff())) {
                return SlotInfo.builder()
                        .slotId(ws.getId())
                        .dayOfWeek(ws.getDayOfWeek())
                        .startTime(ws.getStartTime())
                        .endTime(ws.getEndTime())
                        .status(SlotStatus.DAY_OFF)
                        .isDayOff(true)
                        .note(ws.getNote())
                        .build();
            }

            TrainerBooking overlap = dayBookings.stream()
                    .filter(b -> ws.getStartTime() != null
                            && b.getStartTime().isBefore(ws.getEndTime())
                            && b.getEndTime().isAfter(ws.getStartTime()))
                    .findFirst().orElse(null);

            SlotStatus status = overlap != null ? SlotStatus.BOOKED : SlotStatus.AVAILABLE;
            return SlotInfo.builder()
                    .slotId(ws.getId())
                    .dayOfWeek(ws.getDayOfWeek())
                    .startTime(ws.getStartTime())
                    .endTime(ws.getEndTime())
                    .status(status)
                    .isDayOff(false)
                    .note(ws.getNote())
                    .bookingId(overlap != null ? overlap.getId() : null)
                    .build();
        }).collect(Collectors.toList());

        return TrainerScheduleResponse.builder()
                .trainerId(trainerId)
                .trainerName(trainer.getFullName())
                .trainerAvatar(trainer.getAvatar())
                .date(date)
                .dailySlots(slots)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerScheduleResponse> getDailyAvailabilityForTrainers(
            List<Long> trainerIds, LocalDate date) {
        return trainerIds.stream()
                .map(id -> getDailyAvailability(id, date))
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isWithinWorkingHours(Long trainerId, LocalDate date,
                                         LocalTime startTime, LocalTime endTime) {
        // No working hours configured at all → allow any time
        if (workingHoursRepo.countActiveSlotsByTrainer(trainerId) == 0) {
            return true;
        }

        DayOfWeek dow = date.getDayOfWeek();

        // Explicitly marked as day off → deny
        if (workingHoursRepo.isTrainerDayOff(trainerId, dow)) {
            return false;
        }

        // No slots configured for this specific day → allow (trainer hasn't restricted this day)
        List<TrainerWorkingHours> daySlots = workingHoursRepo.findSlotsByTrainerAndDay(trainerId, dow);
        if (daySlots.isEmpty()) {
            return true;
        }

        // Slots exist for this day → must find a covering slot
        List<TrainerWorkingHours> covering = workingHoursRepo
                .findCoveringSlots(trainerId, dow, startTime, endTime);
        return !covering.isEmpty();
    }


    @Override
    public void requestDayOff(Long trainerId, com.example.project_backend04.dto.request.TrainerWorkingHours.DayOffRequest request) {
        loadTrainer(trainerId); // validate trainer exists
        DayOfWeek dow = request.getDate().getDayOfWeek();

        if (Boolean.TRUE.equals(request.getAllDay())) {
            // Đánh dấu toàn bộ slot của ngày đó là isDayOff
            List<TrainerWorkingHours> daySlots = workingHoursRepo.findSlotsByTrainerAndDay(trainerId, dow);
            if (daySlots.isEmpty()) {
                log.warn("Trainer {} không có slot nào cho ngày {} ({})", trainerId, request.getDate(), dow);
            }
            String note = request.getReason() != null ? request.getReason() : "Xin nghỉ ngày " + request.getDate();
            daySlots.forEach(s -> {
                s.setIsDayOff(true);
                s.setNote(note);
            });
            workingHoursRepo.saveAll(daySlots);
            log.info("Trainer {} xin nghỉ cả ngày {}", trainerId, request.getDate());
        } else {
            // Tắt các slot được chọn
            if (request.getSlotIds() == null || request.getSlotIds().isEmpty()) {
                throw new IllegalArgumentException("Phải chọn ít nhất một khung giờ khi không nghỉ cả ngày");
            }
            List<TrainerWorkingHours> slots = workingHoursRepo.findAllById(request.getSlotIds());
            // Validate tất cả slot thuộc trainer này và đúng ngày
            slots.forEach(s -> {
                if (!s.getTrainer().getId().equals(trainerId)) {
                    throw new SecurityException("Slot " + s.getId() + " không thuộc trainer " + trainerId);
                }
                if (s.getDayOfWeek() != dow) {
                    throw new IllegalArgumentException("Slot " + s.getId() + " không thuộc ngày " + request.getDate());
                }
                s.setIsActive(false);
                if (request.getReason() != null) s.setNote(request.getReason());
            });
            workingHoursRepo.saveAll(slots);
            log.info("Trainer {} tắt {} slot ngày {}", trainerId, slots.size(), request.getDate());
        }
    }

    private User loadTrainer(Long trainerId) {
        return userRepo.findById(trainerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy trainer: " + trainerId));
    }

    private TrainerScheduleResponse buildWeeklyResponse(User trainer,
                                                          List<TrainerWorkingHours> slots) {
        Map<DayOfWeek, List<SlotInfo>> weeklyMap = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dow : DayOfWeek.values()) {
            weeklyMap.put(dow, new ArrayList<>());
        }

        slots.forEach(ws -> {
            SlotStatus status = Boolean.TRUE.equals(ws.getIsDayOff())
                    ? SlotStatus.DAY_OFF
                    : (Boolean.TRUE.equals(ws.getIsActive()) ? SlotStatus.AVAILABLE : SlotStatus.INACTIVE);

            weeklyMap.get(ws.getDayOfWeek()).add(SlotInfo.builder()
                    .slotId(ws.getId())
                    .dayOfWeek(ws.getDayOfWeek())
                    .startTime(ws.getStartTime())
                    .endTime(ws.getEndTime())
                    .status(status)
                    .isDayOff(Boolean.TRUE.equals(ws.getIsDayOff()))
                    .note(ws.getNote())
                    .build());
        });

        return TrainerScheduleResponse.builder()
                .trainerId(trainer.getId())
                .trainerName(trainer.getFullName())
                .trainerAvatar(trainer.getAvatar())
                .weeklySchedule(weeklyMap)
                .build();
    }
}
