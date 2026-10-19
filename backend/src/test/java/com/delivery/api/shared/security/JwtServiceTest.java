package com.delivery.api.shared.security;

import com.delivery.api.auth.UserEntity;
import com.delivery.api.auth.UserSecurityDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String OTHER_SECRET = "70596B5667536450624B2B4728443F412F7838753572326E586A556652634E40";

    @Test
    @DisplayName("Deve gerar token JWT válido e extrair email e userId corretamente")
    void shouldGenerateValidTokenAndExtractClaims() {
        // Arrange
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .nome("Carlos Lima")
                .email("carlos@delivery.com")
                .senha("$2a$10$hashed")
                .build();
        UserSecurityDetails userDetails = new UserSecurityDetails(user);

        // Act
        String token = jwtService.generateToken(user);
        String extractedEmail = jwtService.extractUsername(token);
        UUID extractedUserId = jwtService.extractUserId(token);
        boolean validForUser = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(extractedEmail).isEqualTo("carlos@delivery.com");
        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(validForUser).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar token JWT expirado")
    void shouldRejectExpiredToken() {
        // Arrange
        JwtService expiredJwtService = new JwtService(SECRET, -5_000L);
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Carlos Lima")
                .email("carlos@delivery.com")
                .senha("$2a$10$hashed")
                .build();
        UserSecurityDetails userDetails = new UserSecurityDetails(user);
        String expiredToken = expiredJwtService.generateToken(user);

        // Act
        boolean valid = expiredJwtService.isTokenValid(expiredToken, userDetails);

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar token JWT adulterado ou assinado com chave diferente")
    void shouldRejectTamperedOrForeignSignedToken() {
        // Arrange
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        JwtService otherJwtService = new JwtService(OTHER_SECRET, 60_000L);
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Carlos Lima")
                .email("carlos@delivery.com")
                .senha("$2a$10$hashed")
                .build();
        UserSecurityDetails userDetails = new UserSecurityDetails(user);

        String foreignToken = otherJwtService.generateToken(user);
        String validToken = jwtService.generateToken(user);
        String tamperedToken = validToken.substring(0, validToken.length() - 4) + "abcd";

        // Act
        boolean foreignValid = jwtService.isTokenValid(foreignToken, userDetails);
        boolean tamperedValid = jwtService.isTokenValid(tamperedToken, userDetails);

        // Assert
        assertThat(foreignValid).isFalse();
        assertThat(tamperedValid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar token quando o UserDetails pertencer a outro e-mail ou token for malformado")
    void shouldRejectTokenWhenUsernameDoesNotMatchOrTokenIsMalformed() {
        // Arrange
        JwtService jwtService = new JwtService("chave-secreta-utf8-com-mais-de-32-caracteres-seguros!", 60_000L);
        UserEntity tokenOwner = UserEntity.builder()
                .email("dono@delivery.com")
                .senha("$2a$10$hash")
                .build();
        UserEntity anotherUser = UserEntity.builder()
                .email("intruso@delivery.com")
                .senha("$2a$10$hash")
                .build();

        String token = jwtService.generateToken(tokenOwner);

        // Act & Assert
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isNull();
        assertThat(jwtService.isTokenValid(token, new UserSecurityDetails(anotherUser))).isFalse();
        assertThat(jwtService.isTokenValid("token.invalido.malformado")).isFalse();
    }
}

