package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.response.ServiceCategory.ServiceCategoryResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerResponse;
import com.example.project_backend04.dto.response.Trainer.TrainerSpecialtyResponse;
import com.example.project_backend04.dto.response.TrainerBooking.TrainerBookingResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class TrainerMapper {

    private final UserMapper userMapper;

    public TrainerResponse mapToTrainerResponse(User trainer) {
        TrainerResponse response = new TrainerResponse();
        response.setId(trainer.getId());
        response.setFullName(trainer.getFullName());
        response.setEmail(trainer.getEmail());
        response.setPhoneNumber(trainer.getPhoneNumber());
        response.setAvatar(trainer.getAvatar());
        response.setBio(trainer.getBio());
        response.setTotalExperienceYears(trainer.getTotalExperienceYears());
        response.setEducation(trainer.getEducation());
        response.setIsActive(trainer.getIsActive());


        // Map specialties
        if (trainer.getTrainerSpecialties() != null) {
            List<TrainerSpecialtyResponse> specialties = trainer.getTrainerSpecialties().stream()
                    .filter(ts -> ts.getIsActive())
                    .map(ts -> {
                        TrainerSpecialtyResponse specialty = new TrainerSpecialtyResponse();
                        specialty.setId(ts.getSpecialty().getId());
                        ServiceCategoryResponse categoryResponse = new ServiceCategoryResponse();
                        categoryResponse.setId(ts.getSpecialty().getId());
                        categoryResponse.setName(ts.getSpecialty().getName());
                        specialty.setSpecialty(categoryResponse);
                        specialty.setDescription(ts.getDescription());
                        specialty.setExperienceYears(ts.getExperienceYears());
                        specialty.setCertifications(ts.getCertifications());
                        specialty.setLevel(ts.getLevel());
                        specialty.setIsActive(ts.getIsActive());
                        specialty.setCreatedAt(ts.getCreatedAt());
                        return specialty;
                    })
                    .collect(Collectors.toList());

            response.setSpecialties(specialties);
        }

        return response;
    }
    
    public TrainerBookingResponse toTrainerBookingResponse(TrainerBooking booking) {
        if (booking == null) {
            return null;
        }
        
        UserResponse userResponse = userMapper.toResponse(booking.getUser());
        UserResponse trainerResponse = userMapper.toResponse(booking.getTrainer());
        
        return TrainerBookingResponse.builder()
            .id(booking.getId())
            .bookingId(booking.getBookingId())
            .user(userResponse)
            .trainer(trainerResponse)
            .bookingDate(booking.getBookingDate())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .notes(booking.getNotes())
            .sessionType(booking.getSessionType())
            .status(booking.getStatus())
            .createdAt(booking.getCreatedAt())
            .updatedAt(booking.getUpdatedAt())
            .cancelledAt(booking.getCancelledAt())
            .cancellationReason(booking.getCancellationReason())
            .serviceRegistrationId(booking.getServiceRegistration() != null ? 
                booking.getServiceRegistration().getId() : null)
            .serviceName(booking.getServiceRegistration() != null ? 
                booking.getServiceRegistration().getGymService().getName() : null)
            .sessionObjective(booking.getSessionObjective())
            .sessionNumber(booking.getSessionNumber())
            .trainerNotes(booking.getTrainerNotes())
            .clientFeedback(booking.getClientFeedback())
            .rating(booking.getRating())
            .build();
    }
}
