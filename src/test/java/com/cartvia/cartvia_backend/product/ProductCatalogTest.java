package com.cartvia.cartvia_backend.product;

import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.inventory.entity.Inventory;
import com.cartvia.cartvia_backend.inventory.repository.InventoryRepository;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.product.repository.ProductRepository;
import com.cartvia.cartvia_backend.store.entity.Store;
import com.cartvia.cartvia_backend.store.repository.StoreRepository;
import com.cartvia.cartvia_backend.user.entity.User;
import com.cartvia.cartvia_backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCatalogTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID storeId;
    private String customerToken;
    private String staffToken;
    private String adminToken;
    private UUID productId;

    @BeforeEach
    void setUp() throws Exception {
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        storeRepository.deleteAll();

        Store store = new Store();
        store.setName("CartVia Mart");
        store.setTimezone("UTC");
        store = storeRepository.save(store);
        storeId = store.getId();

        customerToken = loginToken(createUser("customer1", Role.CUSTOMER, null));
        staffToken = loginToken(createUser("staff1", Role.STORE_STAFF, store));
        adminToken = loginToken(createUser("admin1", Role.ADMIN, null));

        Product product = new Product();
        product.setBarcode("8901234567890");
        product.setName("Amul Toned Milk 1L");
        product.setCategory("Dairy");
        product.setPrice(new BigDecimal("62.00"));
        product.setDiscountPct(BigDecimal.ZERO);
        product.setStore(store);
        product.setActive(true);
        product = productRepository.save(product);
        productId = product.getId();

        Inventory inventory = new Inventory();
        inventory.setStore(store);
        inventory.setProduct(product);
        inventory.setStockQty(50);
        inventory.setLowStockThreshold(10);
        inventoryRepository.save(inventory);
    }

    @Test
    void customer_canListProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("storeId", storeId.toString())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Amul Toned Milk 1L"))
                .andExpect(jsonPath("$.data.content[0].inStock").value(true));
    }

    @Test
    void customer_cannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "8901234500011",
                                  "name": "Bread",
                                  "price": 45.00,
                                  "storeId": "%s"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_canCreateProductForOwnStore() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "8901234500011",
                                  "name": "Britannia Brown Bread 400g",
                                  "category": "Bakery",
                                  "price": 45.00,
                                  "storeId": "%s"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Britannia Brown Bread 400g"));
    }

    @Test
    void staff_cannotCreateProductForOtherStore() throws Exception {
        Store otherStore = new Store();
        otherStore.setName("Other Store");
        otherStore.setTimezone("UTC");
        otherStore = storeRepository.save(otherStore);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "8901234500099",
                                  "name": "Other Store Item",
                                  "price": 10.00,
                                  "storeId": "%s"
                                }
                                """.formatted(otherStore.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_canAdjustInventory() throws Exception {
        mockMvc.perform(patch("/api/inventory/{productId}/adjust", productId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\": -3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQty").value(47));
    }

    @Test
    void admin_canBulkDeactivate() throws Exception {
        mockMvc.perform(post("/api/admin/products/bulk-deactivate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productIds\": [\"" + productId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.succeeded").value(1));

        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void staff_cannotBulkDeactivate() throws Exception {
        mockMvc.perform(post("/api/admin/products/bulk-deactivate")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productIds\": [\"" + productId + "\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProductByBarcode() throws Exception {
        mockMvc.perform(get("/api/products/barcode/8901234567890")
                        .param("storeId", storeId.toString())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(productId.toString()));
    }

    @Test
    void staff_canUpdateAndSoftDeleteProduct() throws Exception {
        mockMvc.perform(put("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "8901234567890",
                                  "name": "Amul Toned Milk 1L (Updated)",
                                  "price": 64.00,
                                  "storeId": "%s"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Amul Toned Milk 1L (Updated)"));

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());
    }

    private User createUser(String username, Role role, Store store) {
        User user = new User();
        user.setUsername(username);
        user.setName(username);
        user.setPasswordHash(passwordEncoder.encode("Str0ngPass!"));
        user.setRole(role);
        user.setStore(store);
        return userRepository.save(user);
    }

    private String loginToken(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "Str0ngPass!"}
                                """.formatted(user.getUsername())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }
}
