package com.delivery.api.shared.exception;

import com.delivery.api.order.OrderStatus;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/pedidos");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request com mapa validationErrors ao tratar MethodArgumentNotValidException")
    void shouldReturn400WithValidationErrorsMap() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> fieldErrors = List.of(
                new FieldError("createOrderRequest", "enderecoEntrega", "O endereço de entrega é obrigatório"),
                new FieldError("createOrderRequest", "itens[0].quantidade", "A quantidade mínima é 1")
        );
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationErrors(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Erro de validação nos campos informados");
        assertThat(body.path()).isEqualTo("/api/v1/pedidos");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.validationErrors())
                .hasSize(2)
                .containsEntry("enderecoEntrega", "O endereço de entrega é obrigatório")
                .containsEntry("itens[0].quantidade", "A quantidade mínima é 1");
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found para OrderNotFoundException e EntityNotFoundException")
    void shouldReturn404ForNotFoundExceptions() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderNotFoundException orderNotFoundEx = new OrderNotFoundException(orderId);
        EntityNotFoundException entityNotFoundEx = new EntityNotFoundException("Usuário não encontrado");

        // Act
        ResponseEntity<ErrorResponse> orderResponse = exceptionHandler.handleNotFound(orderNotFoundEx, request);
        ResponseEntity<ErrorResponse> entityResponse = exceptionHandler.handleNotFound(entityNotFoundEx, request);

        // Assert
        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(orderResponse.getBody()).isNotNull();
        assertThat(orderResponse.getBody().status()).isEqualTo(404);
        assertThat(orderResponse.getBody().error()).isEqualTo("Not Found");
        assertThat(orderResponse.getBody().message()).contains(orderId.toString());
        assertThat(orderResponse.getBody().path()).isEqualTo("/api/v1/pedidos");
        assertThat(orderResponse.getBody().validationErrors()).isNull();

        assertThat(entityResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entityResponse.getBody()).isNotNull();
        assertThat(entityResponse.getBody().message()).isEqualTo("Usuário não encontrado");
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden para AccessDeniedException com mensagem específica ou padrão")
    void shouldReturn403ForAccessDeniedException() {
        // Arrange
        AccessDeniedException customEx = new AccessDeniedException("Você não tem permissão para visualizar este pedido.");
        AccessDeniedException blankEx = new AccessDeniedException("");

        // Act
        ResponseEntity<ErrorResponse> customResponse = exceptionHandler.handleAccessDenied(customEx, request);
        ResponseEntity<ErrorResponse> fallbackResponse = exceptionHandler.handleAccessDenied(blankEx, request);

        // Assert
        assertThat(customResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(customResponse.getBody()).isNotNull();
        assertThat(customResponse.getBody().status()).isEqualTo(403);
        assertThat(customResponse.getBody().error()).isEqualTo("Forbidden");
        assertThat(customResponse.getBody().message()).isEqualTo("Você não tem permissão para visualizar este pedido.");
        assertThat(customResponse.getBody().path()).isEqualTo("/api/v1/pedidos");

        assertThat(fallbackResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(fallbackResponse.getBody()).isNotNull();
        assertThat(fallbackResponse.getBody().message()).contains("Acesso negado");
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized para BadCredentialsException e JwtException")
    void shouldReturn401ForAuthenticationAndJwtExceptions() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        BadCredentialsException badCredentialsEx = new BadCredentialsException("Bad credentials");
        JwtException jwtEx = new JwtException("Token expirado");

        // Act
        ResponseEntity<ErrorResponse> authResponse = exceptionHandler.handleAuthenticationException(badCredentialsEx, request);
        ResponseEntity<ErrorResponse> jwtResponse = exceptionHandler.handleAuthenticationException(jwtEx, request);

        // Assert
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(authResponse.getBody()).isNotNull();
        assertThat(authResponse.getBody().status()).isEqualTo(401);
        assertThat(authResponse.getBody().error()).isEqualTo("Unauthorized");
        assertThat(authResponse.getBody().message()).contains("Credenciais inválidas");
        assertThat(authResponse.getBody().path()).isEqualTo("/api/v1/auth/login");

        assertThat(jwtResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(jwtResponse.getBody()).isNotNull();
        assertThat(jwtResponse.getBody().status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Deve retornar 422 Unprocessable Entity para InvalidStatusTransitionException, BusinessException e IllegalStateException")
    void shouldReturn422ForBusinessAndStatusTransitionExceptions() {
        // Arrange
        InvalidStatusTransitionException transitionEx = new InvalidStatusTransitionException(OrderStatus.ENTREGUE, OrderStatus.CANCELADO);
        BusinessException businessEx = new BusinessException("Regra de negócio violada");
        IllegalStateException illegalStateEx = new IllegalStateException("Estado ilegal do pedido");

        // Act
        ResponseEntity<ErrorResponse> transitionResponse = exceptionHandler.handleBusinessException(transitionEx, request);
        ResponseEntity<ErrorResponse> businessResponse = exceptionHandler.handleBusinessException(businessEx, request);
        ResponseEntity<ErrorResponse> stateResponse = exceptionHandler.handleBusinessException(illegalStateEx, request);

        // Assert
        assertThat(transitionResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(transitionResponse.getBody()).isNotNull();
        assertThat(transitionResponse.getBody().status()).isEqualTo(422);
        assertThat(transitionResponse.getBody().error()).isEqualTo("Unprocessable Entity");
        assertThat(transitionResponse.getBody().message()).contains("ENTREGUE").contains("CANCELADO");

        assertThat(businessResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(businessResponse.getBody()).isNotNull();
        assertThat(businessResponse.getBody().message()).isEqualTo("Regra de negócio violada");

        assertThat(stateResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(stateResponse.getBody()).isNotNull();
        assertThat(stateResponse.getBody().message()).isEqualTo("Estado ilegal do pedido");
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict para EmailAlreadyExistsException e 400 Bad Request para IllegalArgument/HttpMessageNotReadable")
    void shouldReturn409ForEmailConflictAnd400ForBadInputs() {
        // Arrange
        EmailAlreadyExistsException conflictEx = new EmailAlreadyExistsException("duplicado@delivery.com");
        IllegalArgumentException illegalArgEx = new IllegalArgumentException("O novo status do pedido não pode ser nulo");
        HttpMessageNotReadableException unreadableEx = new HttpMessageNotReadableException("JSON parse error", (HttpInputMessage) null);

        // Act
        ResponseEntity<ErrorResponse> conflictResponse = exceptionHandler.handleEmailAlreadyExists(conflictEx, request);
        ResponseEntity<ErrorResponse> illegalArgResponse = exceptionHandler.handleIllegalArgument(illegalArgEx, request);
        ResponseEntity<ErrorResponse> unreadableResponse = exceptionHandler.handleMessageNotReadable(unreadableEx, request);

        // Assert
        assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflictResponse.getBody()).isNotNull();
        assertThat(conflictResponse.getBody().status()).isEqualTo(409);
        assertThat(conflictResponse.getBody().error()).isEqualTo("Conflict");
        assertThat(conflictResponse.getBody().message()).contains("duplicado@delivery.com");

        assertThat(illegalArgResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(illegalArgResponse.getBody()).isNotNull();
        assertThat(illegalArgResponse.getBody().status()).isEqualTo(400);
        assertThat(illegalArgResponse.getBody().message()).isEqualTo("O novo status do pedido não pode ser nulo");

        assertThat(unreadableResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unreadableResponse.getBody()).isNotNull();
        assertThat(unreadableResponse.getBody().status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve retornar 500 Internal Server Error com mensagem genérica sem vazar detalhes internos no fallback")
    void shouldReturn500WithSafeGenericMessageOnUnexpectedException() {
        // Arrange
        RuntimeException unexpectedEx = new RuntimeException("org.hibernate.exception.SQLGrammarException: secret table details");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnexpectedException(unexpectedEx, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("Ocorreu um erro interno inesperado. Tente novamente mais tarde.");
        assertThat(response.getBody().message()).doesNotContain("SQLGrammarException").doesNotContain("secret table");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/pedidos");
    }
}

