package com.delivery.api.auth.dto;

import com.delivery.api.auth.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
        String senha,
        
        @NotNull(message = "O perfil (role) é obrigatório")
        Role role
) {
        
    // Construtor compacto para definir CLIENTE por padrão se for nulo
    public RegisterRequest {
        if (role == null) {
            role = Role.CLIENTE;
        }
    }

    // Sobrecarga com 3 parâmetros para manter compatibilidade com os testes antigos
    public RegisterRequest(String nome, String email, String senha) {
        this(nome, email, senha, Role.CLIENTE);
    }
        
}

