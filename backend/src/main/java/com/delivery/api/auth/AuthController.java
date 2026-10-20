package com.delivery.api.auth;

import com.delivery.api.auth.dto.AuthResponse;
import com.delivery.api.auth.dto.LoginRequest;
import com.delivery.api.auth.dto.RegisterRequest;
import com.delivery.api.auth.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Autenticação", description = "Endpoints públicos para registro de usuários e emissão de token JWT")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Registrar novo usuário",
            description = "Cria uma conta de usuário (CLIENTE ou LOJISTA) com e-mail único e senha criptografada via BCrypt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos informados"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado no sistema")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Autenticar usuário e emitir JWT",
            description = "Valida as credenciais (e-mail e senha) e retorna um token JWT assinado com HMAC-SHA256."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload de login inválido"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
