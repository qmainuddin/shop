package com.shop.ordersvc.service;

import com.shop.ordersvc.cache.OrderCacheService;
import com.shop.ordersvc.config.RabbitConfig;
import com.shop.ordersvc.dto.OrderItemRequest;
import com.shop.ordersvc.dto.OrderResponse;
import com.shop.ordersvc.dto.PlaceOrderRequest;
import com.shop.ordersvc.event.OrderPlacedEvent;
import com.shop.ordersvc.model.Order;
import com.shop.ordersvc.model.OrderItem;
import com.shop.ordersvc.repository.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OrderCacheService orderCacheService;

    public OrderService(OrderRepository orderRepository,
                        RabbitTemplate rabbitTemplate,
                        @Autowired(required = false) OrderCacheService orderCacheService) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.orderCacheService = orderCacheService;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Order order = new Order();
        order.setUserId(request.userId());
        order.setStatus("PLACED");

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemReq : request.items()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemReq.productId());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            BigDecimal lineTotal = itemReq.unitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.quantity()));
            totalAmount = totalAmount.add(lineTotal);
            items.add(item);
        }
        order.setTotalAmount(totalAmount);
        order.setItems(items);

        Order saved = orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getTotalAmount(),
                saved.getPlacedAt()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.ORDERS_EXCHANGE, "order.placed", event);

        OrderResponse response = OrderResponse.from(saved);
        if (orderCacheService != null) {
            orderCacheService.put(saved.getId(), response);
            orderCacheService.evictUserOrders(saved.getUserId());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findById(UUID id) {
        if (orderCacheService != null) {
            Optional<OrderResponse> cached = orderCacheService.get(id);
            if (cached.isPresent()) {
                return cached;
            }
        }
        Optional<OrderResponse> result = orderRepository.findById(id).map(OrderResponse::from);
        if (orderCacheService != null) {
            result.ifPresent(r -> orderCacheService.put(id, r));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUserId(UUID userId) {
        if (orderCacheService != null) {
            Optional<List<OrderResponse>> cached = orderCacheService.getUserOrders(userId);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        List<OrderResponse> result = orderRepository.findByUserId(userId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
        if (orderCacheService != null) {
            orderCacheService.putUserOrders(userId, result);
        }
        return result;
    }
}
