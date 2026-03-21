package com.example.project_backend04.controller;

import com.example.project_backend04.dto.request.TrainerBooking.CreateTrainerBookingRequest;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerAvailabilityResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.service.TrainerBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainer-bookings")
@RequiredArgsConstructor
public class TrainerBookingController {
    
    private final TrainerBookingService trainerBookingService;
    
    @PostMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> createBooking(
            @PathVariable Long userId,
            @Valid @RequestBody CreateTrainerBookingRequest request) {
        try {
            TrainerBookingResponse booking = trainerBookingService.createBooking(userId, request);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Booking created successfully",
                    booking,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getUserBookings(
            @PathVariable Long userId) {
        try {
            List<TrainerBookingResponse> bookings = trainerBookingService.getUserBookings(userId);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "User bookings retrieved successfully",
                    bookings,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
    
    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getUpcomingUserBookings(
            @PathVariable Long userId) {
        try {
            List<TrainerBookingResponse> bookings = trainerBookingService.getUpcomingUserBookings(userId);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Upcoming user bookings retrieved successfully",
                    bookings,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
    
    @GetMapping("/trainer/{trainerId}")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getTrainerBookings(
            @PathVariable Long trainerId) {
        try {
            List<TrainerBookingResponse> bookings = trainerBookingService.getTrainerBookings(trainerId);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Trainer bookings retrieved successfully",
                    bookings,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
    
    @GetMapping("/trainer/{trainerId}/upcoming")
    public ResponseEntity<ApiResponse<List<TrainerBookingResponse>>> getUpcomingTrainerBookings(
            @PathVariable Long trainerId) {
        try {
            List<TrainerBookingResponse> bookings = trainerBookingService.getUpcomingTrainerBookings(trainerId);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Upcoming trainer bookings retrieved successfully",
                    bookings,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
    
    @GetMapping("/trainer/{trainerId}/availability")
    public ResponseEntity<ApiResponse<TrainerAvailabilityResponse>> getTrainerAvailability(
            @PathVariable Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            TrainerAvailabilityResponse availability = trainerBookingService.getTrainerAvailability(trainerId, date);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Trainer availability retrieved successfully",
                    availability,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
    
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<TrainerBookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam Long userId,
            @RequestParam(required = false) String reason) {
        try {
            TrainerBookingResponse booking = trainerBookingService.cancelBooking(bookingId, userId, reason);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Booking cancelled successfully",
                    booking,
                    HttpStatus.OK.value()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
}