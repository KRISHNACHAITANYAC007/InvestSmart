package com.example.investsmart.service;


import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.dto.OrderRequest;
import com.example.investsmart.entity.Holding;
import com.example.investsmart.entity.Order;
import com.example.investsmart.entity.OrderStatus;
import com.example.investsmart.entity.OrderType;
import com.example.investsmart.entity.Stock;
import com.example.investsmart.entity.User;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.HoldingRepository;
import com.example.investsmart.repository.OrderRepository;
import com.example.investsmart.repository.StockRepository;
import com.example.investsmart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import com.example.investsmart.dto.OrderResponse;
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final HoldingRepository holdingRepository;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            StockRepository stockRepository,
            HoldingRepository holdingRepository) {

        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.holdingRepository = holdingRepository;
    }

    @Transactional
    public Order buyStock(OrderRequest request) {

        User user = userRepository
                .findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        Stock stock = stockRepository
                .findById(request.getStockId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found"
                        )
                );

        Integer quantity = request.getQuantity();



        BigDecimal price = stock.getCurrentPrice();

        BigDecimal totalAmount =
                price.multiply(BigDecimal.valueOf(quantity));
        if (user.getVirtualBalance().compareTo(totalAmount) < 0) {
            throw new BadRequestException(
                    "Insufficient virtual balance"
            );
        }




        user.setVirtualBalance(
                user.getVirtualBalance().subtract(totalAmount)
        );

        Optional<Holding> existingHolding =
                holdingRepository.findByUserIdAndStockId(
                        user.getId(),
                        stock.getId()
                );

        Holding holding;

        if (existingHolding.isPresent()) {

            holding = existingHolding.get();

            holding.setQuantity(
                    holding.getQuantity() + quantity
            );

        } else {

            holding = new Holding();

            holding.setUser(user);
            holding.setStock(stock);
            holding.setQuantity(quantity);
        }

        holdingRepository.save(holding);

        Order order = new Order();

        order.setUser(user);
        order.setStock(stock);
        order.setOrderType(OrderType.BUY);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.COMPLETED);

        userRepository.save(user);

        return orderRepository.save(order);
    }

    @Transactional
    public Order sellStock(OrderRequest request) {

        User user = userRepository
                .findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        Stock stock = stockRepository
                .findById(request.getStockId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found"
                        )
                );

        Integer quantity = request.getQuantity();




        Optional<Holding> existingHolding =
                holdingRepository.findByUserIdAndStockId(
                        user.getId(),
                        stock.getId()
                );

        if (existingHolding.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Holding not found"
            );
        }

        Holding holding = existingHolding.get();


        if (holding.getQuantity() < quantity) {
            throw new BadRequestException(
                    "Insufficient holdings"
            );
        }


        BigDecimal price = stock.getCurrentPrice();


        BigDecimal totalAmount =
                price.multiply(BigDecimal.valueOf(quantity));


        holding.setQuantity(
                holding.getQuantity() - quantity
        );


        user.setVirtualBalance(
                user.getVirtualBalance().add(totalAmount)
        );


        holdingRepository.save(holding);

        Order order = new Order();

        order.setUser(user);
        order.setStock(stock);
        order.setOrderType(OrderType.SELL);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.COMPLETED);


        userRepository.save(user);


        return orderRepository.save(order);
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getUserOrdersByEmail(String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return getUserOrders(user.getId());
    }

    public List<OrderResponse> getUserOrderResponses(Long userId) {

        List<Order> orders =
                orderRepository.findByUserId(userId);

        List<OrderResponse> responses =
                new ArrayList<>();

        for (Order order : orders) {

            OrderResponse response =
                    new OrderResponse(
                            order.getId(),
                            order.getOrderType().name(),
                            order.getQuantity(),
                            order.getPrice(),
                            order.getTotalAmount(),
                            order.getStatus().name(),
                            order.getStock().getId(),
                            order.getStock().getSymbol(),
                            order.getStock().getCompanyName()
                    );

            responses.add(response);
        }

        return responses;
    }

    public List<OrderResponse> getUserOrderResponsesByEmail(
            String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return getUserOrderResponses(user.getId());
    }

}