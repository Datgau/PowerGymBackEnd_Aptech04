package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.CreateImportReceiptRequest;
import com.example.project_backend04.dto.request.ImportReceiptItemRequest;
import com.example.project_backend04.dto.request.UpdateImportReceiptRequest;
import com.example.project_backend04.dto.response.Product.ImportReceiptDetailResponse;
import com.example.project_backend04.dto.response.Product.ImportReceiptItemResponse;
import com.example.project_backend04.dto.response.Product.ImportReceiptResponse;
import com.example.project_backend04.entity.ImportReceipt;
import com.example.project_backend04.entity.ImportReceiptItem;
import com.example.project_backend04.entity.Product;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.exception.ImportReceiptNotFoundException;
import com.example.project_backend04.exception.ProductNotFoundException;
import com.example.project_backend04.repository.ImportReceiptRepository;
import com.example.project_backend04.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportReceiptService {
    
    private final ImportReceiptRepository importReceiptRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ImportReceiptResponse createImportReceipt(CreateImportReceiptRequest request, User createdBy) {
        log.info("Creating import receipt for supplier: {}", request.getSupplierName());
        List<Product> products = new ArrayList<>();
        for (ImportReceiptItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.getProductId()));
            products.add(product);
        }
        ImportReceipt importReceipt = new ImportReceipt();
        importReceipt.setSupplierName(request.getSupplierName());
        importReceipt.setNotes(request.getNotes());
        importReceipt.setCreatedBy(createdBy);
        importReceipt.setTotalCost(BigDecimal.ZERO);
        List<ImportReceiptItem> items = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            ImportReceiptItemRequest itemRequest = request.getItems().get(i);
            Product product = products.get(i);
            ImportReceiptItem item = new ImportReceiptItem();
            item.setImportReceipt(importReceipt);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setSubtotal(itemRequest.getUnitPrice().multiply(new BigDecimal(itemRequest.getQuantity())));
            
            items.add(item);
            product.setStock(product.getStock() + itemRequest.getQuantity());
            productRepository.save(product);
            
            log.debug("Increased stock for product {} by {}", product.getName(), itemRequest.getQuantity());
        }
        
        importReceipt.setItems(items);
        
        importReceipt.calculateTotalCost();
        ImportReceipt savedReceipt = importReceiptRepository.save(importReceipt);
        
        log.info("Import receipt created successfully with id: {}", savedReceipt.getId());
        
        return mapToResponse(savedReceipt);
    }

    @Transactional(readOnly = true)
    public ImportReceiptDetailResponse getImportReceiptById(Long id) {
        ImportReceipt importReceipt = importReceiptRepository.findById(id)
                .orElseThrow(() -> new ImportReceiptNotFoundException(id));
        
        return mapToDetailResponse(importReceipt);
    }


    @Transactional(readOnly = true)
    public Page<ImportReceiptResponse> getAllImportReceipts(
            int page,
            int size,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String supplierName) {

        final String finalSupplierName;
        if (supplierName != null) {
            String trimmed = supplierName.trim();
            finalSupplierName = trimmed.isEmpty() ? null : trimmed;
        } else {
            finalSupplierName = null;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<ImportReceipt> spec = (root, query, cb) -> {
            root.fetch("createdBy", jakarta.persistence.criteria.JoinType.LEFT);
            
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            if (finalSupplierName != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("supplierName")),
                        "%" + finalSupplierName.toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ImportReceipt> receipts = importReceiptRepository.findAll(spec, pageable);

        return receipts.map(this::mapToResponse);
    }

    private ImportReceiptResponse mapToResponse(ImportReceipt importReceipt) {
        String createdByName = null;
        String createdByEmail = null;
        try {
            if (importReceipt.getCreatedBy() != null) {
                createdByName = importReceipt.getCreatedBy().getFullName();
                createdByEmail = importReceipt.getCreatedBy().getEmail();
            }
        } catch (Exception e) {
            log.warn("Could not fetch createdBy info for receipt {}: {}", importReceipt.getId(), e.getMessage());
        }
        
        return ImportReceiptResponse.builder()
                .id(importReceipt.getId())
                .supplierName(importReceipt.getSupplierName())
                .totalCost(importReceipt.getTotalCost())
                .notes(importReceipt.getNotes())
                .createdById(importReceipt.getCreatedBy() != null ? importReceipt.getCreatedBy().getId() : null)
                .createdByName(createdByName)
                .createdByEmail(createdByEmail)
                .createdAt(importReceipt.getCreatedAt())
                .itemCount(importReceipt.getItems().size())
                .build();
    }

    private ImportReceiptDetailResponse mapToDetailResponse(ImportReceipt importReceipt) {
        List<ImportReceiptItemResponse> itemResponses = importReceipt.getItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
        
        return ImportReceiptDetailResponse.builder()
                .id(importReceipt.getId())
                .supplierName(importReceipt.getSupplierName())
                .totalCost(importReceipt.getTotalCost())
                .notes(importReceipt.getNotes())
                .createdById(importReceipt.getCreatedBy().getId())
                .createdByName(importReceipt.getCreatedBy().getFullName())
                .createdAt(importReceipt.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    private ImportReceiptItemResponse mapToItemResponse(ImportReceiptItem item) {
        return ImportReceiptItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
    

    @Transactional
    public ImportReceiptResponse updateImportReceipt(Long id, UpdateImportReceiptRequest request, User user) {
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }
        
        ImportReceipt importReceipt = importReceiptRepository.findById(id)
                .orElseThrow(() -> new ImportReceiptNotFoundException(id));
        
        for (ImportReceiptItem oldItem : importReceipt.getItems()) {
            Product product = oldItem.getProduct();
            product.setStock(product.getStock() - oldItem.getQuantity());
            productRepository.save(product);
        }
        
        importReceipt.setSupplierName(request.getSupplierName());
        importReceipt.setNotes(request.getNotes());
        
        List<Product> products = new ArrayList<>();
        for (ImportReceiptItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.getProductId()));
            products.add(product);
        }
        List<ImportReceiptItem> oldItems = new ArrayList<>(importReceipt.getItems());
        importReceipt.getItems().clear();
        importReceiptRepository.flush();
        
        for (int i = 0; i < request.getItems().size(); i++) {
            ImportReceiptItemRequest itemRequest = request.getItems().get(i);
            Product product = products.get(i);
            
            ImportReceiptItem item = new ImportReceiptItem();
            item.setImportReceipt(importReceipt);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setSubtotal(itemRequest.getUnitPrice().multiply(new BigDecimal(itemRequest.getQuantity())));
            
            importReceipt.getItems().add(item);
            
            // Increase product stock
            product.setStock(product.getStock() + itemRequest.getQuantity());
            productRepository.save(product);
            
            log.debug("Increased stock for product {} by {}", product.getName(), itemRequest.getQuantity());
        }
        
        importReceipt.calculateTotalCost();
        
        ImportReceipt savedReceipt = importReceiptRepository.save(importReceipt);
        
        log.info("Import receipt updated successfully with id: {}", savedReceipt.getId());
        
        return mapToResponse(savedReceipt);
    }

    @Transactional
    public void deleteImportReceipt(Long id, String password, User user) {
        log.info("Deleting import receipt with id: {}", id);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }
        ImportReceipt importReceipt = importReceiptRepository.findById(id)
                .orElseThrow(() -> new ImportReceiptNotFoundException(id));
        for (ImportReceiptItem item : importReceipt.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
            log.debug("Restored stock for product {} by {}", product.getName(), item.getQuantity());
        }
        importReceiptRepository.delete(importReceipt);
        
        log.info("Import receipt deleted successfully with id: {}", id);
    }
}
