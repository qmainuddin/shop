package com.shop.ordersvc.cache;

import com.shop.ordersvc.dto.OrderResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis wrapper for order-svc. Provides explicit cache-aside operations for
 * individual orders and per-user order lists, using a 10-minute TTL.
 * Only active when a RedisTemplate bean is available (absent in test profile).
 */
@Service
@ConditionalOnBean(RedisTemplate.class)
public class OrderCacheService {

    public static final String ORDER_KEY_PREFIX = "order:";
    public static final String USER_ORDERS_KEY_PREFIX = "user_orders:";
    public static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    public OrderCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void put(UUID orderId, OrderResponse order) {
        redisTemplate.opsForValue().set(ORDER_KEY_PREFIX + orderId, order, TTL);
    }

    public Optional<OrderResponse> get(UUID orderId) {
        Object value = redisTemplate.opsForValue().get(ORDER_KEY_PREFIX + orderId);
        return Optional.ofNullable((OrderResponse) value);
    }

    public void evict(UUID orderId) {
        redisTemplate.delete(ORDER_KEY_PREFIX + orderId);
    }

    public void putUserOrders(UUID userId, List<OrderResponse> orders) {
        redisTemplate.opsForValue().set(USER_ORDERS_KEY_PREFIX + userId, orders, TTL);
    }

    @SuppressWarnings("unchecked")
    public Optional<List<OrderResponse>> getUserOrders(UUID userId) {
        Object value = redisTemplate.opsForValue().get(USER_ORDERS_KEY_PREFIX + userId);
        return Optional.ofNullable((List<OrderResponse>) value);
    }

    public void evictUserOrders(UUID userId) {
        redisTemplate.delete(USER_ORDERS_KEY_PREFIX + userId);
    }
}
