package com.delivery.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.delivery.api.auth.dto.LoginRequest;
import com.delivery.api.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register deve retornar 201 Created e persistir senha com hash BCrypt")
    void shouldRegisterUserAndReturn201() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("Ana Costa", "ana@delivery.com", "minhaSenha123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Ana Costa"))
                .andExpect(jsonPath("$.email").value("ana@delivery.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        UserEntity saved = userRepository.findByEmailIgnoreCase("ana@delivery.com").orElseThrow();
        assertThat(saved.getSenha()).startsWith("$2a$");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register deve retornar 409 Conflict para e-mail duplicado")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("Ana Costa", "duplicado@delivery.com", "minhaSenha123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register deve retornar 400 Bad Request para payload inválido")
    void shouldReturn400WhenPayloadIsInvalid() throws Exception {
        // Arrange
        RegisterRequest invalidRequest = new RegisterRequest("", "email-invalido", "123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login deve retornar 200 OK com token Bearer para credenciais válidas")
    void shouldLoginAndReturn200WithJwtToken() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest("Bruno Dias", "bruno@delivery.com", "senhaForte123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("bruno@delivery.com", "senhaForte123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("bruno@delivery.com"))
                .andExpect(jsonPath("$.user.nome").value("Bruno Dias"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login deve retornar 401 Unauthorized estruturado para credenciais inválidas")
    void shouldReturn401WhenLoginCredentialsAreInvalid() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("inexistente@delivery.com", "senhaErrada");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Rotas protegidas devem retornar 401 Unauthorized quando o cabeçalho Authorization estiver ausente")
    void shouldReturn401OnProtectedRouteWithoutBearerToken() throws Exception {
        // Arrange / Act / Assert
        mockMvc.perform(get("/api/v1/pedidos"))
                .andExpect(status().isUnauthorized());
    }
}

