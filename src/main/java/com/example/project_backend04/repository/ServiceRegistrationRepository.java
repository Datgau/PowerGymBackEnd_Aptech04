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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRegistrationRepository extends JpaRepository<ServiceRegistration, Long>, JpaSpecificationExecutor<ServiceRegistration> {
    

    @Query("SELECT sr FROM ServiceRegistration sr " +
           "JOIN FETCH sr.gymService gs " +
           "JOIN FETCH gs.category " +
           "LEFT JOIN FETCH sr.trainer " +
           "WHERE sr.user = :user " +
           "ORDER BY sr.registrationDate DESC")
    List<ServiceRegistration> findByUserWithGymServiceOrderByRegistrationDateDesc(@Param("user") User user);
  
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.user WHERE sr.gymService = :gymService ORDER BY sr.registrationDate DESC")
    List<ServiceRegistration> findByGymServiceWithUserOrderByRegistrationDateDesc(@Param("gymService") GymService gymService);
  
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.user WHERE sr.gymService = :gymService ORDER BY sr.registrationDate DESC")
    Page<ServiceRegistration> findByGymServiceWithUser(@Param("gymService") GymService gymService, Pageable pageable);
   
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.user JOIN FETCH sr.gymService LEFT JOIN FETCH sr.trainer ORDER BY sr.registrationDate DESC")
    List<ServiceRegistration> findAllWithUserAndGymService();
 
    @Query("SELECT sr FROM ServiceRegistration sr JOIN FETCH sr.user JOIN FETCH sr.gymService LEFT JOIN FETCH sr.trainer ORDER BY sr.registrationDate DESC")
    Page<ServiceRegistration> findAllWithUserAndGymService(Pageable pageable);
    

    @Query("SELECT COUNT(sr) FROM ServiceRegistration sr WHERE sr.gymService = :service AND sr.status = 'ACTIVE'")
    Long countActiveRegistrations(@Param("service") GymService service);
    
    boolean existsByUserAndGymServiceAndStatus(User user, GymService gymService, RegistrationStatus status);
    
   
    Optional<ServiceRegistration> findByUserAndGymServiceAndStatus(User user, GymService gymService, RegistrationStatus status);

    Optional<ServiceRegistration> findTopByUserAndGymServiceAndStatusOrderByRegistrationDateDesc(User user, GymService gymService, RegistrationStatus status);

    List<ServiceRegistration> findByUserIdAndTrainerIsNotNull(Long userId);
    
 
    List<ServiceRegistration> findByTrainerIdAndStatus(Long trainerId, RegistrationStatus status);

    @Query("SELECT sr FROM ServiceRegistration sr " +
           "LEFT JOIN FETCH sr.trainer t " +
           "JOIN FETCH sr.gymService gs " +
           "WHERE sr.user.id = :userId AND sr.status = :status")
    List<ServiceRegistration> findByUserIdAndStatusWithTrainerAndService(
        @Param("userId") Long userId, 
        @Param("status") RegistrationStatus status);

    @Query("SELECT sr FROM ServiceRegistration sr " +
           "LEFT JOIN FETCH sr.trainerBookings tb " +
           "WHERE sr.id = :registrationId AND (tb.status IN :statuses OR tb IS NULL)")
    Optional<ServiceRegistration> findByIdWithBookings(
        @Param("registrationId") Long registrationId,
        @Param("statuses") List<BookingStatus> statuses);

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
     * Find user registrations with gym service, trainer AND bookings eagerly loaded.
     * Used by mobile API to avoid LazyInitializationException.
     */
    @Query("SELECT DISTINCT sr FROM ServiceRegistration sr " +
           "JOIN FETCH sr.gymService gs " +
           "LEFT JOIN FETCH sr.trainer t " +
           "LEFT JOIN FETCH sr.trainerBookings tb " +
           "WHERE sr.user.id = :userId AND sr.status = :status")
    List<ServiceRegistration> findByUserIdAndStatusWithBookings(
        @Param("userId") Long userId,
        @Param("status") RegistrationStatus status);
    
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
    
    /**
     * Find all with filters using EntityGraph to fetch trainer
     */
    @Query("SELECT DISTINCT sr FROM ServiceRegistration sr " +
           "JOIN FETCH sr.user u " +
           "JOIN FETCH sr.gymService gs " +
           "LEFT JOIN FETCH sr.trainer t")
    Page<ServiceRegistration> findAllWithFullDetails(Pageable pageable);

    /**
     * Find expired counter registrations that haven't been paid
     * Used by scheduler to auto-cancel unpaid counter registrations after 3 days
     */
    @Query("SELECT sr FROM ServiceRegistration sr " +
           "WHERE sr.registrationType = :registrationType " +
           "AND sr.status = :status " +
           "AND sr.registrationDate < :expiryThreshold " +
           "AND NOT EXISTS (" +
           "  SELECT po FROM PaymentOrder po " +
           "  WHERE po.user = sr.user " +
           "  AND po.itemType = 'SERVICE' " +
           "  AND po.itemId = CAST(sr.gymService.id AS string) " +
           "  AND po.status = 'SUCCESS'" +
           ")")
    List<ServiceRegistration> findExpiredCounterRegistrations(
        @Param("expiryThreshold") java.time.LocalDateTime expiryThreshold,
        @Param("registrationType") com.example.project_backend04.enums.RegistrationType registrationType,
        @Param("status") RegistrationStatus status
    );

    /**
     * Find active registrations by trainer ID
     * Used for trainer salary calculation
     * Filters by trainerId, status = 'ACTIVE', and non-expired registrations
     * Excludes registrations that have REJECTED bookings
     */
    @Query("SELECT sr FROM ServiceRegistration sr " +
           "WHERE sr.trainer.id = :trainerId " +
           "AND sr.status = 'ACTIVE' " +
           "AND (sr.expirationDate IS NULL OR sr.expirationDate > CURRENT_TIMESTAMP) " +
           "AND NOT EXISTS (SELECT 1 FROM TrainerBooking tb WHERE tb.serviceRegistration.id = sr.id AND tb.status = 'REJECTED')")
    List<ServiceRegistration> findActiveRegistrationsByTrainerId(
        @Param("trainerId") Long trainerId
    );

    /**
     * Find active registrations by trainer and service
     * Used for trainer salary calculation per service
     * Includes LEFT JOIN FETCH for paymentOrder and gymService to avoid N+1 queries
     * Filters by trainerId, serviceId, status = 'ACTIVE', and non-expired registrations
     * Excludes registrations that have REJECTED bookings (trainer rejected the booking)
     */
    @Query("SELECT sr FROM ServiceRegistration sr " +
           "LEFT JOIN FETCH sr.paymentOrder " +
           "LEFT JOIN FETCH sr.gymService " +
           "WHERE sr.trainer.id = :trainerId " +
           "AND sr.gymService.id = :serviceId " +
           "AND sr.status = 'ACTIVE' " +
           "AND (sr.expirationDate IS NULL OR sr.expirationDate > CURRENT_TIMESTAMP) " +
           "AND NOT EXISTS (SELECT 1 FROM TrainerBooking tb WHERE tb.serviceRegistration.id = sr.id AND tb.status = 'REJECTED')")
    List<ServiceRegistration> findActiveRegistrationsByTrainerAndService(
        @Param("trainerId") Long trainerId,
        @Param("serviceId") Long serviceId
    );

    /**
     * Find distinct gym services by trainer ID
     * Used for trainer salary calculation to get all services a trainer teaches
     * Filters by trainerId, status = 'ACTIVE', and non-expired registrations
     * Excludes services where all registrations have REJECTED bookings
     */
    @Query("SELECT DISTINCT sr.gymService FROM ServiceRegistration sr " +
           "WHERE sr.trainer.id = :trainerId " +
           "AND sr.status = 'ACTIVE' " +
           "AND (sr.expirationDate IS NULL OR sr.expirationDate > CURRENT_TIMESTAMP) " +
           "AND NOT EXISTS (SELECT 1 FROM TrainerBooking tb WHERE tb.serviceRegistration.id = sr.id AND tb.status = 'REJECTED')")
    List<GymService> findDistinctServicesByTrainerId(
        @Param("trainerId") Long trainerId
    );
}
