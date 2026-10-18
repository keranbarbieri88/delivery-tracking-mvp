package com.delivery.api.auth.dto;

import com.delivery.api.auth.UserEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String nome,
        String email,
        LocalDateTime createdAt
) {
    public static UserResponse fromEntity(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}

