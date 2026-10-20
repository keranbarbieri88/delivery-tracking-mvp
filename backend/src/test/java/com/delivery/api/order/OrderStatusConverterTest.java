package com.delivery.api.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusConverterTest {

    private OrderStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new OrderStatusConverter();
    }

    @Test
    @DisplayName("Deve converter OrderStatus para String na coluna do banco")
    void shouldConvertToDatabaseColumn() {
        // Arrange
        OrderStatus status = OrderStatus.SAIU_PARA_ENTREGA;

        // Act
        String columnValue = converter.convertToDatabaseColumn(status);

        // Assert
        assertThat(columnValue).isEqualTo("SAIU_PARA_ENTREGA");
    }

    @ParameterizedTest
    @ValueSource(strings = { "ENVIADO", "enviado", "DESPACHADO", "despachado", " ENVIADO " })
    @DisplayName("Deve sanitizar status legados mapeando para SAIU_PARA_ENTREGA")
    void shouldSanitizeLegacyStatuses(String legacyStatus) {
        // Arrange & Act
        OrderStatus result = converter.convertToEntityAttribute(legacyStatus);

        // Assert
        assertThat(result).isEqualTo(OrderStatus.SAIU_PARA_ENTREGA);
    }

    @Test
    @DisplayName("Deve converter status normais corretamente")
    void shouldConvertStandardStatuses() {
        // Arrange & Act & Assert
        assertThat(converter.convertToEntityAttribute("RECEBIDO")).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(converter.convertToEntityAttribute("EM_PREPARO")).isEqualTo(OrderStatus.EM_PREPARO);
        assertThat(converter.convertToEntityAttribute("ENTREGUE")).isEqualTo(OrderStatus.ENTREGUE);
        assertThat(converter.convertToEntityAttribute("CANCELADO")).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    @DisplayName("Deve retornar null para dados nulos ou vazios")
    void shouldReturnNullWhenDataIsBlankOrNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para status completamente desconhecido")
    void shouldThrowExceptionForUnknownStatus() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("STATUS_INEXISTENTE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
