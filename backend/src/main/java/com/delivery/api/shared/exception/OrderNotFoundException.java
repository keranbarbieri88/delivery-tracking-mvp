package com.delivery.api.shared.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Pedido não encontrado com o ID: " + id);
    }
}

