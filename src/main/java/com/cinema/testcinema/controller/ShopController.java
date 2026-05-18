package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.CreateOrderRequest;
import com.cinema.testcinema.dto.CreateProductRequest;
import com.cinema.testcinema.model.Order;
import com.cinema.testcinema.model.Product;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.OrderRepository;
import com.cinema.testcinema.repository.ProductRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/shop")
public class ShopController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public ShopController(ProductRepository productRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          AuthenticatedUserService authenticatedUserService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest req) {
        Product product = new Product();
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setImageUrl(req.getImageUrl());
        product.setStock(req.getStock() != null ? req.getStock() : 0);
        return ResponseEntity.status(HttpStatus.CREATED).body(productRepository.save(product));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest req,
                                             Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден"));

        if (product.getStock() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Товар закончился");
        }

        product.setStock(product.getStock() - 1);
        productRepository.save(product);

        Order order = new Order();
        order.setUser(user);
        order.setProduct(product);
        order.setStatus("PENDING");

        return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
    }

    @GetMapping("/orders/my")
    public ResponseEntity<List<Order>> myOrders(Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        return ResponseEntity.ok(orderRepository.findByUserId(userId));
    }
}
