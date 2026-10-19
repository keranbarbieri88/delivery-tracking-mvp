package com.delivery.api.order;

import com.delivery.api.shared.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDomainTest {

    @Test
    @DisplayName("Deve permitir o fluxo completo válido: RECEBIDO -> EM_PREPARO -> ENVIADO -> ENTREGUE")
    void shouldAllowFullValidDeliveryLifecycle() {
        // Arrange
        Order order = Order.builder()
                .enderecoEntrega("Rua Central, 10")
                .build();

        // Act & Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(OrderStatus.RECEBIDO.nextValidStatuses())
                .containsExactlyInAnyOrder(OrderStatus.EM_PREPARO, OrderStatus.CANCELADO);

        order.updateStatus(OrderStatus.EM_PREPARO);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EM_PREPARO);
        assertThat(OrderStatus.EM_PREPARO.nextValidStatuses())
                .containsExactlyInAnyOrder(OrderStatus.ENVIADO, OrderStatus.CANCELADO);

        order.updateStatus(OrderStatus.ENVIADO);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENVIADO);
        assertThat(OrderStatus.ENVIADO.nextValidStatuses())
                .containsExactly(OrderStatus.ENTREGUE);

        order.updateStatus(OrderStatus.ENTREGUE);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENTREGUE);
        assertThat(OrderStatus.ENTREGUE.nextValidStatuses()).isEmpty();
    }

    @Test
    @DisplayName("Deve permitir cancelamento a partir de RECEBIDO ou EM_PREPARO, mas bloquear após ENVIADO")
    void shouldAllowCancellationOnlyFromRecebidoOrEmPreparo() {
        // Arrange
        Order orderRecebido = Order.builder().status(OrderStatus.RECEBIDO).build();
        Order orderEmPreparo = Order.builder().status(OrderStatus.EM_PREPARO).build();
        Order orderEnviado = Order.builder().status(OrderStatus.ENVIADO).build();

        // Act
        orderRecebido.updateStatus(OrderStatus.CANCELADO);
        orderEmPreparo.updateStatus(OrderStatus.CANCELADO);

        // Assert
        assertThat(orderRecebido.getStatus()).isEqualTo(OrderStatus.CANCELADO);
        assertThat(orderEmPreparo.getStatus()).isEqualTo(OrderStatus.CANCELADO);
        assertThat(OrderStatus.CANCELADO.nextValidStatuses()).isEmpty();

        assertThatThrownBy(() -> orderEnviado.updateStatus(OrderStatus.CANCELADO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("ENVIADO")
                .hasMessageContaining("CANCELADO");
    }

    @Test
    @DisplayName("Deve bloquear saltos de etapas inválidos e alterações em pedidos finalizados (ENTREGUE ou CANCELADO)")
    void shouldRejectInvalidStageJumpsAndFinalizedOrders() {
        // Arrange
        Order orderRecebido = Order.builder().status(OrderStatus.RECEBIDO).build();
        Order orderEmPreparo = Order.builder().status(OrderStatus.EM_PREPARO).build();
        Order orderEntregue = Order.builder().status(OrderStatus.ENTREGUE).build();
        Order orderCancelado = Order.builder().status(OrderStatus.CANCELADO).build();

        // Act & Assert
        assertThat(OrderStatus.RECEBIDO.canTransitionTo(null)).isFalse();

        assertThatThrownBy(() -> orderRecebido.updateStatus(OrderStatus.ENVIADO))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThatThrownBy(() -> orderRecebido.updateStatus(OrderStatus.ENTREGUE))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThatThrownBy(() -> orderEmPreparo.updateStatus(OrderStatus.ENTREGUE))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThatThrownBy(() -> orderEntregue.updateStatus(OrderStatus.EM_PREPARO))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThatThrownBy(() -> orderCancelado.updateStatus(OrderStatus.ENVIADO))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao tentar atualizar status para null ou adicionar item null")
    void shouldThrowIllegalArgumentExceptionWhenStatusOrItemIsNull() {
        // Arrange
        Order order = Order.builder()
                .enderecoEntrega("Rua Central, 10")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> order.updateStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obrigatório");

        assertThatThrownBy(() -> order.addItem(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    @DisplayName("Deve inicializar status, createdAt e updatedAt no prePersist quando nulos")
    void shouldInitializeDefaultFieldsOnPrePersist() {
        // Arrange
        Order order = new Order();

        // Act
        order.prePersist();

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    @DisplayName("Deve retornar BigDecimal.ZERO quando lista de itens ou campos de cálculo forem nulos/vazios")
    void shouldReturnZeroWhenItemsOrFieldsAreNull() {
        // Arrange
        Order emptyOrder = Order.builder().itens(null).build();
        OrderItem itemWithoutPrice = OrderItem.builder().nome("Brinde").quantidade(2).precoUnitario(null).build();
        OrderItem itemWithoutQty = OrderItem.builder().nome("Brinde").quantidade(null).precoUnitario(BigDecimal.TEN).build();

        // Act & Assert
        assertThat(emptyOrder.calcularValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(itemWithoutPrice.calcularSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(itemWithoutQty.calcularSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve validar contrato equals e hashCode baseados no ID em Order e OrderItem")
    void shouldValidateEqualsAndHashCodeContract() {
        // Arrange
        UUID sharedId = UUID.randomUUID();
        Order order1 = Order.builder().id(sharedId).build();
        Order order2 = Order.builder().id(sharedId).build();
        Order orderWithoutId = Order.builder().build();

        OrderItem item1 = OrderItem.builder().id(sharedId).build();
        OrderItem item2 = OrderItem.builder().id(sharedId).build();
        OrderItem itemWithoutId = OrderItem.builder().build();

        // Act & Assert
        assertThat(order1).isEqualTo(order2);
        assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
        assertThat(order1).isNotEqualTo(orderWithoutId);
        assertThat(orderWithoutId).isNotEqualTo(Order.builder().build());

        assertThat(item1).isEqualTo(item2);
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        assertThat(item1).isNotEqualTo(itemWithoutId);
    }
}

