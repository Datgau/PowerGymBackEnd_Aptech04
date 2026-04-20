package com.example.project_backend04.service;

import com.example.project_backend04.entity.GymNotification;
import com.example.project_backend04.entity.Membership;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.repository.GymNotificationRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GymNotificationService {

    private final GymNotificationRepository notifRepo;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Create & push helpers ────────────────────────────────────────────────

    public void notifyServiceRegistered(ServiceRegistration reg) {
        String serviceName = reg.getGymService().getName();
        GymNotification n = save(reg.getUser(),
                "Service Registered",
                "You have successfully registered for: " + serviceName,
                "SERVICE_REGISTERED",
                reg.getId());
        push(reg.getUser().getId(), n);
    }

    public void notifyBookingConfirmed(TrainerBooking booking) {
        String trainerName = booking.getTrainer() != null ? booking.getTrainer().getFullName() : "Trainer";
        GymNotification n = save(booking.getUser(),
                "Booking Confirmed",
                "Your booking with " + trainerName + " on " + booking.getBookingDate() + " has been confirmed.",
                "BOOKING_CONFIRMED",
                booking.getId());
        push(booking.getUser().getId(), n);
    }

    public void notifyBookingRejected(TrainerBooking booking) {
        String trainerName = booking.getTrainer() != null ? booking.getTrainer().getFullName() : "Trainer";
        String reason = booking.getRejectionReason() != null ? " Reason: " + booking.getRejectionReason() : "";
        GymNotification n = save(booking.getUser(),
                "Booking Rejected",
                "Your booking with " + trainerName + " was rejected." + reason,
                "BOOKING_REJECTED",
                booking.getId());
        push(booking.getUser().getId(), n);
    }

    public void notifyMembershipActivated(Membership membership) {
        String pkgName = membership.getMembershipPackage().getName();
        GymNotification n = save(membership.getUser(),
                "Membership Activated",
                "Your membership package \"" + pkgName + "\" is now active until " + membership.getEndDate() + ".",
                "MEMBERSHIP_ACTIVATED",
                membership.getId());
        push(membership.getUser().getId(), n);
    }

    // ─── Trainer notifications ────────────────────────────────────────────────

    public void notifyTrainerNewBooking(TrainerBooking booking) {
        if (booking.getTrainer() == null) return;
        String memberName = booking.getUser() != null ? booking.getUser().getFullName() : "Member";
        String serviceName = booking.getServiceRegistration() != null && 
                             booking.getServiceRegistration().getGymService() != null
                ? booking.getServiceRegistration().getGymService().getName()
                : "";
        GymNotification n = save(booking.getTrainer(),
                "New Booking Request",
                memberName + " has requested a booking" + 
                (serviceName.isEmpty() ? "" : " for " + serviceName) +
                " on " + booking.getBookingDate(),
                "NEW_BOOKING_REQUEST",
                booking.getId());
        push(booking.getTrainer().getId(), n);
    }

    // ─── REST operations ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GymNotification> getMyNotifications() {
        User user = currentUser();
        return notifRepo.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notifRepo.countByUserAndIsReadFalse(currentUser());
    }

    @Transactional
    public void markAsRead(Long id) {
        notifRepo.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notifRepo.save(n);
        });
    }

    @Transactional
    public void markAllRead() {
        notifRepo.markAllReadByUser(currentUser());
    }

    @Transactional
    public void delete(Long id) {
        notifRepo.deleteById(id);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private GymNotification save(User user, String title, String content, String type, Long relatedId) {
        GymNotification n = GymNotification.builder()
                .user(user)
                .title(title)
                .content(content)
                .type(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        return notifRepo.save(n);
    }

    private void push(Long userId, GymNotification n) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", n.getId());
            payload.put("title", n.getTitle());
            payload.put("content", n.getContent());
            payload.put("type", n.getType());
            payload.put("relatedId", n.getRelatedId());
            payload.put("isRead", false);
            payload.put("createdAt", n.getCreatedAt().toString());
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", payload);
            log.info("[WS] Pushed notification to user {}: {}", userId, n.getType());
        } catch (Exception e) {
            log.warn("[WS] Failed to push notification to user {}: {}", userId, e.getMessage());
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        return userRepository.findById(details.getId()).orElseThrow();
    }
}
