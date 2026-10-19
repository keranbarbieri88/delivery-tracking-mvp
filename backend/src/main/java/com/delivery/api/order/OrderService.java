package com.delivery.api.order;

import com.delivery.api.auth.Role;
import com.delivery.api.auth.UserEntity;
import com.delivery.api.auth.UserRepository;
import com.delivery.api.order.dto.CreateOrderRequest;
import com.delivery.api.order.dto.OrderItemRequest;
import com.delivery.api.order.dto.OrderResponse;
import com.delivery.api.order.dto.UpdateOrderStatusRequest;
import com.delivery.api.shared.exception.OrderNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String userEmail) {
        UserEntity cliente = findUserByEmail(userEmail);

        Order order = Order.builder()
                .cliente(cliente)
                .enderecoEntrega(request.enderecoEntrega().trim())
                .status(OrderStatus.RECEBIDO)
                .build();

        for (OrderItemRequest itemRequest : request.itens()) {
            OrderItem item = OrderItem.builder()
                    .nome(itemRequest.nome().trim())
                    .quantidade(itemRequest.quantidade())
                    .precoUnitario(itemRequest.precoUnitario())
                    .build();
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);
        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(String userEmail) {
        UserEntity user = findUserByEmail(userEmail);
        boolean isStaff = user.getRole() == Role.ADMIN || user.getRole() == Role.LOJISTA;

        List<Order> orders = isStaff
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByClienteOrderByCreatedAtDesc(user);

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id, String userEmail) {
        UserEntity user = findUserByEmail(userEmail);
        Order order = findOrderById(id);

        boolean isStaff = user.getRole() == Role.ADMIN || user.getRole() == Role.LOJISTA;
        boolean isOwner = order.getCliente().getId().equals(user.getId());

        if (!isStaff && !isOwner) {
            throw new AccessDeniedException("Acesso negado: o pedido não pertence ao usuário autenticado");
        }

        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request, String userEmail) {
        UserEntity user = findUserByEmail(userEmail);
        Order order = findOrderById(orderId);

        validateStatusUpdatePermissions(order, user, request.status());

        order.updateStatus(request.status());
        // O Dirty Checking do Hibernate sincroniza a entidade no commit da transação
        return OrderResponse.fromEntity(order);
    }

    private void validateStatusUpdatePermissions(Order order, UserEntity user, OrderStatus newStatus) {
        boolean isStaff = user.getRole() == Role.ADMIN || user.getRole() == Role.LOJISTA;
        boolean isOwner = order.getCliente().getId().equals(user.getId());

        if (isStaff) {
            return;
        }

        if (isOwner) {
            if (newStatus != OrderStatus.CANCELADO) {
                throw new AccessDeniedException("Clientes só possuem permissão para cancelar pedidos.");
            }
            if (order.getStatus() != OrderStatus.RECEBIDO) {
                throw new AccessDeniedException("O pedido já está em preparação ou despachado e não pode mais ser cancelado pelo cliente.");
            }
            return;
        }

        throw new AccessDeniedException("Acesso negado: usuário não autorizado a alterar o status deste pedido");
    }

    private Order findOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        
    }

    private UserEntity findUserByEmail(String userEmail) {
        return userRepository.findByEmailIgnoreCase(userEmail.trim())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com o e-mail: " + userEmail));
    }
}