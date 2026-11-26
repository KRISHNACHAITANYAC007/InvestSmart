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
    private final com.example.investsmart.security.SecurityAccounts accounts;
    private final com.example.investsmart.security.SecurityEvents events;

    public OrderController(OrderService orderService,com.example.investsmart.security.SecurityAccounts accounts,com.example.investsmart.security.SecurityEvents events) {
        this.accounts=accounts;
        this.events=events;
        this.orderService = orderService;
    }

    @PostMapping("/buy")
    public ResponseEntity<OrderResponse> buyStock(
            Authentication authentication,
            @Valid @RequestBody OrderRequest request,jakarta.servlet.http.HttpServletRequest httpRequest) {
        Order order;
        try { order=orderService.buyStock(authentication.getName(),request); }
        catch(RuntimeException rejected){recordRejectedOrder(authentication.getName(),httpRequest,rejected);throw rejected;}

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response(order));
    }

    @PostMapping("/sell")
    public ResponseEntity<OrderResponse> sellStock(
            Authentication authentication,
            @Valid @RequestBody OrderRequest request,jakarta.servlet.http.HttpServletRequest httpRequest) {
        Order order;
        try { order=orderService.sellStock(authentication.getName(),request); }
        catch(RuntimeException rejected){recordRejectedOrder(authentication.getName(),httpRequest,rejected);throw rejected;}

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @PathVariable Long userId,Authentication authentication,jakarta.servlet.http.HttpServletRequest request) {
        accounts.requireOwner(userId,authentication.getName(),request);

        List<OrderResponse> orders =
                orderService.getUserOrderResponses(userId);

        return ResponseEntity.ok(orders);
    }

    private OrderResponse response(Order o){var r=new OrderResponse(o.getId(),o.getOrderType().name(),o.getQuantity(),o.getPrice(),o.getTotalAmount(),o.getStatus().name(),o.getStock().getId(),o.getStock().getSymbol(),o.getStock().getCompanyName());r.setInstrumentKey(o.getStock().getInstrumentKey());r.setSource(o.getSource());return r;}
    private void recordRejectedOrder(String email,jakarta.servlet.http.HttpServletRequest request,RuntimeException failure){var user=accounts.user(email);events.record("ORDER_REJECTED",user.getId(),email,request,failure.getClass().getSimpleName()+": "+com.example.investsmart.security.SecurityEvents.bounded(failure.getMessage(),180));}

    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            Authentication authentication) {

        String email = authentication.getName();

        List<OrderResponse> orders =
                orderService.getUserOrderResponsesByEmail(email);

        return ResponseEntity.ok(orders);
    }
}
