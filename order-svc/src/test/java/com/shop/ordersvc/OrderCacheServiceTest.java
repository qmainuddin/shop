package com.shop.ordersvc;

import com.shop.ordersvc.cache.OrderCacheService;
import com.shop.ordersvc.dto.OrderItemResponse;
import com.shop.ordersvc.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderCacheServiceTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;
    private OrderCacheService cacheService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheService = new OrderCacheService(redisTemplate);
    }

    private OrderResponse buildOrderResponse(UUID id, UUID userId) {
        OrderResponse r = new OrderResponse();
        r.setId(id);
        r.setUserId(userId);
        r.setStatus("PLACED");
        r.setTotalAmount(new BigDecimal("49.99"));
        r.setPlacedAt(Instant.now());
        r.setItems(List.of());
        return r;
    }

    @Test
    void put_storesOrderWithCorrectKeyAndTtl() {
        UUID orderId = UUID.randomUUID();
        OrderResponse order = buildOrderResponse(orderId, UUID.randomUUID());

        cacheService.put(orderId, order);

        verify(valueOps).set(
                eq("order:" + orderId),
                eq(order),
                eq(OrderCacheService.TTL)
        );
    }

    @Test
    void get_returnsPresent_whenCacheHit() {
        UUID orderId = UUID.randomUUID();
        OrderResponse order = buildOrderResponse(orderId, UUID.randomUUID());
        when(valueOps.get("order:" + orderId)).thenReturn(order);

        Optional<OrderResponse> result = cacheService.get(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(orderId);
    }

    @Test
    void get_returnsEmpty_onCacheMiss() {
        UUID orderId = UUID.randomUUID();
        when(valueOps.get("order:" + orderId)).thenReturn(null);

        Optional<OrderResponse> result = cacheService.get(orderId);

        assertThat(result).isEmpty();
    }

    @Test
    void evict_deletesOrderKey() {
        UUID orderId = UUID.randomUUID();

        cacheService.evict(orderId);

        verify(redisTemplate).delete("order:" + orderId);
    }

    @Test
    void putUserOrders_storesListWithCorrectKeyAndTtl() {
        UUID userId = UUID.randomUUID();
        List<OrderResponse> orders = List.of(buildOrderResponse(UUID.randomUUID(), userId));

        cacheService.putUserOrders(userId, orders);

        verify(valueOps).set(
                eq("user_orders:" + userId),
                eq(orders),
                eq(OrderCacheService.TTL)
        );
    }

    @Test
    void getUserOrders_returnsPresent_whenCacheHit() {
        UUID userId = UUID.randomUUID();
        List<OrderResponse> orders = List.of(buildOrderResponse(UUID.randomUUID(), userId));
        when(valueOps.get("user_orders:" + userId)).thenReturn(orders);

        Optional<List<OrderResponse>> result = cacheService.getUserOrders(userId);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
    }

    @Test
    void getUserOrders_returnsEmpty_onCacheMiss() {
        UUID userId = UUID.randomUUID();
        when(valueOps.get("user_orders:" + userId)).thenReturn(null);

        Optional<List<OrderResponse>> result = cacheService.getUserOrders(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void evictUserOrders_deletesUserKey() {
        UUID userId = UUID.randomUUID();

        cacheService.evictUserOrders(userId);

        verify(redisTemplate).delete("user_orders:" + userId);
    }
}
