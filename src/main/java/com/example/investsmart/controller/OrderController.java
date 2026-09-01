package com.example.investsmart.controller;

import com.example.investsmart.dto.OrderRequest;
import com.example.investsmart.dto.OrderResponse;
import com.example.investsmart.entity.Order;
import com.example.investsmart.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/buy")
    public ResponseEntity<Order> buyStock(
            @Valid @RequestBody OrderRequest request) {

        Order order = orderService.buyStock(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }

    @PostMapping("/sell")
    public ResponseEntity<Order> sellStock(
            @Valid @RequestBody OrderRequest request) {

        Order order = orderService.sellStock(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(
            @PathVariable Long userId) {

        List<Order> orders =
                orderService.getUserOrders(userId);

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            Authentication authentication) {

        String email = authentication.getName();

        List<OrderResponse> orders =
                orderService.getUserOrderResponsesByEmail(email);

        return ResponseEntity.ok(orders);
    }
}