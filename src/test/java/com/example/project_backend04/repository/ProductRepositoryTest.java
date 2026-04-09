package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        // Create test products
        product1 = new Product();
        product1.setName("Whey Protein");
        product1.setDescription("High quality protein powder");
        product1.setPrice(new BigDecimal("50.00"));
        product1.setStock(100);
        product1.setLowStockThreshold(10);
        entityManager.persist(product1);

        product2 = new Product();
        product2.setName("Creatine Monohydrate");
        product2.setDescription("Pure creatine supplement");
        product2.setPrice(new BigDecimal("30.00"));
        product2.setStock(5);
        product2.setLowStockThreshold(10);
        entityManager.persist(product2);

        product3 = new Product();
        product3.setName("BCAA Energy");
        product3.setDescription("Branch chain amino acids");
        product3.setPrice(new BigDecimal("40.00"));
        product3.setStock(50);
        product3.setLowStockThreshold(10);
        entityManager.persist(product3);

        entityManager.flush();
    }

    @Test
    void testFindByNameContainingIgnoreCase() {
        // Test case-insensitive search
        List<Product> results = productRepository.findByNameContainingIgnoreCase("protein");
        assertEquals(1, results.size());
        assertEquals("Whey Protein", results.get(0).getName());

        // Test partial match
        results = productRepository.findByNameContainingIgnoreCase("CREATINE");
        assertEquals(1, results.size());
        assertEquals("Creatine Monohydrate", results.get(0).getName());

        // Test no match
        results = productRepository.findByNameContainingIgnoreCase("Vitamin");
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByStockGreaterThan() {
        // Find products with stock > 10
        List<Product> results = productRepository.findByStockGreaterThan(10);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getStock() > 10));

        // Find products with stock > 50
        results = productRepository.findByStockGreaterThan(50);
        assertEquals(1, results.size());
        assertEquals("Whey Protein", results.get(0).getName());
    }

    @Test
    void testFindByStockLessThan() {
        // Find products with stock < 10
        List<Product> results = productRepository.findByStockLessThan(10);
        assertEquals(1, results.size());
        assertEquals("Creatine Monohydrate", results.get(0).getName());

        // Find products with stock < 60
        results = productRepository.findByStockLessThan(60);
        assertEquals(2, results.size());
    }

    @Test
    void testFindByIdWithLock() {
        // Test pessimistic locking query
        Optional<Product> result = productRepository.findByIdWithLock(product1.getId());
        assertTrue(result.isPresent());
        assertEquals("Whey Protein", result.get().getName());

        // Test with non-existent ID
        result = productRepository.findByIdWithLock(999L);
        assertFalse(result.isPresent());
    }
}
