package com.delivery.api.order;

import com.delivery.api.auth.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "itens"})
    Optional<Order> findById(UUID id);

    @EntityGraph(attributePaths = {"cliente", "itens"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"cliente", "itens"})
    List<Order> findByClienteOrderByCreatedAtDesc(UserEntity cliente);

    Page<Order> findByClienteId(UUID clienteId, Pageable pageable);
}

