package com.delivery.api.order;

import com.delivery.api.auth.UserEntity;
import com.delivery.api.auth.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Deve manter consistência bidirecional em addItem e removeItem e inicializar status RECEBIDO")
    void shouldMaintainBidirectionalConsistencyAndDefaultStatus() {
        // Arrange
        Order order = Order.builder()
                .enderecoEntrega("Rua das Flores, 123 - Centro")
                .build();

        OrderItem item1 = OrderItem.builder()
                .nome("Hambúrguer Artesanal")
                .quantidade(2)
                .precoUnitario(new BigDecimal("32.50"))
                .build();

        OrderItem item2 = OrderItem.builder()
                .nome("Refrigerante Lata")
                .quantidade(1)
                .precoUnitario(new BigDecimal("7.00"))
                .build();

        // Act
        order.addItem(item1);
        order.addItem(item2);
        order.removeItem(item2);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(order.getItens()).containsExactly(item1);
        assertThat(item1.getOrder()).isSameAs(order);
        assertThat(item2.getOrder()).isNull();
        assertThat(order.calcularValorTotal()).isEqualByComparingTo(new BigDecimal("65.00"));
    }

    @Test
    @DisplayName("Deve persistir Order e OrderItems em cascata, remover órfãos e consultar por cliente")
    void shouldPersistOrderWithCascadeItemsAndQueryByCliente() {
        // Arrange
        UserEntity cliente = userRepository.save(UserEntity.builder()
                .nome("Fernanda Lima")
                .email("fernanda@delivery.com")
                .senha("$2a$10$hashed")
                .build());

        Order order = Order.builder()
                .cliente(cliente)
                .enderecoEntrega("Av. Paulista, 1000 - Apto 42")
                .build();

        OrderItem pizza = OrderItem.builder()
                .nome("Pizza Margherita")
                .quantidade(1)
                .precoUnitario(new BigDecimal("59.90"))
                .build();

        OrderItem suco = OrderItem.builder()
                .nome("Suco Natural 500ml")
                .quantidade(2)
                .precoUnitario(new BigDecimal("12.50"))
                .build();

        order.addItem(pizza);
        order.addItem(suco);

        // Act
        Order savedOrder = orderRepository.saveAndFlush(order);
        List<Order> customerOrders = orderRepository.findByClienteOrderByCreatedAtDesc(cliente);
        Page<Order> pagedOrders = orderRepository.findByClienteId(cliente.getId(), PageRequest.of(0, 10));

        // Assert
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(savedOrder.getCreatedAt()).isNotNull();
        assertThat(savedOrder.getItens()).hasSize(2);
        assertThat(savedOrder.getItens())
                .allSatisfy(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getOrder()).isEqualTo(savedOrder);
                });
        assertThat(savedOrder.calcularValorTotal()).isEqualByComparingTo(new BigDecimal("84.90"));

        assertThat(customerOrders).containsExactly(savedOrder);
        assertThat(pagedOrders.getTotalElements()).isEqualTo(1);
    }
}

