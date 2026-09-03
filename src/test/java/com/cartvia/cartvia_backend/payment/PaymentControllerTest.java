package com.cartvia.cartvia_backend.payment;

import com.cartvia.cartvia_backend.common.enums.OrderStatus;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.repository.OrderRepository;
import com.cartvia.cartvia_backend.payment.repository.PaymentRepository;
import com.cartvia.cartvia_backend.payment.repository.WebhookEventRepository;
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
class PaymentControllerTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ownerToken;
    private String otherCustomerToken;
    private String staffToken;
    private UUID orderId;

    @BeforeEach
    void setUp() throws Exception {
        webhookEventRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        storeRepository.deleteAll();

        Store store = new Store();
        store.setName("CartVia Mart");
        store.setTimezone("UTC");
        store = storeRepository.save(store);

        User owner = createUser("payowner1", Role.CUSTOMER, null);
        ownerToken = loginToken(owner);
        otherCustomerToken = loginToken(createUser("payother1", Role.CUSTOMER, null));
        staffToken = loginToken(createUser("paystaff1", Role.STORE_STAFF, store));

        Product product = new Product();
        product.setBarcode("8901234500055");
        product.setName("Amul Toned Milk 1L");
        product.setCategory("Dairy");
        product.setPrice(new BigDecimal("62.00"));
        product.setDiscountPct(BigDecimal.ZERO);
        product.setStore(store);
        product.setActive(true);
        productRepository.save(product);

        Order order = new Order();
        order.setOrderCode("ORD-TEST-PAY-0001");
        order.setUser(owner);
        order.setStore(store);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(new BigDecimal("186.00"));
        order.setDiscountTotal(new BigDecimal("3.10"));
        order.setTaxTotal(new BigDecimal("12.40"));
        order.setGrandTotal(new BigDecimal("195.30"));
        order = orderRepository.save(order);
        orderId = order.getId();
    }

    @Test
    void owner_canInitiatePayment() throws Exception {
        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"%s\"}".formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("INITIATED"))
                .andExpect(jsonPath("$.data.amount").value(195.30))
                .andExpect(jsonPath("$.data.upiDeepLink").isNotEmpty())
                .andExpect(jsonPath("$.data.qrPayload").isNotEmpty());
    }

    @Test
    void otherCustomer_cannotInitiatePaymentForSomeoneElsesOrder() throws Exception {
        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + otherCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"%s\"}".formatted(orderId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_cannotInitiatePayment() throws Exception {
        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"%s\"}".formatted(orderId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void webhook_withValidSignature_marksOrderPaidAndIsIdempotent() throws Exception {
        initiate(ownerToken);

        String signature = "v1=" + validHmac("SUCCESS", "UPI9988");
        String body = """
                {"orderId": "%s", "status": "SUCCESS", "signature": "%s", "upiTxnRef": "UPI9988"}
                """.formatted(orderId, signature);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/payments/{orderId}/status", orderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        // Replay of the same event must not fail or double-apply.
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_withInvalidSignature_isRejected() throws Exception {
        initiate(ownerToken);

        String body = """
                {"orderId": "%s", "status": "SUCCESS", "signature": "v1=deadbeef", "upiTxnRef": "UPI1"}
                """.formatted(orderId);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void owner_getStatus_beforePaymentInitiated_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/payments/{orderId}/status", orderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void staff_cannotGetPaymentStatus() throws Exception {
        initiate(ownerToken);
        mockMvc.perform(get("/api/payments/{orderId}/status", orderId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    private void initiate(String token) throws Exception {
        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"%s\"}".formatted(orderId)))
                .andExpect(status().isOk());
    }

    /**
     * Computes the same HMAC-SHA256 hex digest that
     * {@link PaymentSignatureService} expects, over the canonical
     * "orderId|status|upiTxnRef" payload, keyed with the test webhook
     * secret configured in application-test.properties.
     */
    private String validHmac(String status, String upiTxnRef) {
        try {
            String payload = orderId + "|" + status + "|" + upiTxnRef;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    "test-webhook-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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
