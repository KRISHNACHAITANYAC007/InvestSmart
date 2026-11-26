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

    private final org.springframework.jdbc.core.JdbcTemplate pendingOrders;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final HoldingRepository holdingRepository;
    private final NseMarketSessionService nseMarketSessionService;
    private final com.example.investsmart.market.MarketDataService marketDataService;
    private final com.example.investsmart.settlement.SettlementAccounting accounting;
    public OrderService(OrderRepository orders,UserRepository users,StockRepository stocks,HoldingRepository holdings,
            NseMarketSessionService sessions,com.example.investsmart.market.MarketDataService market,
            org.springframework.jdbc.core.JdbcTemplate db,com.example.investsmart.settlement.SettlementAccounting accounting) {
        this.orderRepository=orders;this.userRepository=users;this.stockRepository=stocks;this.holdingRepository=holdings;
        this.nseMarketSessionService=sessions;this.marketDataService=market;this.pendingOrders=db;this.accounting=accounting;
    }

    @Transactional
    public Order buyStock(String email, OrderRequest request) {return buyStock(email,request,null);}
    @Transactional public Order buyStock(String email, OrderRequest request, BigDecimal maximumPrice) {

        User user = userRepository
                .findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
        user.requireActive();
        accounting.prepare(user);

        Stock stock = stockRepository
                .findById(request.getStockId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found"
                        )
                );

        nseMarketSessionService.requireOpenSession(stock.getExchange());
        Integer quantity = validQuantity(request);



        BigDecimal price = executionPrice(stock);
        if(maximumPrice!=null&&price.compareTo(maximumPrice)>0)throw new BadRequestException("Price moved above the buy limit.");

        BigDecimal totalAmount =
                price.multiply(BigDecimal.valueOf(quantity));
        if (user.getVirtualBalance().compareTo(totalAmount) < 0) {
            throw new BadRequestException(
                    "Insufficient virtual balance"
            );
        }




        String reference = accounting.request();
        accounting.pay(user,reference,totalAmount,false);
        accounting.purchase(user,reference,stock.getId(),quantity,totalAmount,stock.getExchange());

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
    public Order sellStock(String email, OrderRequest request) {return sellStock(email,request,null);}
    @Transactional public Order sellStock(String email, OrderRequest request, BigDecimal minimumPrice) {

        User user = userRepository
                .findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
        user.requireActive();
        accounting.prepare(user);

        Stock stock = stockRepository
                .findById(request.getStockId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found"
                        )
                );

        nseMarketSessionService.requireOpenSession(stock.getExchange());
        Integer quantity = validQuantity(request);




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


        int reserved=pendingOrders==null?0:pendingOrders.queryForObject("SELECT COALESCE(SUM(units),0) FROM equity_pending_orders WHERE user_id=? AND stock_id=? AND side='SELL' AND status='PENDING'",Integer.class,user.getId(),stock.getId());
        if(pendingOrders!=null)reserved+=pendingOrders.queryForObject("SELECT COALESCE(SUM(reserved_units),0) FROM paper_tickets WHERE user_id=? AND stock_id=? AND position_id IS NULL AND status IN ('PENDING','TRIGGERED','PART_FILLED')",Integer.class,user.getId(),stock.getId());
        if (holding.getQuantity()-reserved < quantity) {
            throw new BadRequestException(
                    "Insufficient holdings"
            );
        }


        BigDecimal price = executionPrice(stock);
        if(minimumPrice!=null&&price.compareTo(minimumPrice)<0)throw new BadRequestException("Price moved below the sell limit.");


        BigDecimal totalAmount =
                price.multiply(BigDecimal.valueOf(quantity));


        holding.setQuantity(
                holding.getQuantity() - quantity
        );


        accounting.sell(user,accounting.request(),stock.getId(),quantity,price,stock.getExchange());


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

    private int validQuantity(OrderRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 1)
            throw new BadRequestException("Quantity must be at least 1.");
        return request.getQuantity();
    }

    private BigDecimal executionPrice(Stock stock) {
        try {
            com.example.investsmart.market.MarketQuote quote;
            if (stock.getInstrumentKey() == null) {
                if (!"NSE".equals(stock.getExchange())) throw new IllegalStateException("Missing listing identifier");
                quote = marketDataService.getQuote(stock.getSymbol());
            } else {
                quote = marketDataService.getQuote(new com.example.investsmart.market.MarketInstrument(
                        stock.getInstrumentKey(), stock.getSymbol(), stock.getCompanyName(), stock.getExchange(), stock.getIsin()));
                if (quote != null && !stock.getInstrumentKey().equals(quote.getInstrumentKey()))
                    throw new IllegalStateException("Quote does not match selected listing");
            }
            if (quote == null || quote.getCurrentPrice() == null || quote.getCurrentPrice().signum() <= 0)
                throw new IllegalStateException("Listing price unavailable");
            return quote.getCurrentPrice();
        } catch (RuntimeException failure) {
            throw new com.example.investsmart.exception.MarketStatusUnavailableException(
                    "Unable to confirm the selected listing's price. Please retry.", failure);
        }
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

    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

            response.setInstrumentKey(order.getStock().getInstrumentKey());
            response.setSource(order.getSource());
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
