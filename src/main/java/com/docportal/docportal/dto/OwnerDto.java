package com.docportal.docportal.dto;

import com.docportal.docportal.entities.User;

public record OwnerDto(Long id, String name) {

    public static OwnerDto from(User user) {
        return new OwnerDto(
                user.getId(),
                user.getFullName() != null ? user.getFullName() : user.getUsername()
        );
    }
}
