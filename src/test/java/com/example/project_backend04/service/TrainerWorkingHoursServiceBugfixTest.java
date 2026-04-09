package com.example.project_backend04.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Bug Condition Exploration Test for Trainer Schedule Complete Slots Fix
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists.
 * DO NOT attempt to fix the test or the code when it fails.
 * 
 * Property 1: Bug Condition - Incomplete Slot Array for Partial Configuration
 * 
 * Goal: Surface counterexamples that demonstrate the bug exists (incomplete slot arrays)
 * Expected Outcome: Test FAILS (this is correct - it proves the bug exists)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bug Condition Exploration: Trainer Schedule Complete Slots")
class TrainerWorkingHoursServiceBugfixTest {

    @Mock
    private TrainerWorkingHoursRepository workingHoursRepo;

    @Mock
    private TrainerBookingRepository bookingRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private TrainerWorkingHoursService service;

    private User mockTrainer;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        mockTrainer = new User();
        mockTrainer.setId(1L);
        mockTrainer.setFullName("Test Trainer");
        mockTrainer.setAvatar("avatar.jpg");

        testDate = LocalDate.of(2026, 4, 10);
    }

    @Test
    @DisplayName("Bug Condition: Trainer with 6 configured slots returns only 6 slots instead of 14")
    void testPartialConfiguration_Returns14Slots() {
        // ARRANGE: Trainer has only 6 configured slots (9:00-15:00)
        List<TrainerWorkingHours> partialSlots = createPartialWorkingHours(mockTrainer, 
            DayOfWeek.THURSDAY, 9, 15); // 6 slots: 9-10, 10-11, 11-12, 12-13, 13-14, 14-15

        when(userRepo.findById(1L)).thenReturn(Optional.of(mockTrainer));
        when(workingHoursRepo.findSlotsByTrainerAndDay(1L, DayOfWeek.THURSDAY))
            .thenReturn(partialSlots);
        when(bookingRepo.findByTrainerIdAndDateRangeAndStatuses(
            eq(1L), eq(testDate), eq(testDate), any()))
            .thenReturn(new ArrayList<>());

        // ACT: Call getDailyAvailability
        TrainerScheduleResponse response = service.getDailyAvailability(1L, testDate);

        // ASSERT: Should return exactly 14 slots (8:00-22:00)
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getDailySlots(), "Daily slots should not be null");
        
        // CRITICAL ASSERTION: This will FAIL on unfixed code
        assertEquals(14, response.getDailySlots().size(), 
            "Bug Condition: Expected 14 slots (8:00-22:00) but got " + response.getDailySlots().size() + 
            ". This confirms the bug exists - backend only returns configured slots.");

        // Verify slot structure
        List<SlotInfo> slots = response.getDailySlots();
        assertEquals(LocalTime.of(8, 0), slots.get(0).getStartTime(), 
            "First slot should start at 08:00");
        assertEquals(LocalTime.of(22, 0), slots.get(13).getEndTime(), 
            "Last slot should end at 22:00");

        // Verify status distribution
        long availableCount = slots.stream()
            .filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();
        long inactiveCount = slots.stream()
            .filter(s -> s.getStatus() == SlotStatus.INACTIVE).count();

        assertEquals(6, availableCount, "Should have 6 AVAILABLE slots (9:00-15:00)");
        assertEquals(8, inactiveCount, "Should have 8 INACTIVE slots (unconfigured hours)");
    }

    @Test
    @DisplayName("Bug Condition: Trainer with 0 configured slots returns empty array instead of 14 INACTIVE slots")
    void testNoConfiguration_Returns14InactiveSlots() {
        // ARRANGE: Trainer has NO configured slots for this day
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockTrainer));
        when(workingHoursRepo.findSlotsByTrainerAndDay(1L, DayOfWeek.THURSDAY))
            .thenReturn(new ArrayList<>());
        when(bookingRepo.findByTrainerIdAndDateRangeAndStatuses(
            eq(1L), eq(testDate), eq(testDate), any()))
            .thenReturn(new ArrayList<>());

        // ACT
        TrainerScheduleResponse response = service.getDailyAvailability(1L, testDate);

        // ASSERT: Should return 14 INACTIVE slots
        assertNotNull(response.getDailySlots());
        
        // CRITICAL ASSERTION: This will FAIL on unfixed code
        assertEquals(14, response.getDailySlots().size(),
            "Bug Condition: Expected 14 INACTIVE slots but got " + response.getDailySlots().size() + 
            ". Trainer with no configuration should still return full 14-slot structure.");

        // All slots should be INACTIVE
        long inactiveCount = response.getDailySlots().stream()
            .filter(s -> s.getStatus() == SlotStatus.INACTIVE).count();
        assertEquals(14, inactiveCount, "All 14 slots should be INACTIVE when trainer has no configuration");
    }

    @Test
    @DisplayName("Bug Condition: Trainer with gap configuration returns incomplete array instead of 14 slots")
    void testGapConfiguration_Returns14SlotsWithProperStatus() {
        // ARRANGE: Trainer has morning (8:00-12:00) and evening (18:00-22:00) slots - 8 total
        List<TrainerWorkingHours> gapSlots = new ArrayList<>();
        gapSlots.addAll(createPartialWorkingHours(mockTrainer, DayOfWeek.THURSDAY, 8, 12));  // 4 slots
        gapSlots.addAll(createPartialWorkingHours(mockTrainer, DayOfWeek.THURSDAY, 18, 22)); // 4 slots

        when(userRepo.findById(1L)).thenReturn(Optional.of(mockTrainer));
        when(workingHoursRepo.findSlotsByTrainerAndDay(1L, DayOfWeek.THURSDAY))
            .thenReturn(gapSlots);
        when(bookingRepo.findByTrainerIdAndDateRangeAndStatuses(
            eq(1L), eq(testDate), eq(testDate), any()))
            .thenReturn(new ArrayList<>());

        // ACT
        TrainerScheduleResponse response = service.getDailyAvailability(1L, testDate);

        // ASSERT
        assertNotNull(response.getDailySlots());
        
        // CRITICAL ASSERTION: This will FAIL on unfixed code
        assertEquals(14, response.getDailySlots().size(),
            "Bug Condition: Expected 14 slots but got " + response.getDailySlots().size() + 
            ". Gap configuration should still return full 14-slot structure with proper status.");

        // Verify status distribution
        long availableCount = response.getDailySlots().stream()
            .filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();
        long inactiveCount = response.getDailySlots().stream()
            .filter(s -> s.getStatus() == SlotStatus.INACTIVE).count();

        assertEquals(8, availableCount, "Should have 8 AVAILABLE slots (morning + evening)");
        assertEquals(6, inactiveCount, "Should have 6 INACTIVE slots (gap hours 12:00-18:00)");
    }

    @Test
    @DisplayName("Bug Condition: Trainer with all 14 slots configured and some booked")
    void testFullConfiguration_Returns14SlotsWithBookedStatus() {
        // ARRANGE: Trainer has all 14 slots configured
        List<TrainerWorkingHours> fullSlots = createPartialWorkingHours(mockTrainer, 
            DayOfWeek.THURSDAY, 8, 22); // All 14 slots

        // Create 3 bookings
        List<TrainerBooking> bookings = new ArrayList<>();
        bookings.add(createBooking(testDate, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        bookings.add(createBooking(testDate, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        bookings.add(createBooking(testDate, LocalTime.of(14, 0), LocalTime.of(15, 0)));

        when(userRepo.findById(1L)).thenReturn(Optional.of(mockTrainer));
        when(workingHoursRepo.findSlotsByTrainerAndDay(1L, DayOfWeek.THURSDAY))
            .thenReturn(fullSlots);
        when(bookingRepo.findByTrainerIdAndDateRangeAndStatuses(
            eq(1L), eq(testDate), eq(testDate), any()))
            .thenReturn(bookings);

        // ACT
        TrainerScheduleResponse response = service.getDailyAvailability(1L, testDate);

        // ASSERT
        assertNotNull(response.getDailySlots());
        assertEquals(14, response.getDailySlots().size(), 
            "Should return exactly 14 slots even with full configuration");

        // Verify status distribution
        long bookedCount = response.getDailySlots().stream()
            .filter(s -> s.getStatus() == SlotStatus.BOOKED).count();
        long availableCount = response.getDailySlots().stream()
            .filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();

        assertEquals(3, bookedCount, "Should have 3 BOOKED slots");
        assertEquals(11, availableCount, "Should have 11 AVAILABLE slots");
    }

    // Helper methods

    private List<TrainerWorkingHours> createPartialWorkingHours(User trainer, DayOfWeek dow, 
                                                                  int startHour, int endHour) {
        List<TrainerWorkingHours> slots = new ArrayList<>();
        int slotIndex = 0;
        for (int hour = startHour; hour < endHour; hour++) {
            TrainerWorkingHours slot = TrainerWorkingHours.builder()
                .id((long) (hour * 10))
                .trainer(trainer)
                .dayOfWeek(dow)
                .startTime(LocalTime.of(hour, 0))
                .endTime(LocalTime.of(hour + 1, 0))
                .slotIndex(slotIndex++)
                .isDayOff(false)
                .isActive(true)
                .build();
            slots.add(slot);
        }
        return slots;
    }

    private TrainerBooking createBooking(LocalDate date, LocalTime start, LocalTime end) {
        return TrainerBooking.builder()
            .id(1L)
            .bookingId("TB" + System.currentTimeMillis())
            .trainer(mockTrainer)
            .bookingDate(date)
            .startTime(start)
            .endTime(end)
            .status(BookingStatus.CONFIRMED)
            .build();
    }
}
