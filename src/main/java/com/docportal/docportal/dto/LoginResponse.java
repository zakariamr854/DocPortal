package com.docportal.docportal.dto;

public record LoginResponse(String token, String tokenType, long expiresIn, UserDto user) {
}
