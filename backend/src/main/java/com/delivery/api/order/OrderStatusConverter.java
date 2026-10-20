package com.delivery.api.order;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        String normalized = dbData.trim().toUpperCase();

        // Sanitização defensiva para registros legados no banco
        if ("ENVIADO".equals(normalized) || "DESPACHADO".equals(normalized)) {
            return OrderStatus.SAIU_PARA_ENTREGA;
        }

        return OrderStatus.valueOf(normalized);
    }
}
