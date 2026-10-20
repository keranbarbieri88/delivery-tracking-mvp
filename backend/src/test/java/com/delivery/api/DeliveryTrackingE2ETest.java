package com.delivery.api;

import com.delivery.api.auth.Role;
import com.delivery.api.auth.UserRepository;
import com.delivery.api.auth.dto.LoginRequest;
import com.delivery.api.auth.dto.RegisterRequest;
import com.delivery.api.order.OrderRepository;
import com.delivery.api.order.OrderStatus;
import com.delivery.api.order.dto.CreateOrderRequest;
import com.delivery.api.order.dto.OrderItemRequest;
import com.delivery.api.order.dto.UpdateOrderStatusRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("E2E Integration — Fluxos Críticos do DeliveryTracker")
class DeliveryTrackingE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("E2E 1: Jornada completa de entrega (Cadastro Cliente/Lojista -> Login JWT -> Criação do Pedido -> Ciclo RECEBIDO até ENTREGUE)")
    void shouldExecuteCompleteDeliveryLifecycleEndToEnd() throws Exception {
        // Arrange — Dados de cadastro e login do Cliente e do Lojista
        RegisterRequest clienteRegister = new RegisterRequest(
                "Mariana Souza",
                "mariana.e2e@delivery.com",
                "senhaCliente123",
                Role.CLIENTE
        );
        RegisterRequest lojistaRegister = new RegisterRequest(
                "Pizzaria Napoli",
                "napoli.e2e@delivery.com",
                "senhaLojista123",
                Role.LOJISTA
        );

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                "Av. Paulista, 1578 - Bela Vista, São Paulo",
                List.of(
                        new OrderItemRequest("Pizza Margherita Artesanal", 2, new BigDecimal("54.90")),
                        new OrderItemRequest("Suco Natural de Laranja 500ml", 3, new BigDecimal("12.50"))
                )
        );
        // Subtotal item 1: 2 * 54.90 = 109.80
        // Subtotal item 2: 3 * 12.50 = 37.50
        // Total esperado: 147.30

        // Act & Assert — 1. Registrar Cliente e Lojista via endpoint público
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteRegister)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("mariana.e2e@delivery.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lojistaRegister)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("napoli.e2e@delivery.com"));

        // Act & Assert — 2. Autenticar Cliente e Lojista para obter tokens JWT reais
        String clienteBearer = authenticateAndExtractBearerToken("mariana.e2e@delivery.com", "senhaCliente123");
        String lojistaBearer = authenticateAndExtractBearerToken("napoli.e2e@delivery.com", "senhaLojista123");

        // Act & Assert — 3. Cliente cria um novo pedido (Status inicial RECEBIDO e cálculo server-side exato)
        MvcResult createResult = mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.total").value(147.30))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].subtotal").value(109.80))
                .andExpect(jsonPath("$.itens[1].subtotal").value(37.50))
                .andReturn();

        JsonNode createdOrder = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String orderId = createdOrder.get("id").asText();

        // Act & Assert — 4. Lojista avança o pedido: RECEBIDO -> EM_PREPARO -> SAIU_PARA_ENTREGA -> ENTREGUE
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.EM_PREPARO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PREPARO"));

        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.SAIU_PARA_ENTREGA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAIU_PARA_ENTREGA"));

        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.ENTREGUE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));

        // Act & Assert — 5. Cliente consulta os detalhes do pedido entregue
        mockMvc.perform(get("/api/v1/pedidos/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, clienteBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.total").value(147.30));
    }

    @Test
    @DisplayName("E2E 2: Isolamento multi-cliente e ordenação cronológica reversa (createdAt DESC) na listagem")
    void shouldIsolateOrdersBetweenClientsAndAllowLojistaToViewAllInReverseChronologicalOrder() throws Exception {
        // Arrange — Dois clientes distintos e um lojista
        String clienteABearer = registerAndLogin("Cliente A", "clientea.e2e@delivery.com", "senha123", Role.CLIENTE);
        String clienteBBearer = registerAndLogin("Cliente B", "clienteb.e2e@delivery.com", "senha123", Role.CLIENTE);
        String lojistaBearer = registerAndLogin("Lojista Central", "lojista.central@delivery.com", "senha123", Role.LOJISTA);

        CreateOrderRequest orderA = new CreateOrderRequest(
                "Rua A, 100",
                List.of(new OrderItemRequest("Combo Sushi 20 Peças", 1, new BigDecimal("68.00")))
        );
        CreateOrderRequest orderB = new CreateOrderRequest(
                "Rua B, 200",
                List.of(new OrderItemRequest("Temaki Salmão Completo", 2, new BigDecimal("32.50")))
        );

        // Act — Cliente A cria o primeiro pedido e Cliente B cria o segundo pedido
        MvcResult resultA = mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteABearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderA)))
                .andExpect(status().isCreated())
                .andReturn();
        String orderAId = objectMapper.readTree(resultA.getResponse().getContentAsString()).get("id").asText();

        MvcResult resultB = mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderB)))
                .andExpect(status().isCreated())
                .andReturn();
        String orderBId = objectMapper.readTree(resultB.getResponse().getContentAsString()).get("id").asText();

        // Assert — Cliente A vê apenas seu próprio pedido e recebe 403 ao tentar acessar o pedido de B
        mockMvc.perform(get("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteABearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderAId));

        mockMvc.perform(get("/api/v1/pedidos/{id}", orderBId)
                        .header(HttpHeaders.AUTHORIZATION, clienteABearer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Assert — Lojista vê ambos os pedidos ordenados pelo mais recente primeiro (orderB -> orderA)
        MvcResult lojistaListResult = mockMvc.perform(get("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        JsonNode lojistaList = objectMapper.readTree(lojistaListResult.getResponse().getContentAsString());
        assertThat(lojistaList.get(0).get("id").asText()).isEqualTo(orderBId);
        assertThat(lojistaList.get(1).get("id").asText()).isEqualTo(orderAId);
    }

    @Test
    @DisplayName("E2E 3: Regras de cancelamento e bloqueio de transições inválidas em estados terminais")
    void shouldEnforceCancellationRulesAndBlockTransitionsFromTerminalStates() throws Exception {
        // Arrange
        String clienteBearer = registerAndLogin("Carlos Lima", "carlos.e2e@delivery.com", "senha123", Role.CLIENTE);
        String lojistaBearer = registerAndLogin("Burger House", "burger.e2e@delivery.com", "senha123", Role.LOJISTA);

        CreateOrderRequest request = new CreateOrderRequest(
                "Alameda Santos, 900",
                List.of(new OrderItemRequest("Smash Burger Triplo", 1, new BigDecimal("42.90")))
        );

        MvcResult created = mockMvc.perform(post("/api/v1/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, clienteBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        // Act & Assert — Cliente cancela o próprio pedido ainda em RECEBIDO (Permitido -> 200 OK)
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, clienteBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CANCELADO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        // Act & Assert — Lojista tenta reabrir/avançar um pedido já CANCELADO (Bloqueado -> 422 Unprocessable Entity)
        mockMvc.perform(patch("/api/v1/pedidos/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, lojistaBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.EM_PREPARO))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.path").value("/api/v1/pedidos/" + orderId + "/status"));
    }

    private String registerAndLogin(String nome, String email, String senha, Role role) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(nome, email, senha, role);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        return authenticateAndExtractBearerToken(email, senha);
    }

    private String authenticateAndExtractBearerToken(String email, String senha) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, senha);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return "Bearer " + loginJson.get("token").asText();
    }
}

