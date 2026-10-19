package com.delivery.api.shared.security;

import com.delivery.api.auth.UserEntity;
import com.delivery.api.auth.UserSecurityDetails;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("shouldNotFilter deve ignorar rotas públicas de auth, swagger e h2-console, mas filtrar rotas de pedidos")
    void shouldSkipPublicEndpointsAndFilterProtectedOnes() {
        // Arrange
        MockHttpServletRequest authReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        authReq.setServletPath("/api/v1/auth/login");

        MockHttpServletRequest swaggerReq = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        swaggerReq.setServletPath("/swagger-ui/index.html");

        MockHttpServletRequest apiDocsReq = new MockHttpServletRequest("GET", "/v3/api-docs");
        apiDocsReq.setServletPath("/v3/api-docs");

        MockHttpServletRequest h2Req = new MockHttpServletRequest("GET", "/h2-console/login.do");
        h2Req.setServletPath("/h2-console/login.do");

        MockHttpServletRequest ordersReq = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        ordersReq.setServletPath("/api/v1/pedidos");

        // Act & Assert
        assertThat(jwtAuthenticationFilter.shouldNotFilter(authReq)).isTrue();
        assertThat(jwtAuthenticationFilter.shouldNotFilter(swaggerReq)).isTrue();
        assertThat(jwtAuthenticationFilter.shouldNotFilter(apiDocsReq)).isTrue();
        assertThat(jwtAuthenticationFilter.shouldNotFilter(h2Req)).isTrue();
        assertThat(jwtAuthenticationFilter.shouldNotFilter(ordersReq)).isFalse();
    }

    @Test
    @DisplayName("Deve prosseguir sem autenticar quando cabeçalho Authorization estiver ausente ou sem prefixo Bearer")
    void shouldContinueChainWhenAuthorizationHeaderIsMissingOrNotBearer() throws Exception {
        // Arrange
        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        MockHttpServletRequest requestBasicAuth = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        requestBasicAuth.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        jwtAuthenticationFilter.doFilterInternal(requestWithoutHeader, response, filterChain);
        jwtAuthenticationFilter.doFilterInternal(requestBasicAuth, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("Deve autenticar e popular SecurityContextHolder quando token Bearer for válido")
    void shouldPopulateSecurityContextWhenBearerTokenIsValid() throws Exception {
        // Arrange
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .nome("Helena Martins")
                .email("helena@delivery.com")
                .senha("$2a$10$hash")
                .build();
        UserSecurityDetails userDetails = new UserSecurityDetails(user);

        when(jwtService.extractUsername(token)).thenReturn("helena@delivery.com");
        when(userDetailsService.loadUserByUsername("helena@delivery.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve limpar o contexto de segurança e prosseguir caso ocorra exceção na validação do token ou busca do usuário")
    void shouldClearSecurityContextWhenExceptionOccurs() throws Exception {
        // Arrange
        String token = "valid.token.deleted.user";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(token)).thenReturn("removido@delivery.com");
        when(userDetailsService.loadUserByUsername("removido@delivery.com"))
                .thenThrow(new UsernameNotFoundException("Não encontrado"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}

