package com.delivery.api.auth;

import com.delivery.api.auth.dto.RegisterRequest;
import com.delivery.api.auth.dto.UserResponse;
import com.delivery.api.shared.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve registrar usuário com e-mail normalizado e senha criptografada")
    void shouldRegisterUserWithHashedPassword() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Maria Silva", "  Maria@Email.com ", "senhaSegura123");
        UUID generatedId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(userRepository.existsByEmailIgnoreCase("maria@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("$2a$10$hashedPasswordValue");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(generatedId);
            entity.setCreatedAt(now);
            return entity;
        });

        // Act
        UserResponse response = authService.register(request);

        // Assert
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity persisted = userCaptor.getValue();
        assertThat(persisted.getNome()).isEqualTo("Maria Silva");
        assertThat(persisted.getEmail()).isEqualTo("maria@email.com");
        assertThat(persisted.getSenha()).isEqualTo("$2a$10$hashedPasswordValue");
        assertThat(persisted.getSenha()).isNotEqualTo("senhaSegura123");

        assertThat(response.id()).isEqualTo(generatedId);
        assertThat(response.nome()).isEqualTo("Maria Silva");
        assertThat(response.email()).isEqualTo("maria@email.com");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve lançar EmailAlreadyExistsException quando o e-mail já estiver cadastrado")
    void shouldThrowConflictWhenEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest("João Souza", "joao@email.com", "senha123");
        when(userRepository.existsByEmailIgnoreCase("joao@email.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("joao@email.com");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}

