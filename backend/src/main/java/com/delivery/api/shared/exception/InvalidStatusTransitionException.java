package com.delivery.api.shared.exception;

import com.delivery.api.order.OrderStatus;

public class InvalidStatusTransitionException extends BusinessException {

    public InvalidStatusTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super("Transição de status inválida de " + currentStatus
                + " para " + targetStatus
                + ". Transições permitidas a partir de " + currentStatus + ": "
                + (currentStatus != null ? currentStatus.nextValidStatuses() : "[]"));
    }
}
