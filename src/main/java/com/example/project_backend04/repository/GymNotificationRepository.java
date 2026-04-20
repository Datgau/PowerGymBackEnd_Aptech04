package com.example.project_backend04.repository;

import com.example.project_backend04.entity.GymNotification;
import com.example.project_backend04.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymNotificationRepository extends JpaRepository<GymNotification, Long> {

    List<GymNotification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Query("UPDATE GymNotification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllReadByUser(User user);
}
