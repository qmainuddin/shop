package com.shop.ordersvc;

import com.shop.ordersvc.dto.OrderItemRequest;
import com.shop.ordersvc.dto.OrderResponse;
import com.shop.ordersvc.dto.PlaceOrderRequest;
import com.shop.ordersvc.event.OrderPlacedEvent;
import com.shop.ordersvc.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private PlaceOrderRequest buildRequest(UUID userId, List<OrderItemRequest> items) {
        return new PlaceOrderRequest(userId, items);
    }

    @Test
    void placeOrder_calculatesTotalAmount_correctly() {
        UUID userId = UUID.randomUUID();
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(UUID.randomUUID(), 2, new BigDecimal("49.99")),
                new OrderItemRequest(UUID.randomUUID(), 1, new BigDecimal("10.00"))
        );
        PlaceOrderRequest request = buildRequest(userId, items);

        OrderResponse response = orderService.placeOrder(request);

        // 2 * 49.99 + 1 * 10.00 = 99.98 + 10.00 = 109.98
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("109.98"));
        assertThat(response.getStatus()).isEqualTo("PLACED");
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void placeOrder_publishesOrderPlacedEvent() {
        UUID userId = UUID.randomUUID();
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(UUID.randomUUID(), 2, new BigDecimal("49.99"))
        );
        PlaceOrderRequest request = buildRequest(userId, items);

        orderService.placeOrder(request);

        verify(rabbitTemplate, times(1))
                .convertAndSend(eq("orders"), eq("order.placed"), any(OrderPlacedEvent.class));
    }

    @Test
    void findById_returnsEmpty_forUnknownId() {
        UUID unknownId = UUID.randomUUID();

        Optional<OrderResponse> result = orderService.findById(unknownId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserId_returnsCorrectOrders() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(UUID.randomUUID(), 1, new BigDecimal("25.00"))
        );
        orderService.placeOrder(buildRequest(userId, items));
        orderService.placeOrder(buildRequest(userId, items));
        orderService.placeOrder(buildRequest(otherUserId, items));

        List<OrderResponse> result = orderService.findByUserId(userId);

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result).allMatch(o -> o.getUserId().equals(userId));
    }
}
