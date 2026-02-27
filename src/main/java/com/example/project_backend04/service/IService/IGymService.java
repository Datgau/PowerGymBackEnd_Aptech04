package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.Service.GymServiceRequest;
import com.example.project_backend04.dto.request.Service.UpdateGymServiceDto;
import com.example.project_backend04.dto.response.Service.GymServiceResponse;

import java.util.List;

public interface IGymService {
    List<GymServiceResponse> getPublicServices();
    List<GymServiceResponse> getAllServices();
    GymServiceResponse getServiceById(Long id);
    GymServiceResponse createService(GymServiceRequest request) throws Exception;
    GymServiceResponse updateService(Long id, UpdateGymServiceDto request);
    void deleteService(Long id);
}
