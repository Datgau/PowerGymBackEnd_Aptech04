package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Service.GymServiceRequest;
import com.example.project_backend04.dto.request.Service.UpdateGymServiceDto;
import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface IGymService {
    List<GymServiceResponse> getPublicServices();
    Page<GymServiceResponse> getPublicServices(int page, int size);
    List<GymServiceResponse> getAllServices();
    Page<GymServiceResponse> getAllServices(int page, int size);
    GymServiceResponse getServiceById(Long id);
    Map<String, Object> getServiceRegistrationStats(Long id);
    GymServiceResponse createService(GymServiceRequest request) throws Exception;
    GymServiceResponse updateService(Long id, UpdateGymServiceDto request);
    void deleteService(Long id);
}
