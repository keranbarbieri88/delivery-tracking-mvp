package com.delivery.api.auth;

import com.delivery.api.auth.dto.AuthResponse;
import com.delivery.api.auth.dto.LoginRequest;
import com.delivery.api.auth.dto.RegisterRequest;
import com.delivery.api.auth.dto.UserResponse;
import com.delivery.api.shared.exception.EmailAlreadyExistsException;
import com.delivery.api.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        UserEntity user = UserEntity.builder()
                .nome(request.nome().trim())
                .email(normalizedEmail)
                .senha(passwordEncoder.encode(request.senha()))
                .role(request.role() != null ? request.role() : Role.CLIENTE)
                .build();

        UserEntity savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.senha())
        );

        UserSecurityDetails userDetails = (UserSecurityDetails) authentication.getPrincipal();
        UserEntity user = userDetails.getUser();
        String token = jwtService.generateToken(user);

        return AuthResponse.bearer(token, UserResponse.fromEntity(user));
    }
}

