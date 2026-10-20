package com.delivery.api.order;

import com.delivery.api.auth.Role;
import com.delivery.api.auth.UserEntity;
import com.delivery.api.auth.UserRepository;
import com.delivery.api.order.dto.CreateOrderRequest;
import com.delivery.api.order.dto.OrderItemRequest;
import com.delivery.api.order.dto.OrderResponse;
import com.delivery.api.order.dto.UpdateOrderStatusRequest;
import com.delivery.api.shared.exception.InvalidStatusTransitionException;
import com.delivery.api.shared.exception.OrderNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Deve criar pedido em cascata e calcular corretamente subtotais dos itens e valor total")
    void shouldCreateOrderAndCalculateSubtotalsAndTotalCorrectly() {
        // Arrange
        String email = "cliente@delivery.com";
        UserEntity cliente = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Mendes")
                .email(email)
                .senha("$2a$10$hash")
                .role(Role.CLIENTE)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "Rua das Acácias, 450 - Sala 2",
                List.of(
                        new OrderItemRequest("Combo Sushi 20 peças", 2, new BigDecimal("49.90")),
                        new OrderItemRequest("Temaki Salmão", 1, new BigDecimal("24.50"))
                )
        );

        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(cliente));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            order.setCreatedAt(now);
            order.getItens().forEach(item -> item.setId(UUID.randomUUID()));
            return order;
        });

        // Act
        OrderResponse response = orderService.createOrder(request, email);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order persistedOrder = orderCaptor.getValue();
        assertThat(persistedOrder.getCliente()).isEqualTo(cliente);
        assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(persistedOrder.getItens()).hasSize(2);
        assertThat(persistedOrder.getItens()).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(persistedOrder));

        assertThat(response.id()).isEqualTo(orderId);
        assertThat(response.status()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(response.itens()).hasSize(2);
        assertThat(response.itens().get(0).subtotal()).isEqualByComparingTo(new BigDecimal("99.80"));
        assertThat(response.itens().get(1).subtotal()).isEqualByComparingTo(new BigDecimal("24.50"));
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("124.30"));
    }

    @Test
    @DisplayName("Deve listar apenas os pedidos do próprio cliente quando o perfil for CLIENTE")
    void shouldListOnlyOwnOrdersWhenUserIsCliente() {
        // Arrange
        String email = "cliente@delivery.com";
        UserEntity cliente = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Mendes")
                .email(email)
                .role(Role.CLIENTE)
                .build();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .cliente(cliente)
                .enderecoEntrega("Rua A, 10")
                .status(OrderStatus.EM_PREPARO)
                .createdAt(Instant.now())
                .build();
        order.addItem(OrderItem.builder()
                .id(UUID.randomUUID())
                .nome("Açaí 500ml")
                .quantidade(2)
                .precoUnitario(new BigDecimal("22.00"))
                .build());

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(cliente));
        when(orderRepository.findByClienteOrderByCreatedAtDesc(cliente)).thenReturn(List.of(order));

        // Act
        List<OrderResponse> responses = orderService.listOrders(email);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(order.getId());
        assertThat(responses.get(0).total()).isEqualByComparingTo(new BigDecimal("44.00"));
        verify(orderRepository).findByClienteOrderByCreatedAtDesc(cliente);
        verify(orderRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Deve listar todos os pedidos do sistema quando o perfil for LOJISTA ou ADMIN")
    void shouldListAllOrdersWhenUserIsStaff() {
        // Arrange
        String email = "lojista@delivery.com";
        UserEntity lojista = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Restaurante Central")
                .email(email)
                .role(Role.LOJISTA)
                .build();

        UserEntity cliente = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Cliente 1")
                .email("c1@delivery.com")
                .role(Role.CLIENTE)
                .build();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .cliente(cliente)
                .enderecoEntrega("Rua B, 20")
                .status(OrderStatus.RECEBIDO)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(lojista));
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        // Act
        List<OrderResponse> responses = orderService.listOrders(email);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(order.getId());
        verify(orderRepository).findAllByOrderByCreatedAtDesc();
        verify(orderRepository, never()).findByClienteOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Deve retornar detalhes do pedido por ID quando pertencer ao cliente autenticado ou quando for Staff")
    void shouldGetOrderByIdForOwnerOrStaff() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UserEntity dono = UserEntity.builder()
                .id(ownerId)
                .nome("Lucas Mendes")
                .email("cliente@delivery.com")
                .role(Role.CLIENTE)
                .build();
        UserEntity admin = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Admin")
                .email("admin@delivery.com")
                .role(Role.ADMIN)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .cliente(dono)
                .enderecoEntrega("Rua B, 200")
                .status(OrderStatus.RECEBIDO)
                .createdAt(Instant.now())
                .build();
        order.addItem(OrderItem.builder()
                .id(UUID.randomUUID())
                .nome("Pizza Calabresa")
                .quantidade(1)
                .precoUnitario(new BigDecimal("55.00"))
                .build());

        when(userRepository.findByEmailIgnoreCase("cliente@delivery.com")).thenReturn(Optional.of(dono));
        when(userRepository.findByEmailIgnoreCase("admin@delivery.com")).thenReturn(Optional.of(admin));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        OrderResponse ownerResponse = orderService.getOrderById(orderId, "cliente@delivery.com");
        OrderResponse adminResponse = orderService.getOrderById(orderId, "admin@delivery.com");

        // Assert
        assertThat(ownerResponse.id()).isEqualTo(orderId);
        assertThat(ownerResponse.total()).isEqualByComparingTo(new BigDecimal("55.00"));
        assertThat(adminResponse.id()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Deve lançar OrderNotFoundException ao buscar ou atualizar ID de pedido inexistente")
    void shouldThrowOrderNotFoundExceptionWhenIdDoesNotExist() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        UserEntity cliente = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("cliente@delivery.com")
                .role(Role.CLIENTE)
                .build();

        when(userRepository.findByEmailIgnoreCase("cliente@delivery.com")).thenReturn(Optional.of(cliente));
        when(orderRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(nonExistentId, "cliente@delivery.com"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());

        assertThatThrownBy(() -> orderService.updateOrderStatus(
                nonExistentId,
                new UpdateOrderStatusRequest(OrderStatus.CANCELADO),
                "cliente@delivery.com"
        ))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException quando um CLIENTE tentar consultar ou alterar pedido de outro cliente")
    void shouldThrowAccessDeniedExceptionWhenClienteAccessesAnotherUsersOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UserEntity dono = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Dono Real")
                .email("dono@delivery.com")
                .role(Role.CLIENTE)
                .build();
        UserEntity intruso = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Intruso")
                .email("outro@delivery.com")
                .role(Role.CLIENTE)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .cliente(dono)
                .enderecoEntrega("Rua Secreta, 99")
                .status(OrderStatus.RECEBIDO)
                .build();

        when(userRepository.findByEmailIgnoreCase("outro@delivery.com")).thenReturn(Optional.of(intruso));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(orderId, "outro@delivery.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não pertence ao usuário autenticado");

        assertThatThrownBy(() -> orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.CANCELADO),
                "outro@delivery.com"
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não autorizado");
    }

    @Test
    @DisplayName("Deve permitir que o CLIENTE dono cancele seu pedido enquanto estiver RECEBIDO")
    void shouldAllowClienteOwnerToCancelOrderWhenStatusIsRecebido() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UserEntity cliente = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Mendes")
                .email("cliente@delivery.com")
                .role(Role.CLIENTE)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .cliente(cliente)
                .enderecoEntrega("Rua C, 300")
                .status(OrderStatus.RECEBIDO)
                .build();

        when(userRepository.findByEmailIgnoreCase("cliente@delivery.com")).thenReturn(Optional.of(cliente));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        OrderResponse response = orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.CANCELADO),
                "cliente@delivery.com"
        );

        // Assert
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELADO);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException quando CLIENTE dono tentar avançar status diferente de CANCELADO ou cancelar após RECEBIDO")
    void shouldDenyClienteOwnerAdvancingStatusOrCancelingAfterPreparationStarted() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UserEntity cliente = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Mendes")
                .email("cliente@delivery.com")
                .role(Role.CLIENTE)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .cliente(cliente)
                .enderecoEntrega("Rua C, 300")
                .status(OrderStatus.RECEBIDO)
                .build();

        when(userRepository.findByEmailIgnoreCase("cliente@delivery.com")).thenReturn(Optional.of(cliente));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert — Cliente tentando mudar para EM_PREPARO
        assertThatThrownBy(() -> orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.EM_PREPARO),
                "cliente@delivery.com"
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Clientes só possuem permissão para cancelar pedidos");

        // Act & Assert — Pedido já em preparo não pode ser cancelado pelo cliente
        order.setStatus(OrderStatus.EM_PREPARO);
        assertThatThrownBy(() -> orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.CANCELADO),
                "cliente@delivery.com"
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não pode mais ser cancelado pelo cliente");
    }

    @Test
    @DisplayName("Deve permitir que LOJISTA/ADMIN avance o status respeitando a máquina de estados e rejeite transições inválidas")
    void shouldAllowStaffToAdvanceStatusAndRejectInvalidStateMachineTransitions() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UserEntity cliente = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Lucas Mendes")
                .email("cliente@delivery.com")
                .role(Role.CLIENTE)
                .build();
        UserEntity lojista = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Lojista")
                .email("lojista@delivery.com")
                .role(Role.LOJISTA)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .cliente(cliente)
                .enderecoEntrega("Rua D, 400")
                .status(OrderStatus.RECEBIDO)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByEmailIgnoreCase("lojista@delivery.com")).thenReturn(Optional.of(lojista));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert — Salto inválido RECEBIDO -> ENTREGUE deve lançar InvalidStatusTransitionException
        assertThatThrownBy(() -> orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.ENTREGUE),
                "lojista@delivery.com"
        )).isInstanceOf(InvalidStatusTransitionException.class);

        // Act & Assert — Transições válidas passo a passo: RECEBIDO -> EM_PREPARO -> SAIU_PARA_ENTREGA -> ENTREGUE
        assertThat(orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.EM_PREPARO),
                "lojista@delivery.com"
        ).status()).isEqualTo(OrderStatus.EM_PREPARO);

        assertThat(orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.SAIU_PARA_ENTREGA),
                "lojista@delivery.com"
        ).status()).isEqualTo(OrderStatus.SAIU_PARA_ENTREGA);

        assertThat(orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.ENTREGUE),
                "lojista@delivery.com"
        ).status()).isEqualTo(OrderStatus.ENTREGUE);

        // Act & Assert — Pedido já ENTREGUE não pode transicionar
        assertThatThrownBy(() -> orderService.updateOrderStatus(
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.CANCELADO),
                "lojista@delivery.com"
        )).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando e-mail autenticado não existir no banco")
    void shouldThrowEntityNotFoundExceptionWhenAuthenticatedUserDoesNotExist() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("inexistente@delivery.com")).thenReturn(Optional.empty());
        CreateOrderRequest request = new CreateOrderRequest(
                "Rua X, 1",
                List.of(new OrderItemRequest("Item", 1, BigDecimal.TEN))
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(request, "inexistente@delivery.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("inexistente@delivery.com");

        assertThatThrownBy(() -> orderService.listOrders("inexistente@delivery.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("inexistente@delivery.com");
    }
}
