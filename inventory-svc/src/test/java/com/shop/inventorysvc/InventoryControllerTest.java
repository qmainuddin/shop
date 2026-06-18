package com.shop.inventorysvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.inventorysvc.dto.AdjustRequest;
import com.shop.inventorysvc.dto.ReserveRequest;
import com.shop.inventorysvc.model.Inventory;
import com.shop.inventorysvc.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
    }

    // --- GET /inventory ---

    @Test
    void listAll_returns200_withEmptyArray_whenNoRows() throws Exception {
        mockMvc.perform(get("/inventory").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listAll_returns200_withRows() throws Exception {
        Inventory inv = new Inventory();
        inv.setProductId(1L);
        inv.setAvailableQuantity(50);
        inv.setReservedQuantity(5);
        inventoryRepository.save(inv);

        mockMvc.perform(get("/inventory").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].availableQuantity").value(50));
    }

    // --- GET /inventory/{productId} ---

    @Test
    void getByProductId_returns200_whenFound() throws Exception {
        Inventory inv = new Inventory();
        inv.setProductId(7L);
        inv.setAvailableQuantity(30);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);

        mockMvc.perform(get("/inventory/7").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(7))
                .andExpect(jsonPath("$.availableQuantity").value(30));
    }

    @Test
    void getByProductId_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/inventory/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("999")));
    }

    // --- POST /inventory/{productId}/adjust ---

    @Test
    void adjust_createsNewInventory_whenProductNotExist() throws Exception {
        String body = objectMapper.writeValueAsString(new AdjustRequest(100));

        mockMvc.perform(post("/inventory/10/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void adjust_updatesExisting_withPositiveDelta() throws Exception {
        Inventory inv = new Inventory();
        inv.setProductId(11L);
        inv.setAvailableQuantity(20);
        inventoryRepository.save(inv);

        String body = objectMapper.writeValueAsString(new AdjustRequest(10));

        mockMvc.perform(post("/inventory/11/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(30));
    }

    @Test
    void adjust_returns400_whenResultWouldGoNegative() throws Exception {
        Inventory inv = new Inventory();
        inv.setProductId(12L);
        inv.setAvailableQuantity(5);
        inventoryRepository.save(inv);

        String body = objectMapper.writeValueAsString(new AdjustRequest(-100));

        mockMvc.perform(post("/inventory/12/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- POST /inventory/{productId}/reserve ---

    @Test
    void reserve_returns200_andUpdatesQuantities() throws Exception {
        Inventory inv = new Inventory();
        inv.setProductId(20L);
        inv.setAvailableQuantity(50);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);

        String body = objectMapper.writeValueAsString(new ReserveRequest(15));

        mockMvc.perform(post("/inventory/20/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(35))
                .andExpect(jsonPath("$.reservedQuantity").value(15));
    }

    @Test
    void reserve_returns409_whenInsufficientStock() throws Exception {
        Inventory inv = new Inventory();
        inv.setProductId(21L);
        inv.setAvailableQuantity(5);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);

        String body = objectMapper.writeValueAsString(new ReserveRequest(100));

        mockMvc.perform(post("/inventory/21/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void reserve_returns404_whenProductNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(new ReserveRequest(10));

        mockMvc.perform(post("/inventory/888/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- POST /inventory/{productId}/release ---

    @Test
    void release_returns200_andUpdatesQuantities() throws Exception {
        Inventory inv = new Inventory();
        inv.setProductId(30L);
        inv.setAvailableQuantity(40);
        inv.setReservedQuantity(20);
        inventoryRepository.save(inv);

        String body = objectMapper.writeValueAsString(new ReserveRequest(10));

        mockMvc.perform(post("/inventory/30/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(50))
                .andExpect(jsonPath("$.reservedQuantity").value(10));
    }

    @Test
    void release_returns400_whenExceedsReserved() throws Exception {
        Inventory inv = new Inventory();
        inv.setProductId(31L);
        inv.setAvailableQuantity(10);
        inv.setReservedQuantity(3);
        inventoryRepository.save(inv);

        String body = objectMapper.writeValueAsString(new ReserveRequest(50));

        mockMvc.perform(post("/inventory/31/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void release_returns404_whenProductNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(new ReserveRequest(5));

        mockMvc.perform(post("/inventory/777/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- Actuator health ---

    @Test
    void actuatorHealth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
