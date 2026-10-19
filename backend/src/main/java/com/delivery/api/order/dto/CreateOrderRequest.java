package com.delivery.api.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "O endereço de entrega é obrigatório")
        String enderecoEntrega,

        @NotEmpty(message = "O pedido deve conter ao menos um item")
        @Valid
        List<OrderItemRequest> itens
) {
}

