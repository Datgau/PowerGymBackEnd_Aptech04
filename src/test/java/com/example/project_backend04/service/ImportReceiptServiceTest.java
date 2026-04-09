package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.CreateImportReceiptRequest;
import com.example.project_backend04.dto.request.ImportReceiptItemRequest;
import com.example.project_backend04.dto.response.Product.ImportReceiptDetailResponse;
import com.example.project_backend04.dto.response.Product.ImportReceiptResponse;
import com.example.project_backend04.entity.ImportReceipt;
import com.example.project_backend04.entity.ImportReceiptItem;
import com.example.project_backend04.entity.Product;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.exception.ImportReceiptNotFoundException;
import com.example.project_backend04.exception.ProductNotFoundException;
import com.example.project_backend04.repository.ImportReceiptRepository;
import com.example.project_backend04.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportReceiptServiceTest {

    @Mock
    private ImportReceiptRepository importReceiptRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ImportReceiptService importReceiptService;

    private User mockUser;
    private Product mockProduct1;
    private Product mockProduct2;
    private ImportReceipt mockImportReceipt;
    private CreateImportReceiptRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Setup mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFullName("Admin User");

        // Setup mock products
        mockProduct1 = new Product();
        mockProduct1.setId(1L);
        mockProduct1.setName("Protein Powder");
        mockProduct1.setPrice(new BigDecimal("50.00"));
        mockProduct1.setStock(10);

        mockProduct2 = new Product();
        mockProduct2.setId(2L);
        mockProduct2.setName("Energy Bar");
        mockProduct2.setPrice(new BigDecimal("5.00"));
        mockProduct2.setStock(20);

        // Setup mock import receipt
        mockImportReceipt = new ImportReceipt();
        mockImportReceipt.setId(1L);
        mockImportReceipt.setSupplierName("Test Supplier");
        mockImportReceipt.setNotes("Test notes");
        mockImportReceipt.setCreatedBy(mockUser);
        mockImportReceipt.setCreatedAt(LocalDateTime.now());
        mockImportReceipt.setTotalCost(new BigDecimal("600.00"));

        // Setup mock items
        List<ImportReceiptItem> items = new ArrayList<>();
        ImportReceiptItem item1 = new ImportReceiptItem();
        item1.setId(1L);
        item1.setProduct(mockProduct1);
        item1.setQuantity(10);
        item1.setUnitPrice(new BigDecimal("50.00"));
        item1.setSubtotal(new BigDecimal("500.00"));
        item1.setImportReceipt(mockImportReceipt);
        items.add(item1);

        ImportReceiptItem item2 = new ImportReceiptItem();
        item2.setId(2L);
        item2.setProduct(mockProduct2);
        item2.setQuantity(20);
        item2.setUnitPrice(new BigDecimal("5.00"));
        item2.setSubtotal(new BigDecimal("100.00"));
        item2.setImportReceipt(mockImportReceipt);
        items.add(item2);

        mockImportReceipt.setItems(items);

        // Setup mock request
        mockRequest = new CreateImportReceiptRequest();
        mockRequest.setSupplierName("Test Supplier");
        mockRequest.setNotes("Test notes");
        
        List<ImportReceiptItemRequest> itemRequests = new ArrayList<>();
        ImportReceiptItemRequest itemRequest1 = new ImportReceiptItemRequest();
        itemRequest1.setProductId(1L);
        itemRequest1.setQuantity(10);
        itemRequest1.setUnitPrice(new BigDecimal("50.00"));
        itemRequests.add(itemRequest1);

        ImportReceiptItemRequest itemRequest2 = new ImportReceiptItemRequest();
        itemRequest2.setProductId(2L);
        itemRequest2.setQuantity(20);
        itemRequest2.setUnitPrice(new BigDecimal("5.00"));
        itemRequests.add(itemRequest2);

        mockRequest.setItems(itemRequests);
    }

    @Test
    void createImportReceipt_ValidRequest_ReturnsImportReceiptResponse() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptRepository.save(any(ImportReceipt.class))).thenReturn(mockImportReceipt);

        // Act
        ImportReceiptResponse result = importReceiptService.createImportReceipt(mockRequest, mockUser);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Supplier", result.getSupplierName());
        assertEquals(new BigDecimal("600.00"), result.getTotalCost());
        assertEquals(2, result.getItemCount());
        assertEquals(1L, result.getCreatedById());
        assertEquals("Admin User", result.getCreatedByName());

        // Verify stock was increased
        verify(productRepository, times(2)).save(any(Product.class));
        verify(importReceiptRepository, times(1)).save(any(ImportReceipt.class));
    }

    @Test
    void createImportReceipt_ProductNotFound_ThrowsProductNotFoundException() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> importReceiptService.createImportReceipt(mockRequest, mockUser));
        
        assertEquals("Product not found with id: 1", exception.getMessage());
        verify(importReceiptRepository, never()).save(any(ImportReceipt.class));
    }

    @Test
    void createImportReceipt_IncreasesProductStock() {
        // Arrange
        int initialStock1 = mockProduct1.getStock();
        int initialStock2 = mockProduct2.getStock();
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptRepository.save(any(ImportReceipt.class))).thenReturn(mockImportReceipt);

        // Act
        importReceiptService.createImportReceipt(mockRequest, mockUser);

        // Assert - verify stock was increased correctly
        assertEquals(initialStock1 + 10, mockProduct1.getStock());
        assertEquals(initialStock2 + 20, mockProduct2.getStock());
    }

    @Test
    void getImportReceiptById_ValidId_ReturnsImportReceiptDetailResponse() {
        // Arrange
        when(importReceiptRepository.findById(1L)).thenReturn(Optional.of(mockImportReceipt));

        // Act
        ImportReceiptDetailResponse result = importReceiptService.getImportReceiptById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Supplier", result.getSupplierName());
        assertEquals(new BigDecimal("600.00"), result.getTotalCost());
        assertEquals(2, result.getItems().size());
        assertEquals("Admin User", result.getCreatedByName());
    }

    @Test
    void getImportReceiptById_InvalidId_ThrowsImportReceiptNotFoundException() {
        // Arrange
        when(importReceiptRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ImportReceiptNotFoundException exception = assertThrows(ImportReceiptNotFoundException.class,
                () -> importReceiptService.getImportReceiptById(999L));
        
        assertEquals("Import receipt not found with id: 999", exception.getMessage());
    }

    @Test
    void getAllImportReceipts_WithFilters_ReturnsPagedResults() {
        // Arrange
        List<ImportReceipt> receipts = List.of(mockImportReceipt);
        Page<ImportReceipt> page = new PageImpl<>(receipts);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        String supplierName = "Test";
        
        when(importReceiptRepository.findByFilters(
                eq(startDate), 
                eq(endDate), 
                eq(supplierName), 
                any(Pageable.class)
        )).thenReturn(page);

        // Act
        Page<ImportReceiptResponse> result = importReceiptService.getAllImportReceipts(
                0, 10, startDate, endDate, supplierName
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Supplier", result.getContent().get(0).getSupplierName());
        verify(importReceiptRepository, times(1)).findByFilters(
                eq(startDate), eq(endDate), eq(supplierName), any(Pageable.class)
        );
    }

    @Test
    void getAllImportReceipts_WithoutFilters_ReturnsAllReceipts() {
        // Arrange
        List<ImportReceipt> receipts = List.of(mockImportReceipt);
        Page<ImportReceipt> page = new PageImpl<>(receipts);
        
        when(importReceiptRepository.findByFilters(
                isNull(), 
                isNull(), 
                isNull(), 
                any(Pageable.class)
        )).thenReturn(page);

        // Act
        Page<ImportReceiptResponse> result = importReceiptService.getAllImportReceipts(
                0, 10, null, null, null
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(importReceiptRepository, times(1)).findByFilters(
                isNull(), isNull(), isNull(), any(Pageable.class)
        );
    }
}
