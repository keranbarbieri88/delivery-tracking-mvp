package com.delivery.api.order.dto;

import com.delivery.api.order.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        String nome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
    public static OrderItemResponse fromEntity(OrderItem item) {
        BigDecimal subtotal = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
        return new OrderItemResponse(
                item.getId(),
                item.getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                subtotal
        );
    }
}

