package com.docportal.docportal.dto;

import com.docportal.docportal.entities.User;

public record UserDto(Long id, String username, String fullName, String email, String role) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null
        );
    }
}
