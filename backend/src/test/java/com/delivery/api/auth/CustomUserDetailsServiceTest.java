package com.delivery.api.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Deve carregar UserSecurityDetails com ROLE_USER quando e-mail existir")
    void shouldLoadUserByUsernameWhenEmailExists() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Rafael Souza")
                .email("rafael@delivery.com")
                .senha("$2a$10$hashedPassword")
                .build();
        when(userRepository.findByEmailIgnoreCase("rafael@delivery.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("  rafael@delivery.com ");

        // Assert
        assertThat(userDetails).isInstanceOf(UserSecurityDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo("rafael@delivery.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(((UserSecurityDetails) userDetails).getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando e-mail não for encontrado")
    void shouldThrowUsernameNotFoundExceptionWhenEmailDoesNotExist() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("naoexiste@delivery.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("naoexiste@delivery.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("naoexiste@delivery.com");
    }
}

