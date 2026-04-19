package com.example.project_backend04.entity;

import com.example.project_backend04.enums.LeaveRequestStatus;
import com.example.project_backend04.enums.LeaveRequestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "trainer_leave_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerLeaveRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;
    
    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveRequestType leaveType; // FULL_DAY, TIME_SLOT
    
    @Column(name = "start_time")
    private LocalTime startTime; // For TIME_SLOT type
    
    @Column(name = "end_time")
    private LocalTime endTime; // For TIME_SLOT type
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveRequestStatus status; // PENDING, APPROVED, REJECTED
    
    @Column(name = "admin_notes", length = 500)
    private String adminNotes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy; // Admin who approved/rejected
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = LeaveRequestStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
