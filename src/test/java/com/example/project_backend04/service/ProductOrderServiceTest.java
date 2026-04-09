package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.CreateProductOrderRequest;
import com.example.project_backend04.dto.request.ProductOrderItemRequest;
import com.example.project_backend04.dto.request.UpdateDeliveryStatusRequest;
import com.example.project_backend04.dto.request.UpdatePaymentStatusRequest;
import com.example.project_backend04.dto.response.Product.ProductOrderResponse;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.enums.DeliveryStatus;
import com.example.project_backend04.enums.SaleType;
import com.example.project_backend04.exception.InsufficientStockException;
import com.example.project_backend04.exception.InvalidDeliveryStatusTransitionException;
import com.example.project_backend04.exception.InvalidStatusTransitionException;
import com.example.project_backend04.exception.ProductNotFoundException;
import com.example.project_backend04.exception.ProductOrderNotFoundException;
import com.example.project_backend04.repository.ProductOrderRepository;
import com.example.project_backend04.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductOrderServiceTest {

    @Mock
    private ProductOrderRepository productOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductOrderService productOrderService;

    private User mockUser;
    private Product mockProduct1;
    private Product mockProduct2;
    private CreateProductOrderRequest mockRequest;
    private ProductOrder mockProductOrder;

    @BeforeEach
    void setUp() {
        // Setup mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFullName("Test User");

        // Setup mock products
        mockProduct1 = new Product();
        mockProduct1.setId(1L);
        mockProduct1.setName("Protein Powder");
        mockProduct1.setPrice(new BigDecimal("50.00"));
        mockProduct1.setStock(100);
        mockProduct1.setLowStockThreshold(10);

        mockProduct2 = new Product();
        mockProduct2.setId(2L);
        mockProduct2.setName("Energy Bar");
        mockProduct2.setPrice(new BigDecimal("5.00"));
        mockProduct2.setStock(200);
        mockProduct2.setLowStockThreshold(20);

        // Setup mock request
        mockRequest = new CreateProductOrderRequest();
        mockRequest.setCustomerName("John Doe");
        mockRequest.setCustomerPhone("1234567890");
        mockRequest.setCustomerAddress("123 Main St");
        mockRequest.setSaleType(SaleType.ONLINE);
        mockRequest.setNotes("Test order");

        List<ProductOrderItemRequest> itemRequests = new ArrayList<>();
        ProductOrderItemRequest itemRequest1 = new ProductOrderItemRequest();
        itemRequest1.setProductId(1L);
        itemRequest1.setQuantity(2);
        itemRequests.add(itemRequest1);

        ProductOrderItemRequest itemRequest2 = new ProductOrderItemRequest();
        itemRequest2.setProductId(2L);
        itemRequest2.setQuantity(5);
        itemRequests.add(itemRequest2);

        mockRequest.setItems(itemRequests);

        // Setup mock product order
        mockProductOrder = new ProductOrder();
        mockProductOrder.setId(1L);
        mockProductOrder.setUser(mockUser);
        mockProductOrder.setCustomerName("John Doe");
        mockProductOrder.setCustomerPhone("1234567890");
        mockProductOrder.setCustomerAddress("123 Main St");
        mockProductOrder.setSaleType(SaleType.ONLINE);
        mockProductOrder.setTotalAmount(new BigDecimal("125.00"));
        mockProductOrder.setPaymentStatus(PaymentStatus.PENDING);
        mockProductOrder.setDeliveryStatus(DeliveryStatus.PENDING);
        mockProductOrder.setNotes("Test order");
        mockProductOrder.setCreatedAt(LocalDateTime.now());
        mockProductOrder.setUpdatedAt(LocalDateTime.now());

        List<ProductOrderItem> items = new ArrayList<>();
        ProductOrderItem item1 = new ProductOrderItem();
        item1.setId(1L);
        item1.setProduct(mockProduct1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("50.00"));
        item1.setSubtotal(new BigDecimal("100.00"));
        item1.setProductOrder(mockProductOrder);
        items.add(item1);

        ProductOrderItem item2 = new ProductOrderItem();
        item2.setId(2L);
        item2.setProduct(mockProduct2);
        item2.setQuantity(5);
        item2.setUnitPrice(new BigDecimal("5.00"));
        item2.setSubtotal(new BigDecimal("25.00"));
        item2.setProductOrder(mockProductOrder);
        items.add(item2);

        mockProductOrder.setItems(items);
    }

    @Test
    void createProductOrder_ValidRequest_ReturnsProductOrderResponse() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productOrderRepository.save(any(ProductOrder.class))).thenReturn(mockProductOrder);

        // Act
        ProductOrderResponse result = productOrderService.createProductOrder(mockRequest, mockUser);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getCustomerName());
        assertEquals("1234567890", result.getCustomerPhone());
        assertEquals("123 Main St", result.getCustomerAddress());
        assertEquals(SaleType.ONLINE, result.getSaleType());
        assertEquals(new BigDecimal("125.00"), result.getTotalAmount());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        assertEquals(DeliveryStatus.PENDING, result.getDeliveryStatus());
        assertEquals(2, result.getItemCount());
        assertEquals(1L, result.getUserId());

        verify(productOrderRepository, times(1)).save(any(ProductOrder.class));
    }

    @Test
    void createProductOrder_CounterSale_SetsDeliveryStatusToDelivered() {
        // Arrange
        mockRequest.setSaleType(SaleType.COUNTER);
        mockProductOrder.setSaleType(SaleType.COUNTER);
        mockProductOrder.setDeliveryStatus(DeliveryStatus.DELIVERED);

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productOrderRepository.save(any(ProductOrder.class))).thenReturn(mockProductOrder);

        // Act
        ProductOrderResponse result = productOrderService.createProductOrder(mockRequest, mockUser);

        // Assert
        assertNotNull(result);
        assertEquals(SaleType.COUNTER, result.getSaleType());
        assertEquals(DeliveryStatus.DELIVERED, result.getDeliveryStatus());
    }

    @Test
    void createProductOrder_NullUser_CreatesOrderWithoutUser() {
        // Arrange
        mockProductOrder.setUser(null);
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productOrderRepository.save(any(ProductOrder.class))).thenReturn(mockProductOrder);

        // Act
        ProductOrderResponse result = productOrderService.createProductOrder(mockRequest, null);

        // Assert
        assertNotNull(result);
        assertNull(result.getUserId());
    }

    @Test
    void createProductOrder_ProductNotFound_ThrowsProductNotFoundException() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> productOrderService.createProductOrder(mockRequest, mockUser));

        assertEquals("Product not found with id: 1", exception.getMessage());
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }

    @Test
    void createProductOrder_InsufficientStock_ThrowsInsufficientStockException() {
        // Arrange
        mockProduct1.setStock(1); // Less than requested quantity of 2
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));

        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> productOrderService.createProductOrder(mockRequest, mockUser));

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertTrue(exception.getMessage().contains("Protein Powder"));
        assertEquals(1L, exception.getProductId());
        assertEquals("Protein Powder", exception.getProductName());
        assertEquals(2, exception.getRequestedQuantity());
        assertEquals(1, exception.getAvailableStock());
        
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }

    @Test
    void createProductOrder_CapturesCurrentProductPrice() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productOrderRepository.save(any(ProductOrder.class))).thenReturn(mockProductOrder);

        // Act
        ProductOrderResponse result = productOrderService.createProductOrder(mockRequest, mockUser);

        // Assert
        assertNotNull(result);
        // Verify that the order was created with current product prices
        verify(productOrderRepository, times(1)).save(argThat(order -> {
            ProductOrderItem item1 = order.getItems().get(0);
            ProductOrderItem item2 = order.getItems().get(1);
            
            // Verify unit prices match current product prices
            return item1.getUnitPrice().equals(mockProduct1.getPrice()) &&
                   item2.getUnitPrice().equals(mockProduct2.getPrice());
        }));
    }

    @Test
    void createProductOrder_CalculatesTotalAmountCorrectly() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productOrderRepository.save(any(ProductOrder.class))).thenReturn(mockProductOrder);

        // Act
        ProductOrderResponse result = productOrderService.createProductOrder(mockRequest, mockUser);

        // Assert
        assertNotNull(result);
        // Total should be: (2 * 50.00) + (5 * 5.00) = 100.00 + 25.00 = 125.00
        assertEquals(new BigDecimal("125.00"), result.getTotalAmount());
    }

    @Test
    void createProductOrder_SetsPaymentStatusToPending() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productOrderRepository.save(any(ProductOrder.class))).thenReturn(mockProductOrder);

        // Act
        ProductOrderResponse result = productOrderService.createProductOrder(mockRequest, mockUser);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
    }

    @Test
    void createProductOrder_ValidatesAllProductsBeforeCreating() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> productOrderService.createProductOrder(mockRequest, mockUser));

        assertEquals("Product not found with id: 2", exception.getMessage());
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }

    @Test
    void createProductOrder_ValidatesAllStockBeforeCreating() {
        // Arrange
        mockProduct2.setStock(3); // Less than requested quantity of 5
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));

        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> productOrderService.createProductOrder(mockRequest, mockUser));

        assertTrue(exception.getMessage().contains("Energy Bar"));
        assertEquals(2L, exception.getProductId());
        assertEquals(5, exception.getRequestedQuantity());
        assertEquals(3, exception.getAvailableStock());
        
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    // ========== Payment Status Update Tests ==========
    
    @Test
    void updatePaymentStatus_PendingToPaid_DeductsStockSuccessfully() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PAID);
        
        mockProductOrder.setPaymentStatus(PaymentStatus.PENDING);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findByIdWithLock(2L)).thenReturn(Optional.of(mockProduct2));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productOrderRepository.save(any(ProductOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProductOrderResponse result = productOrderService.updatePaymentStatus(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.PAID, result.getPaymentStatus());
        
        // Verify stock was deducted
        verify(productRepository, times(1)).findByIdWithLock(1L);
        verify(productRepository, times(1)).findByIdWithLock(2L);
        verify(productRepository, times(2)).save(any(Product.class));
        
        // Verify stock values were updated correctly
        assertEquals(98, mockProduct1.getStock()); // 100 - 2
        assertEquals(195, mockProduct2.getStock()); // 200 - 5
    }
    
    @Test
    void updatePaymentStatus_PendingToPaid_InsufficientStock_ThrowsException() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PAID);
        
        mockProductOrder.setPaymentStatus(PaymentStatus.PENDING);
        mockProduct1.setStock(1); // Insufficient for quantity 2
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(mockProduct1));
        
        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> productOrderService.updatePaymentStatus(1L, request));
        
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertEquals(1L, exception.getProductId());
        assertEquals(2, exception.getRequestedQuantity());
        assertEquals(1, exception.getAvailableStock());
        
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updatePaymentStatus_PaidToCancelled_RestoresStockSuccessfully() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.CANCELLED);
        
        mockProductOrder.setPaymentStatus(PaymentStatus.PAID);
        mockProduct1.setStock(98); // Already deducted
        mockProduct2.setStock(195); // Already deducted
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findByIdWithLock(2L)).thenReturn(Optional.of(mockProduct2));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productOrderRepository.save(any(ProductOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProductOrderResponse result = productOrderService.updatePaymentStatus(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.CANCELLED, result.getPaymentStatus());
        
        // Verify stock was restored
        verify(productRepository, times(1)).findByIdWithLock(1L);
        verify(productRepository, times(1)).findByIdWithLock(2L);
        verify(productRepository, times(2)).save(any(Product.class));
        
        // Verify stock values were restored correctly
        assertEquals(100, mockProduct1.getStock()); // 98 + 2
        assertEquals(200, mockProduct2.getStock()); // 195 + 5
    }
    
    @Test
    void updatePaymentStatus_PendingToCancelled_NoStockChanges() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.CANCELLED);
        
        mockProductOrder.setPaymentStatus(PaymentStatus.PENDING);
        int initialStock1 = mockProduct1.getStock();
        int initialStock2 = mockProduct2.getStock();
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productOrderRepository.save(any(ProductOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProductOrderResponse result = productOrderService.updatePaymentStatus(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.CANCELLED, result.getPaymentStatus());
        
        // Verify no stock operations were performed
        verify(productRepository, never()).findByIdWithLock(any());
        verify(productRepository, never()).save(any(Product.class));
        
        // Verify stock values remain unchanged
        assertEquals(initialStock1, mockProduct1.getStock());
        assertEquals(initialStock2, mockProduct2.getStock());
    }
    
    @Test
    void updatePaymentStatus_InvalidTransition_PaidToPending_ThrowsException() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PENDING);
        
        mockProductOrder.setPaymentStatus(PaymentStatus.PAID);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(InvalidStatusTransitionException.class,
                () -> productOrderService.updatePaymentStatus(1L, request));
        
        assertTrue(exception.getMessage().contains("Invalid payment status transition from PAID to PENDING"));
        assertEquals(PaymentStatus.PAID, exception.getCurrentStatus());
        assertEquals(PaymentStatus.PENDING, exception.getRequestedStatus());
        
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updatePaymentStatus_InvalidTransition_CancelledToPaid_ThrowsException() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PAID);
        
        mockProductOrder.setPaymentStatus(PaymentStatus.CANCELLED);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(InvalidStatusTransitionException.class,
                () -> productOrderService.updatePaymentStatus(1L, request));
        
        assertTrue(exception.getMessage().contains("Invalid payment status transition from CANCELLED to PAID"));
        assertEquals(PaymentStatus.CANCELLED, exception.getCurrentStatus());
        assertEquals(PaymentStatus.PAID, exception.getRequestedStatus());
        
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updatePaymentStatus_InvalidTransition_CancelledToPending_ThrowsException() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PENDING);
        
        mockProductOrder.setPaymentStatus(PaymentStatus.CANCELLED);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(InvalidStatusTransitionException.class,
                () -> productOrderService.updatePaymentStatus(1L, request));
        
        assertTrue(exception.getMessage().contains("Invalid payment status transition from CANCELLED to PENDING"));
        
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updatePaymentStatus_OrderNotFound_ThrowsException() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PAID);
        
        when(productOrderRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ProductOrderNotFoundException exception = assertThrows(ProductOrderNotFoundException.class,
                () -> productOrderService.updatePaymentStatus(999L, request));
        
        assertEquals("Product order not found with id: 999", exception.getMessage());
        assertEquals(999L, exception.getOrderId());
    }
    
    @Test
    void updatePaymentStatus_UsesPessimisticLocking() {
        // Arrange
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PAID);
        
        mockProductOrder.setPaymentStatus(PaymentStatus.PENDING);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findByIdWithLock(2L)).thenReturn(Optional.of(mockProduct2));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productOrderRepository.save(any(ProductOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        productOrderService.updatePaymentStatus(1L, request);
        
        // Assert - Verify pessimistic locking was used
        verify(productRepository, times(1)).findByIdWithLock(1L);
        verify(productRepository, times(1)).findByIdWithLock(2L);
        verify(productRepository, never()).findById(any()); // Should not use regular findById
    }
    
    // ========== Delivery Status Update Tests ==========
    
    @Test
    void updateDeliveryStatus_PendingToProcessing_UpdatesSuccessfully() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.PROCESSING);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.PENDING);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productOrderRepository.save(any(ProductOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProductOrderResponse result = productOrderService.updateDeliveryStatus(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(DeliveryStatus.PROCESSING, result.getDeliveryStatus());
        verify(productOrderRepository, times(1)).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_ProcessingToShipped_UpdatesSuccessfully() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.SHIPPED);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.PROCESSING);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productOrderRepository.save(any(ProductOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProductOrderResponse result = productOrderService.updateDeliveryStatus(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(DeliveryStatus.SHIPPED, result.getDeliveryStatus());
        verify(productOrderRepository, times(1)).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_ShippedToDelivered_UpdatesSuccessfully() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.DELIVERED);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.SHIPPED);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productOrderRepository.save(any(ProductOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProductOrderResponse result = productOrderService.updateDeliveryStatus(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(DeliveryStatus.DELIVERED, result.getDeliveryStatus());
        verify(productOrderRepository, times(1)).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_AnyStateToCancelled_UpdatesSuccessfully() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.CANCELLED);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.PROCESSING);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        when(productOrderRepository.save(any(ProductOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProductOrderResponse result = productOrderService.updateDeliveryStatus(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(DeliveryStatus.CANCELLED, result.getDeliveryStatus());
        verify(productOrderRepository, times(1)).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_CounterOrder_ThrowsException() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.PROCESSING);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.DELIVERED);
        mockProductOrder.setSaleType(SaleType.COUNTER);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidDeliveryStatusTransitionException exception = assertThrows(
            InvalidDeliveryStatusTransitionException.class,
            () -> productOrderService.updateDeliveryStatus(1L, request)
        );
        
        assertTrue(exception.getMessage().contains("Invalid delivery status transition"));
        assertEquals(DeliveryStatus.DELIVERED, exception.getCurrentStatus());
        assertEquals(DeliveryStatus.PROCESSING, exception.getRequestedStatus());
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_SkipState_PendingToShipped_ThrowsException() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.SHIPPED);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.PENDING);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidDeliveryStatusTransitionException exception = assertThrows(
            InvalidDeliveryStatusTransitionException.class,
            () -> productOrderService.updateDeliveryStatus(1L, request)
        );
        
        assertTrue(exception.getMessage().contains("Invalid delivery status transition from PENDING to SHIPPED"));
        assertEquals(DeliveryStatus.PENDING, exception.getCurrentStatus());
        assertEquals(DeliveryStatus.SHIPPED, exception.getRequestedStatus());
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_SkipState_PendingToDelivered_ThrowsException() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.DELIVERED);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.PENDING);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidDeliveryStatusTransitionException exception = assertThrows(
            InvalidDeliveryStatusTransitionException.class,
            () -> productOrderService.updateDeliveryStatus(1L, request)
        );
        
        assertTrue(exception.getMessage().contains("Invalid delivery status transition from PENDING to DELIVERED"));
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_Backwards_ShippedToProcessing_ThrowsException() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.PROCESSING);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.SHIPPED);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidDeliveryStatusTransitionException exception = assertThrows(
            InvalidDeliveryStatusTransitionException.class,
            () -> productOrderService.updateDeliveryStatus(1L, request)
        );
        
        assertTrue(exception.getMessage().contains("Invalid delivery status transition from SHIPPED to PROCESSING"));
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_Backwards_DeliveredToShipped_ThrowsException() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.SHIPPED);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.DELIVERED);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidDeliveryStatusTransitionException exception = assertThrows(
            InvalidDeliveryStatusTransitionException.class,
            () -> productOrderService.updateDeliveryStatus(1L, request)
        );
        
        assertTrue(exception.getMessage().contains("Invalid delivery status transition from DELIVERED to SHIPPED"));
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_FromCancelled_ThrowsException() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.PROCESSING);
        
        mockProductOrder.setDeliveryStatus(DeliveryStatus.CANCELLED);
        mockProductOrder.setSaleType(SaleType.ONLINE);
        
        when(productOrderRepository.findById(1L)).thenReturn(Optional.of(mockProductOrder));
        
        // Act & Assert
        InvalidDeliveryStatusTransitionException exception = assertThrows(
            InvalidDeliveryStatusTransitionException.class,
            () -> productOrderService.updateDeliveryStatus(1L, request)
        );
        
        assertTrue(exception.getMessage().contains("Invalid delivery status transition from CANCELLED to PROCESSING"));
        verify(productOrderRepository, never()).save(any(ProductOrder.class));
    }
    
    @Test
    void updateDeliveryStatus_OrderNotFound_ThrowsException() {
        // Arrange
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setDeliveryStatus(DeliveryStatus.PROCESSING);
        
        when(productOrderRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ProductOrderNotFoundException exception = assertThrows(
            ProductOrderNotFoundException.class,
            () -> productOrderService.updateDeliveryStatus(999L, request)
        );
        
        assertEquals("Product order not found with id: 999", exception.getMessage());
        assertEquals(999L, exception.getOrderId());
    }
}
