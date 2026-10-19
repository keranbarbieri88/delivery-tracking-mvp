package com.delivery.api.order;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    RECEBIDO {
        @Override
        public Set<OrderStatus> nextValidStatuses() {
            return EnumSet.of(EM_PREPARO, CANCELADO);
        }
    },
    EM_PREPARO {
        @Override
        public Set<OrderStatus> nextValidStatuses() {
            return EnumSet.of(ENVIADO, CANCELADO);
        }
    },
    ENVIADO {
        @Override
        public Set<OrderStatus> nextValidStatuses() {
            return EnumSet.of(ENTREGUE);
        }
    },
    ENTREGUE {
        @Override
        public Set<OrderStatus> nextValidStatuses() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    },
    CANCELADO {
        @Override
        public Set<OrderStatus> nextValidStatuses() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    };

    public abstract Set<OrderStatus> nextValidStatuses();

    public boolean canTransitionTo(OrderStatus next) {
        return next != null && nextValidStatuses().contains(next);
    }
}

