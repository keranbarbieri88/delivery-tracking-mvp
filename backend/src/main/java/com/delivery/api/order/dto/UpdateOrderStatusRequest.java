package com.delivery.api.order.dto;

import com.delivery.api.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "O novo status do pedido é obrigatório")
        OrderStatus status
) {
}

