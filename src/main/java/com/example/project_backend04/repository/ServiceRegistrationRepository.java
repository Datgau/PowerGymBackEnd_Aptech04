package com.example.project_backend04.repository;

import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRegistrationRepository extends JpaRepository<ServiceRegistration, Long> {
    

    /**
     * Lấy registrations theo user với JOIN FETCH để tránh lazy loading
     */
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.gymService WHERE sr.user = :user ORDER BY sr.registrationDate DESC")
    List<ServiceRegistration> findByUserWithGymServiceOrderByRegistrationDateDesc(@Param("user") User user);
    
    /**
     * Lấy registrations theo service với JOIN FETCH để tránh lazy loading
     */
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.user WHERE sr.gymService = :gymService ORDER BY sr.registrationDate DESC")
    List<ServiceRegistration> findByGymServiceWithUserOrderByRegistrationDateDesc(@Param("gymService") GymService gymService);
    
    /**
     * Lấy registrations theo service với pagination và JOIN FETCH
     */
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.user WHERE sr.gymService = :gymService ORDER BY sr.registrationDate DESC")
    Page<ServiceRegistration> findByGymServiceWithUser(@Param("gymService") GymService gymService, Pageable pageable);
    
    /**
     * Lấy tất cả registrations với JOIN FETCH
     */
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.user JOIN FETCH sr.gymService ORDER BY sr.registrationDate DESC")
    List<ServiceRegistration> findAllWithUserAndGymService();
    
    /**
     * Lấy tất cả registrations với pagination và JOIN FETCH
     */
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.user JOIN FETCH sr.gymService ORDER BY sr.registrationDate DESC")
    Page<ServiceRegistration> findAllWithUserAndGymService(Pageable pageable);
    

    @Query("SELECT COUNT(sr) FROM ServiceRegistration sr WHERE sr.gymService = :service AND sr.status = 'ACTIVE'")
    Long countActiveRegistrations(@Param("service") GymService service);
    
    boolean existsByUserAndGymServiceAndStatus(User user, GymService gymService, RegistrationStatus status);
    
    // NEW METHODS for trainer integration
    
    /**
     * Find registrations by user that have a trainer assigned
     */
    List<ServiceRegistration> findByUserIdAndTrainerIsNotNull(Long userId);
    
    /**
     * Find registrations by trainer and status
     */
    List<ServiceRegistration> findByTrainerIdAndStatus(Long trainerId, RegistrationStatus status);
    
    /**
     * Find registrations by user and status with trainer and service info
     */
    @Query("SELECT sr FROM ServiceRegistration sr " +
           "LEFT JOIN FETCH sr.trainer t " +
           "JOIN FETCH sr.gymService gs " +
           "WHERE sr.user.id = :userId AND sr.status = :status")
    List<ServiceRegistration> findByUserIdAndStatusWithTrainerAndService(
        @Param("userId") Long userId, 
        @Param("status") RegistrationStatus status);
    
    /**
     * Find registration with its trainer bookings
     */
    @Query("SELECT sr FROM ServiceRegistration sr " +
           "LEFT JOIN FETCH sr.trainerBookings tb " +
           "WHERE sr.id = :registrationId AND (tb.status IN :statuses OR tb IS NULL)")
    Optional<ServiceRegistration> findByIdWithBookings(
        @Param("registrationId") Long registrationId,
        @Param("statuses") List<BookingStatus> statuses);
    
    /**
     * Count active registrations by trainer
     */
    @Query("SELECT COUNT(sr) FROM ServiceRegistration sr " +
           "WHERE sr.trainer.id = :trainerId AND sr.status = 'ACTIVE'")
    Long countActiveRegistrationsByTrainer(@Param("trainerId") Long trainerId);
    
    /**
     * Find registrations with trainer and service details
     */
    @Query("SELECT sr FROM ServiceRegistration sr " +
           "JOIN FETCH sr.user u " +
           "JOIN FETCH sr.gymService gs " +
           "LEFT JOIN FETCH sr.trainer t " +
           "WHERE sr.id = :registrationId")
    Optional<ServiceRegistration> findByIdWithFullDetails(@Param("registrationId") Long registrationId);
    
    /**
     * Find registrations by service category that have trainers
     */
    @Query("SELECT sr FROM ServiceRegistration sr " +
           "JOIN FETCH sr.trainer t " +
           "JOIN FETCH sr.gymService gs " +
           "WHERE gs.category.id = :categoryId AND sr.trainer IS NOT NULL " +
           "AND sr.status = 'ACTIVE'")
    List<ServiceRegistration> findByServiceCategoryWithTrainer(@Param("categoryId") Long categoryId);
    
    /**
     * Find registrations that need trainer assignment
     */
    @Query("SELECT sr FROM ServiceRegistration sr " +
           "JOIN FETCH sr.gymService gs " +
           "WHERE sr.trainer IS NULL AND sr.status = 'ACTIVE' " +
           "ORDER BY sr.registrationDate ASC")
    List<ServiceRegistration> findRegistrationsNeedingTrainer();
    
    /**
     * Find registrations by user and status
     */
    List<ServiceRegistration> findByUserAndStatusOrderByRegistrationDateDesc(User user, RegistrationStatus status);
}
