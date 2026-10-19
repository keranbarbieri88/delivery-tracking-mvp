package com.delivery.api.order.dto;

import com.delivery.api.order.Order;
import com.delivery.api.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String enderecoEntrega,
        OrderStatus status,
        BigDecimal total,
        Instant createdAt,
        List<OrderItemResponse> itens
) {
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> itensResponse = order.getItens().stream()
                .map(OrderItemResponse::fromEntity)
                .toList();
        BigDecimal total = itensResponse.stream()
                .map(OrderItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(
                order.getId(),
                order.getEnderecoEntrega(),
                order.getStatus(),
                total,
                order.getCreatedAt(),
                itensResponse
        );
    }
}

