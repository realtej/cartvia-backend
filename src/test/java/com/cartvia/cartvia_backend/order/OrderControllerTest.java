package com.cartvia.cartvia_backend.order;

import com.cartvia.cartvia_backend.common.enums.OrderStatus;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.entity.OrderItem;
import com.cartvia.cartvia_backend.order.repository.OrderItemRepository;
import com.cartvia.cartvia_backend.order.repository.OrderRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ownerToken;
    private String otherCustomerToken;
    private String staffToken;
    private String adminToken;
    private UUID orderId;

    @BeforeEach
    void setUp() throws Exception {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        storeRepository.deleteAll();

        Store store = new Store();
        store.setName("CartVia Mart");
        store.setTimezone("UTC");
        store = storeRepository.save(store);

        User owner = createUser("owner1", Role.CUSTOMER, null);
        ownerToken = loginToken(owner);
        otherCustomerToken = loginToken(createUser("other1", Role.CUSTOMER, null));
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

        Order order = new Order();
        order.setOrderCode("ORD-TEST-0001");
        order.setUser(owner);
        order.setStore(store);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(new BigDecimal("186.00"));
        order.setDiscountTotal(new BigDecimal("3.10"));
        order.setTaxTotal(new BigDecimal("12.40"));
        order.setGrandTotal(new BigDecimal("195.30"));
        order = orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setName(product.getName());
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("62.00"));
        item.setLineTotal(new BigDecimal("186.00"));
        orderItemRepository.save(item);

        orderId = order.getId();
    }

    @Test
    void owner_canGetOwnOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-TEST-0001"))
                .andExpect(jsonPath("$.data.grandTotal").value(195.30))
                .andExpect(jsonPath("$.data.items[0].name").value("Amul Toned Milk 1L"));
    }

    @Test
    void otherCustomer_cannotGetSomeoneElsesOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void storeStaff_cannotGetOrderDirectly() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canGetAnyOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()));
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void owner_getUnknownOrder_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
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
