package com.delivery.api.auth;

import com.delivery.api.auth.dto.RegisterRequest;
import com.delivery.api.auth.dto.UserResponse;
import com.delivery.api.shared.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                .build();

        UserEntity savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }
}

