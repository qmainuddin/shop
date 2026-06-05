package com.shop.ordersvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.ordersvc.dto.OrderItemRequest;
import com.shop.ordersvc.dto.PlaceOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private PlaceOrderRequest buildRequest(UUID userId) {
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(UUID.randomUUID(), 2, new BigDecimal("49.99"))
        );
        return new PlaceOrderRequest(userId, items);
    }

    @Test
    void placeOrder_returns201_withOrderId() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(buildRequest(userId));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    void getOrder_returns200_whenExists() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(buildRequest(userId));

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String id = objectMapper.readTree(responseJson).get("id").asText();

        mockMvc.perform(get("/api/orders/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getOrder_returns404_whenNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/orders/{id}", randomId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void listOrdersByUser_returns200_withArray() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(buildRequest(userId));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void health_returns200_withStatusUp() throws Exception {
        mockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
