package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Service.ServiceRegistrationFilterRequest;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.enums.*;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRegistrationServiceFilterTest {

    @Mock
    private ServiceRegistrationRepository registrationRepository;

    @Mock
    private GymServiceRepository gymServiceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private GymServiceService gymServiceService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TrainerBookingRepository trainerBookingRepository;

    @Mock
    private EnhancedServiceRegistrationService enhancedServiceRegistrationService;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private TrainerSpecialtyRepository trainerSpecialtyRepository;

    @InjectMocks
    private ServiceRegistrationService serviceRegistrationService;

    private User mockUser;
    private GymService mockService;
    private ServiceRegistration mockRegistration;
    private PaymentOrder mockPaymentOrder;

    @BeforeEach
    void setUp() {
        // Setup mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFullName("John Doe");
        mockUser.setEmail("john@example.com");

        // Setup mock service
        mockService = new GymService();
        mockService.setId(1L);
        mockService.setName("Personal Training");
        mockService.setPrice(BigDecimal.valueOf(500000));
        mockService.setDuration(30);

        // Setup mock registration
        mockRegistration = new ServiceRegistration();
        mockRegistration.setId(1L);
        mockRegistration.setUser(mockUser);
        mockRegistration.setGymService(mockService);
        mockRegistration.setStatus(RegistrationStatus.ACTIVE);
        mockRegistration.setRegistrationType(RegistrationType.ONLINE);
        mockRegistration.setRegistrationDate(LocalDateTime.now());
        mockRegistration.setExpirationDate(LocalDateTime.now().plusDays(30));

        // Setup mock payment order - itemType="SERVICE" and itemId=serviceId
        mockPaymentOrder = new PaymentOrder();
        mockPaymentOrder.setId("payment-1");
        mockPaymentOrder.setStatus(com.example.project_backend04.enums.PaymentStatus.SUCCESS);
        mockPaymentOrder.setItemType("SERVICE");
        mockPaymentOrder.setItemId("1");
        mockPaymentOrder.setUser(mockUser);
        mockPaymentOrder.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getAllRegistrationsWithFilters_NoFilters_ReturnsAllRegistrations() {
        // Arrange
        ServiceRegistrationFilterRequest request = new ServiceRegistrationFilterRequest();
        request.setPage(0);
        request.setSize(10);

        List<ServiceRegistration> registrations = Arrays.asList(mockRegistration);
        Page<ServiceRegistration> page = new PageImpl<>(registrations, PageRequest.of(0, 10), 1);

        when(registrationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(paymentOrderRepository.findByUserAndItemTypeAndItemIdOrderByCreatedAtDesc(any(User.class), anyString(), anyString())).thenReturn(Arrays.asList(mockPaymentOrder));
        when(gymServiceService.getServiceById(anyLong())).thenReturn(null);
        when(userMapper.toResponse(any(User.class))).thenReturn(null);

        // Act
        Page<ServiceRegistrationResponse> result = serviceRegistrationService.getAllRegistrationsWithFilters(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(registrationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllRegistrationsWithFilters_WithStatusFilter_ReturnsFilteredRegistrations() {
        // Arrange
        ServiceRegistrationFilterRequest request = new ServiceRegistrationFilterRequest();
        request.setStatus(RegistrationStatus.ACTIVE);
        request.setPage(0);
        request.setSize(10);

        List<ServiceRegistration> registrations = Arrays.asList(mockRegistration);
        Page<ServiceRegistration> page = new PageImpl<>(registrations, PageRequest.of(0, 10), 1);

        when(registrationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(paymentOrderRepository.findByUserAndItemTypeAndItemIdOrderByCreatedAtDesc(any(User.class), anyString(), anyString())).thenReturn(Arrays.asList(mockPaymentOrder));
        when(gymServiceService.getServiceById(anyLong())).thenReturn(null);
        when(userMapper.toResponse(any(User.class))).thenReturn(null);

        // Act
        Page<ServiceRegistrationResponse> result = serviceRegistrationService.getAllRegistrationsWithFilters(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(registrationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllRegistrationsWithFilters_WithRegistrationTypeFilter_ReturnsFilteredRegistrations() {
        // Arrange
        ServiceRegistrationFilterRequest request = new ServiceRegistrationFilterRequest();
        request.setRegistrationType(RegistrationType.ONLINE);
        request.setPage(0);
        request.setSize(10);

        List<ServiceRegistration> registrations = Arrays.asList(mockRegistration);
        Page<ServiceRegistration> page = new PageImpl<>(registrations, PageRequest.of(0, 10), 1);

        when(registrationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(paymentOrderRepository.findByUserAndItemTypeAndItemIdOrderByCreatedAtDesc(any(User.class), anyString(), anyString())).thenReturn(Arrays.asList(mockPaymentOrder));
        when(gymServiceService.getServiceById(anyLong())).thenReturn(null);
        when(userMapper.toResponse(any(User.class))).thenReturn(null);

        // Act
        Page<ServiceRegistrationResponse> result = serviceRegistrationService.getAllRegistrationsWithFilters(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(registrationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllRegistrationsWithFilters_WithSearchQuery_ReturnsFilteredRegistrations() {
        // Arrange
        ServiceRegistrationFilterRequest request = new ServiceRegistrationFilterRequest();
        request.setSearchQuery("John");
        request.setPage(0);
        request.setSize(10);

        List<ServiceRegistration> registrations = Arrays.asList(mockRegistration);
        Page<ServiceRegistration> page = new PageImpl<>(registrations, PageRequest.of(0, 10), 1);

        when(registrationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(paymentOrderRepository.findByUserAndItemTypeAndItemIdOrderByCreatedAtDesc(any(User.class), anyString(), anyString())).thenReturn(Arrays.asList(mockPaymentOrder));
        when(gymServiceService.getServiceById(anyLong())).thenReturn(null);
        when(userMapper.toResponse(any(User.class))).thenReturn(null);

        // Act
        Page<ServiceRegistrationResponse> result = serviceRegistrationService.getAllRegistrationsWithFilters(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(registrationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllRegistrationsWithFilters_WithMultipleFilters_ReturnsFilteredRegistrations() {
        // Arrange
        ServiceRegistrationFilterRequest request = new ServiceRegistrationFilterRequest();
        request.setStatus(RegistrationStatus.ACTIVE);
        request.setRegistrationType(RegistrationType.ONLINE);
        request.setSearchQuery("Training");
        request.setPage(0);
        request.setSize(10);

        List<ServiceRegistration> registrations = Arrays.asList(mockRegistration);
        Page<ServiceRegistration> page = new PageImpl<>(registrations, PageRequest.of(0, 10), 1);

        when(registrationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(paymentOrderRepository.findByUserAndItemTypeAndItemIdOrderByCreatedAtDesc(any(User.class), anyString(), anyString())).thenReturn(Arrays.asList(mockPaymentOrder));
        when(gymServiceService.getServiceById(anyLong())).thenReturn(null);
        when(userMapper.toResponse(any(User.class))).thenReturn(null);

        // Act
        Page<ServiceRegistrationResponse> result = serviceRegistrationService.getAllRegistrationsWithFilters(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(registrationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllRegistrationsWithFilters_WithPagination_ReturnsCorrectPage() {
        // Arrange
        ServiceRegistrationFilterRequest request = new ServiceRegistrationFilterRequest();
        request.setPage(1);
        request.setSize(5);

        List<ServiceRegistration> registrations = Arrays.asList(mockRegistration);
        Page<ServiceRegistration> page = new PageImpl<>(registrations, PageRequest.of(1, 5), 10);

        when(registrationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(paymentOrderRepository.findByUserAndItemTypeAndItemIdOrderByCreatedAtDesc(any(User.class), anyString(), anyString())).thenReturn(Arrays.asList(mockPaymentOrder));
        when(gymServiceService.getServiceById(anyLong())).thenReturn(null);
        when(userMapper.toResponse(any(User.class))).thenReturn(null);

        // Act
        Page<ServiceRegistrationResponse> result = serviceRegistrationService.getAllRegistrationsWithFilters(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        verify(registrationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAvailableTrainersForRegistration_ValidRegistrationId_ReturnsAvailableTrainers() {
        // Arrange
        Long registrationId = 1L;
        
        // Setup service category
        ServiceCategory category = new ServiceCategory();
        category.setId(1L);
        category.setName("Fitness");
        
        mockService.setCategory(category);
        mockRegistration.setGymService(mockService);
        
        // Setup trainers
        User trainer1 = new User();
        trainer1.setId(10L);
        trainer1.setFullName("Trainer One");
        trainer1.setAvatar("avatar1.jpg");
        
        User trainer2 = new User();
        trainer2.setId(11L);
        trainer2.setFullName("Trainer Two");
        trainer2.setAvatar("avatar2.jpg");
        
        // Setup trainer specialties
        TrainerSpecialty specialty1 = new TrainerSpecialty();
        specialty1.setId(1L);
        specialty1.setUser(trainer1);
        specialty1.setSpecialty(category);
        specialty1.setExperienceYears(5);
        
        TrainerSpecialty specialty2 = new TrainerSpecialty();
        specialty2.setId(2L);
        specialty2.setUser(trainer2);
        specialty2.setSpecialty(category);
        specialty2.setExperienceYears(3);
        
        List<TrainerSpecialty> trainerSpecialties = Arrays.asList(specialty1, specialty2);
        
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(mockRegistration));
        when(trainerSpecialtyRepository.findTrainerSpecialtiesByCategory(category.getId())).thenReturn(trainerSpecialties);
        
        // Act
        List<com.example.project_backend04.dto.response.Trainer.AvailableTrainerResponse> result = 
            serviceRegistrationService.getAvailableTrainersForRegistration(registrationId);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify trainer 1
        com.example.project_backend04.dto.response.Trainer.AvailableTrainerResponse trainerResponse1 = result.stream()
            .filter(t -> t.getId().equals(10L))
            .findFirst()
            .orElse(null);
        assertNotNull(trainerResponse1);
        assertEquals("Trainer One", trainerResponse1.getFullName());
        assertEquals("avatar1.jpg", trainerResponse1.getAvatar());
        assertEquals(5, trainerResponse1.getTotalExperienceYears());
        assertTrue(trainerResponse1.getSpecialtyNames().contains("Fitness"));
        
        // Verify trainer 2
        com.example.project_backend04.dto.response.Trainer.AvailableTrainerResponse trainerResponse2 = result.stream()
            .filter(t -> t.getId().equals(11L))
            .findFirst()
            .orElse(null);
        assertNotNull(trainerResponse2);
        assertEquals("Trainer Two", trainerResponse2.getFullName());
        assertEquals("avatar2.jpg", trainerResponse2.getAvatar());
        assertEquals(3, trainerResponse2.getTotalExperienceYears());
        assertTrue(trainerResponse2.getSpecialtyNames().contains("Fitness"));
        
        verify(registrationRepository, times(1)).findById(registrationId);
        verify(trainerSpecialtyRepository, times(1)).findTrainerSpecialtiesByCategory(category.getId());
    }
    
    @Test
    void getAvailableTrainersForRegistration_RegistrationNotFound_ThrowsException() {
        // Arrange
        Long registrationId = 999L;
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            serviceRegistrationService.getAvailableTrainersForRegistration(registrationId);
        });
        
        assertEquals("Registration not found", exception.getMessage());
        verify(registrationRepository, times(1)).findById(registrationId);
        verify(trainerSpecialtyRepository, never()).findTrainerSpecialtiesByCategory(anyLong());
    }
    
    @Test
    void getAvailableTrainersForRegistration_NoTrainersAvailable_ReturnsEmptyList() {
        // Arrange
        Long registrationId = 1L;
        
        ServiceCategory category = new ServiceCategory();
        category.setId(1L);
        category.setName("Fitness");
        
        mockService.setCategory(category);
        mockRegistration.setGymService(mockService);
        
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(mockRegistration));
        when(trainerSpecialtyRepository.findTrainerSpecialtiesByCategory(category.getId())).thenReturn(Collections.emptyList());
        
        // Act
        List<com.example.project_backend04.dto.response.Trainer.AvailableTrainerResponse> result = 
            serviceRegistrationService.getAvailableTrainersForRegistration(registrationId);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(registrationRepository, times(1)).findById(registrationId);
        verify(trainerSpecialtyRepository, times(1)).findTrainerSpecialtiesByCategory(category.getId());
    }
}
