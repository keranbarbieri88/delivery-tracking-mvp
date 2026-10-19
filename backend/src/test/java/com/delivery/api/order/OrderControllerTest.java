package com.delivery.api.order;

import com.delivery.api.auth.Role;
import com.delivery.api.auth.UserEntity;
import com.delivery.api.auth.UserRepository;
import com.delivery.api.order.dto.CreateOrderRequest;
import com.delivery.api.order.dto.OrderItemRequest;
import com.delivery.api.order.dto.UpdateOrderStatusRequest;
import com.delivery.api.shared.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String clienteBearerToken;
    private String lojistaBearerToken;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity cliente = userRepository.save(UserEntity.builder()
                .nome("Pedro Rocha")
                .email("pedro@delivery.com")
                .senha(passwordEncoder.encode("senha123"))
                .role(Role.CLIENTE)
                .build());

        UserEntity lojista = userRepository.save(UserEntity.builder()
                .nome("Restaurante Sabor")
                .email("lojista@delivery.com")
                .senha(passwordEncoder.encode("senha123"))
                .role(Role.LOJISTA)
                .build());

        clienteBearerToken = "Bearer " + jwtService.generateToken(cliente);
        lojistaBearerToken = "Bearer " + jwtService.generateToken(lojista);
    }

    @Test
    @DisplayName("POST, GET lista e GET por ID em /api/v1/pedidos devem funcionar com token JWT válido")
    void shouldCreateListAndGetOrderById() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                "Av. Brasil, 500 - Bloco B",
                List.of(
                        new OrderItemRequest("X-Burguer Duplo", 2, new BigDecimal("28.50")),
                        new OrderItemRequest("Batata Frita Media", 1, new BigDecimal("14.00"))
                )
        );

        // Act & Assert — Create (201 Created)
        MvcResult createResult = mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.total").value(71.00))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String orderId = createdJson.get("id").asText();

        // Act & Assert — List (200 OK para Cliente e Lojista)
        mockMvc.perform(get("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId));

        mockMvc.perform(get("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId));

        // Act & Assert — Get By ID (200 OK)
        mockMvc.perform(get("/api/v1/pedidos/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.enderecoEntrega").value("Av. Brasil, 500 - Bloco B"))
                .andExpect(jsonPath("$.total").value(71.00));
    }

    @Test
    @DisplayName("GET /api/v1/pedidos/{id} deve retornar 404 Not Found estruturado para ID inexistente")
    void shouldReturn404WhenOrderDoesNotExist() throws Exception {
        // Arrange
        UUID randomId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/v1/pedidos/{id}", randomId)
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/pedidos deve retornar 400 Bad Request quando lista de itens for vazia ou campos inválidos")
    void shouldReturn400WhenOrderItemsAreEmptyOrInvalid() throws Exception {
        // Arrange
        CreateOrderRequest emptyItemsRequest = new CreateOrderRequest("Av. Brasil, 500", List.of());
        CreateOrderRequest invalidItemsRequest = new CreateOrderRequest(
                "Av. Brasil, 500",
                List.of(new OrderItemRequest("", 0, new BigDecimal("0.00")))
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyItemsRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItemsRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/pedidos"))
                .andExpect(jsonPath("$.validationErrors").isNotEmpty());
    }

    @Test
    @DisplayName("PATCH /api/v1/pedidos/{id}/status deve respeitar permissões por Role e máquina de estados (RECEBIDO -> EM_PREPARO -> ENVIADO -> ENTREGUE)")
    void shouldEnforceRolePermissionsAndStateMachineTransitions() throws Exception {
        // Arrange
        CreateOrderRequest createRequest = new CreateOrderRequest(
                "Rua Sete de Setembro, 100",
                List.of(new OrderItemRequest("Esfiha Carne", 4, new BigDecimal("6.50")))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Act & Assert — Cliente tentando avançar para EM_PREPARO -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.EM_PREPARO))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Act & Assert — Lojista tentando pular RECEBIDO -> ENTREGUE -> 422 Unprocessable Entity
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.ENTREGUE))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // Act & Assert — Lojista avança RECEBIDO -> EM_PREPARO (200 OK)
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.EM_PREPARO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PREPARO"));

        // Act & Assert — Cliente tentando cancelar após entrar EM_PREPARO -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CANCELADO))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Act & Assert — Lojista avança EM_PREPARO -> ENVIADO (200 OK)
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.ENVIADO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENVIADO"));

        // Act & Assert — Lojista avança ENVIADO -> ENTREGUE (200 OK)
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.ENTREGUE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));

        // Act & Assert — Lojista tenta alterar pedido já ENTREGUE -> 422 Unprocessable Entity
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CANCELADO))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("PATCH /api/v1/pedidos/{id}/status deve permitir que o CLIENTE dono cancele pedido ainda em RECEBIDO")
    void shouldAllowClienteOwnerToCancelOrderWhileRecebido() throws Exception {
        // Arrange
        CreateOrderRequest createRequest = new CreateOrderRequest(
                "Rua das Palmeiras, 50",
                List.of(new OrderItemRequest("Pastel de Queijo", 2, new BigDecimal("12.00")))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CANCELADO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    @DisplayName("GET e PATCH /api/v1/pedidos/{id} devem retornar 403 Forbidden quando outro CLIENTE tentar acessar")
    void shouldReturn403WhenAnotherClienteAccessesOrUpdatesOrder() throws Exception {
        // Arrange
        CreateOrderRequest createRequest = new CreateOrderRequest(
                "Rua Privada, 10",
                List.of(new OrderItemRequest("Lanche Especial", 1, new BigDecimal("30.00")))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        UserEntity otherCliente = userRepository.save(UserEntity.builder()
                .nome("Outro Cliente")
                .email("outro@delivery.com")
                .senha(passwordEncoder.encode("senha123"))
                .role(Role.CLIENTE)
                .build());
        String otherBearerToken = "Bearer " + jwtService.generateToken(otherCliente);

        // Act & Assert
        mockMvc.perform(get("/api/v1/pedidos/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, otherBearerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, otherBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CANCELADO))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("PATCH /api/v1/pedidos/{id}/status deve retornar 400 Bad Request para enum inválido no JSON")
    void shouldReturn400WhenStatusEnumInJsonIsInvalid() throws Exception {
        // Arrange
        UUID randomId = UUID.randomUUID();
        String invalidJson = "{\"status\": \"STATUS_INEXISTENTE\"}";

        // Act & Assert
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", randomId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
