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
    
    List<ServiceRegistration> findByUserOrderByRegistrationDateDesc(User user);
    
    List<ServiceRegistration> findByGymServiceOrderByRegistrationDateDesc(GymService gymService);
    
    /**
     * Lấy registrations theo service với pagination
     */
    Page<ServiceRegistration> findByGymService(GymService gymService, Pageable pageable);
    
    List<ServiceRegistration> findByUserAndStatus(User user, ServiceRegistration.RegistrationStatus status);
    
    List<ServiceRegistration> findByGymServiceAndStatus(GymService gymService, ServiceRegistration.RegistrationStatus status);
    
    @Query("SELECT sr FROM ServiceRegistration sr WHERE sr.user = :user AND sr.gymService = :service AND sr.status = 'ACTIVE'")
    Optional<ServiceRegistration> findActiveRegistration(@Param("user") User user, @Param("service") GymService service);
    
    @Query("SELECT COUNT(sr) FROM ServiceRegistration sr WHERE sr.gymService = :service AND sr.status = 'ACTIVE'")
    Long countActiveRegistrations(@Param("service") GymService service);
    
    boolean existsByUserAndGymServiceAndStatus(User user, GymService gymService, ServiceRegistration.RegistrationStatus status);
}
