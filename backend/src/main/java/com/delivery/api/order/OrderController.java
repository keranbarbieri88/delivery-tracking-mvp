package com.delivery.api.order;

import com.delivery.api.auth.UserSecurityDetails;
import com.delivery.api.order.dto.CreateOrderRequest;
import com.delivery.api.order.dto.OrderResponse;
import com.delivery.api.order.dto.UpdateOrderStatusRequest;
import com.delivery.api.shared.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Tag(name = "2. Pedidos", description = "Gestão de pedidos de entrega, consulta de detalhes e máquina de estados de status")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Criar novo pedido",
            description = "Registra um novo pedido com status inicial RECEBIDO e calcula automaticamente os subtotais dos itens e o valor total no servidor."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos itens ou endereço"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserSecurityDetails userDetails
    ) {
        OrderResponse created = orderService.createOrder(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Listar pedidos",
            description = "Lista pedidos ordenados pelos mais recentes (createdAt DESC). Perfis LOJISTA/ADMIN visualizam todos os pedidos; CLIENTE visualiza apenas os próprios pedidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @AuthenticationPrincipal UserSecurityDetails userDetails
    ) {
        List<OrderResponse> orders = orderService.listOrders(userDetails.getUsername());
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "Buscar pedido por ID",
            description = "Retorna os detalhes completos de um pedido específico, validando se pertence ao cliente autenticado ou se o usuário possui perfil LOJISTA/ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao pedido de outro cliente"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserSecurityDetails userDetails
    ) {
        OrderResponse order = orderService.getOrderById(id, userDetails.getUsername());
        return ResponseEntity.ok(order);
    }

    @Operation(
            summary = "Atualizar status do pedido",
            description = "Atualiza o status do pedido respeitando a máquina de estados (RECEBIDO -> EM_PREPARO -> SAIU_PARA_ENTREGA -> ENTREGUE / CANCELADO) e as permissões do perfil."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Valor de status inválido no JSON"),
            @ApiResponse(responseCode = "403", description = "Permissão insuficiente para a transição solicitada"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Transição de estado inválida na máquina de estados")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserSecurityDetails userDetails
    ) {
        OrderResponse updated = orderService.updateOrderStatus(id, request, userDetails.getUsername());
        return ResponseEntity.ok(updated);
    }
}
