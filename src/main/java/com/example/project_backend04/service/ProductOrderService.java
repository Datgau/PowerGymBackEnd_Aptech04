package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.CreateProductOrderRequest;
import com.example.project_backend04.dto.request.CreateOrderFromPaymentRequest;
import com.example.project_backend04.dto.request.ProductOrderItemRequest;
import com.example.project_backend04.dto.response.Product.ProductOrderDetailResponse;
import com.example.project_backend04.dto.response.Product.ProductOrderItemResponse;
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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductOrderService {
    
    private final ProductOrderRepository productOrderRepository;
    private final ProductRepository productRepository;
    private final com.example.project_backend04.repository.PaymentOrderRepository paymentOrderRepository;

    @Transactional(readOnly = true)
    public Page<ProductOrderResponse> getAllProductOrders(
            int page,
            int size,
            PaymentStatus paymentStatus,
            DeliveryStatus deliveryStatus,
            SaleType saleType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            User user
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        boolean isAdmin = user.getRole().getName().equals("ADMIN") || 
                          user.getRole().getName().equals("STAFF");

        Specification<ProductOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!isAdmin) {
                predicates.add(cb.equal(root.get("user"), user));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (deliveryStatus != null) {
                predicates.add(cb.equal(root.get("deliveryStatus"), deliveryStatus));
            }
            if (saleType != null) {
                predicates.add(cb.equal(root.get("saleType"), saleType));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("customerName")), searchPattern),
                        cb.like(cb.lower(root.get("customerPhone")), searchPattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ProductOrder> orders = productOrderRepository.findAll(spec, pageable);
        return orders.map(this::buildProductOrderResponse);
    }
    

    @Transactional(readOnly = true)
    public ProductOrderDetailResponse getProductOrderById(Long orderId, User user) {
        ProductOrder order = productOrderRepository.findById(orderId)
            .orElseThrow(() -> new ProductOrderNotFoundException(orderId));
        boolean isAdmin = user.getRole().getName().equals("ADMIN") || 
                          user.getRole().getName().equals("STAFF");
        if (!isAdmin && (order.getUser() == null || !order.getUser().getId().equals(user.getId()))) {
            throw new AccessDeniedException("You don't have permission to access this order");
        }
        
        return buildProductOrderDetailResponse(order);
    }

    @Transactional
    public ProductOrderResponse createProductOrder(CreateProductOrderRequest request, User user) {
        List<Product> products = new ArrayList<>();
        for (ProductOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(itemRequest.getProductId()));
            if (!product.hasStock(itemRequest.getQuantity())) {
                throw new InsufficientStockException(
                    product.getId(),
                    product.getName(),
                    itemRequest.getQuantity(),
                    product.getStock()
                );
            }
            
            products.add(product);
        }

        ProductOrder order = new ProductOrder();
        order.setUser(user);
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerAddress(request.getCustomerAddress());
        order.setSaleType(request.getSaleType());
        order.setNotes(request.getNotes());
        order.setPaymentStatus(PaymentStatus.PENDING);
        // deliveryStatus will be set by @PrePersist based on saleType
        
        // Create ProductOrderItem entities
        List<ProductOrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (int i = 0; i < request.getItems().size(); i++) {
            ProductOrderItemRequest itemRequest = request.getItems().get(i);
            Product product = products.get(i);
            
            ProductOrderItem orderItem = new ProductOrderItem();
            orderItem.setProductOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            // Capture current product price as unitPrice
            orderItem.setUnitPrice(product.getPrice());
            // subtotal will be calculated by @PrePersist
            orderItem.setSubtotal(product.getPrice().multiply(new BigDecimal(itemRequest.getQuantity())));
            
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }
        
        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        
        // Save order (cascade will save items)
        ProductOrder savedOrder = productOrderRepository.save(order);
        
        // Build and return response
        return buildProductOrderResponse(savedOrder);
    }

    @Transactional
    public ProductOrderResponse createProductOrderFromPayment(CreateOrderFromPaymentRequest request, User user) {
        // Validate payment exists and is successful
        PaymentOrder payment = paymentOrderRepository.findById(request.getPaymentId())
            .orElseThrow(() -> new com.example.project_backend04.exception.PaymentOrderNotFoundException(request.getPaymentId()));
        
        // Validate payment status is SUCCESS
        if (payment.getStatus() != com.example.project_backend04.enums.PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Payment must be successful to create order. Current status: " + payment.getStatus());
        }
        
        // Validate payment itemType is PRODUCT
        if (!"PRODUCT".equals(payment.getItemType())) {
            throw new IllegalStateException("Payment itemType must be PRODUCT. Current type: " + payment.getItemType());
        }
        
        // Check if order already exists for this payment (prevent duplicate)
        boolean orderExists = productOrderRepository.existsByPaymentId(request.getPaymentId());
        if (orderExists) {
            throw new IllegalStateException("Order already exists for payment ID: " + request.getPaymentId());
        }
        
        // Validate products and stock
        List<Product> products = new ArrayList<>();
        for (CreateOrderFromPaymentRequest.CartItemRequest itemRequest : request.getCartItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(itemRequest.getProductId()));
            if (!product.hasStock(itemRequest.getQuantity())) {
                throw new InsufficientStockException(
                    product.getId(),
                    product.getName(),
                    itemRequest.getQuantity(),
                    product.getStock()
                );
            }
            products.add(product);
        }
        
        // Create ProductOrder
        ProductOrder order = new ProductOrder();
        order.setUser(user);
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerAddress(request.getCustomerAddress());
        order.setSaleType(SaleType.ONLINE);
        order.setNotes(request.getNotes());
        order.setPaymentStatus(PaymentStatus.PAID); // Payment already succeeded
        order.setPaymentId(request.getPaymentId()); // Link to payment
        // deliveryStatus will be set to PENDING by @PrePersist for ONLINE orders
        
        // Create ProductOrderItem entities
        List<ProductOrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (int i = 0; i < request.getCartItems().size(); i++) {
            CreateOrderFromPaymentRequest.CartItemRequest itemRequest = request.getCartItems().get(i);
            Product product = products.get(i);
            
            ProductOrderItem orderItem = new ProductOrderItem();
            orderItem.setProductOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSubtotal(product.getPrice().multiply(new BigDecimal(itemRequest.getQuantity())));
            
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }
        
        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        
        // Save order (cascade will save items)
        ProductOrder savedOrder = productOrderRepository.save(order);
        
        // Deduct stock immediately since payment is already PAID
        deductStockForOrder(savedOrder);
        
        // Build and return response
        return buildProductOrderResponse(savedOrder);
    }

    @Transactional
    public ProductOrderResponse updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        ProductOrder order = productOrderRepository.findById(orderId)
            .orElseThrow(() -> new ProductOrderNotFoundException(orderId));
        
        PaymentStatus currentStatus = order.getPaymentStatus();
        PaymentStatus requestedStatus = paymentStatus;

        if (!isValidPaymentStatusTransition(currentStatus, requestedStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, requestedStatus);
        }
        if (currentStatus == PaymentStatus.PENDING && requestedStatus == PaymentStatus.PAID) {
            deductStockForOrder(order);
        } else if (currentStatus == PaymentStatus.PAID && requestedStatus == PaymentStatus.CANCELLED) {
            restoreStockForOrder(order);
        }
        order.setPaymentStatus(requestedStatus);

        ProductOrder savedOrder = productOrderRepository.save(order);
        return buildProductOrderResponse(savedOrder);
    }

    private boolean isValidPaymentStatusTransition(PaymentStatus current, PaymentStatus requested) {
        if (current == requested) {
            return false; // No transition needed
        }
        
        return switch (current) {
            case PENDING -> requested == PaymentStatus.PAID || requested == PaymentStatus.CANCELLED;
            case PAID -> requested == PaymentStatus.CANCELLED;
            case CANCELLED -> false; // Cannot transition from CANCELLED
        };
    }
    

    private void deductStockForOrder(ProductOrder order) {
        for (ProductOrderItem item : order.getItems()) {
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProduct().getId()));
            if (!product.hasStock(item.getQuantity())) {
                throw new InsufficientStockException(
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    product.getStock()
                );
            }

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
    }

    private void restoreStockForOrder(ProductOrder order) {
        for (ProductOrderItem item : order.getItems()) {
            // Use pessimistic locking for consistency
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProduct().getId()));
            
            // Restore stock
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }
    

    @Transactional
    public ProductOrderResponse updateDeliveryStatus(Long orderId, DeliveryStatus deliveryStatus) {
        ProductOrder order = productOrderRepository.findById(orderId)
            .orElseThrow(() -> new ProductOrderNotFoundException(orderId));
        if (order.getSaleType() == SaleType.COUNTER) {
            throw new InvalidDeliveryStatusTransitionException(
                order.getDeliveryStatus(),
                deliveryStatus
            );
        }
        
        DeliveryStatus currentStatus = order.getDeliveryStatus();
        DeliveryStatus requestedStatus = deliveryStatus;

        if (!isValidDeliveryStatusTransition(currentStatus, requestedStatus)) {
            throw new InvalidDeliveryStatusTransitionException(currentStatus, requestedStatus);
        }
        order.setDeliveryStatus(requestedStatus);
        // Save order
        ProductOrder savedOrder = productOrderRepository.save(order);
        return buildProductOrderResponse(savedOrder);
    }
    

    private boolean isValidDeliveryStatusTransition(DeliveryStatus current, DeliveryStatus requested) {
        if (current == requested) {
            return false;
        }
        
        if (requested == DeliveryStatus.CANCELLED) {
            return true;
        }

        if (current == DeliveryStatus.CANCELLED || current == DeliveryStatus.DELIVERED) {
            return false;
        }

        return switch (current) {
            case PENDING -> requested == DeliveryStatus.PROCESSING;
            case PROCESSING -> requested == DeliveryStatus.SHIPPED;
            case SHIPPED -> requested == DeliveryStatus.DELIVERED;
            default -> false;
        };
    }
    private ProductOrderResponse buildProductOrderResponse(ProductOrder order) {
        return ProductOrderResponse.builder()
            .id(order.getId())
            .userId(order.getUser() != null ? order.getUser().getId() : null)
            .customerName(order.getCustomerName())
            .customerPhone(order.getCustomerPhone())
            .customerAddress(order.getCustomerAddress())
            .saleType(order.getSaleType())
            .totalAmount(order.getTotalAmount())
            .paymentStatus(order.getPaymentStatus())
            .deliveryStatus(order.getDeliveryStatus())
            .notes(order.getNotes())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .itemCount(order.getItems().size())
            .build();
    }

    private ProductOrderDetailResponse buildProductOrderDetailResponse(ProductOrder order) {
        List<ProductOrderItemResponse> items = order.getItems().stream()
            .map(item -> ProductOrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build())
            .collect(Collectors.toList());
        
        return ProductOrderDetailResponse.builder()
            .id(order.getId())
            .userId(order.getUser() != null ? order.getUser().getId() : null)
            .customerName(order.getCustomerName())
            .customerPhone(order.getCustomerPhone())
            .customerAddress(order.getCustomerAddress())
            .saleType(order.getSaleType())
            .totalAmount(order.getTotalAmount())
            .paymentStatus(order.getPaymentStatus())
            .deliveryStatus(order.getDeliveryStatus())
            .notes(order.getNotes())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .items(items)
            .build();
    }
}
