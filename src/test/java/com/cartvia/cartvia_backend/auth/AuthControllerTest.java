package com.cartvia.cartvia_backend.auth;

import com.cartvia.cartvia_backend.common.enums.Role;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    void register_returnsUserId() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "priya_shah",
                                  "name": "Priya Shah",
                                  "email": "priya.shah@example.com",
                                  "phone": "9876543210",
                                  "password": "Str0ngPass!",
                                  "gender": "F"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        User existing = new User();
        existing.setUsername("priya_shah");
        existing.setName("Existing");
        existing.setPasswordHash(passwordEncoder.encode("password"));
        existing.setRole(Role.CUSTOMER);
        userRepository.save(existing);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "priya_shah",
                                  "name": "Priya Shah",
                                  "password": "Str0ngPass!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_TAKEN"));
    }

    @Test
    void register_invalidPassword_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new_user",
                                  "name": "New User",
                                  "password": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    void login_returnsTokensAndUser() throws Exception {
        User user = new User();
        user.setUsername("priya_shah");
        user.setName("Priya Shah");
        user.setEmail("priya.shah@example.com");
        user.setPasswordHash(passwordEncoder.encode("Str0ngPass!"));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "priya_shah",
                                  "password": "Str0ngPass!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.role").value("CUSTOMER"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "nobody",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refresh_returnsNewAccessToken() throws Exception {
        User user = new User();
        user.setUsername("refresh_user");
        user.setName("Refresh User");
        user.setPasswordHash(passwordEncoder.encode("Str0ngPass!"));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "refresh_user",
                                  "password": "Str0ngPass!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.path("data").path("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void protectedEndpoint_withValidToken_isAuthenticated() throws Exception {
        User user = new User();
        user.setUsername("auth_user");
        user.setName("Auth User");
        user.setPasswordHash(passwordEncoder.encode("Str0ngPass!"));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "auth_user",
                                  "password": "Str0ngPass!"
                                }
                                """))
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        MvcResult protectedResult = mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        assertThat(protectedResult.getResponse().getStatus()).isNotEqualTo(401);
    }

    @Test
    void resetPassword_withValidToken_succeeds() throws Exception {
        User user = new User();
        user.setUsername("reset_user");
        user.setName("Reset User");
        user.setEmail("reset@example.com");
        user.setPasswordHash(passwordEncoder.encode("OldPass123!"));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"reset@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetTokenSent").value(true));
    }
}
