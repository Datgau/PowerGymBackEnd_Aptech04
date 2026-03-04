package com.example.project_backend04.repository;

import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.User;
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
    
    boolean existsByUserAndGymServiceAndStatus(User user, GymService gymService, ServiceRegistration.RegistrationStatus status);
}
