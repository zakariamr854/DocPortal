package com.docportal.docportal.dto;

public record DocumentUpdateRequest(
        String title,
        String description,
        String tags,
        Long categoryId,
        String visibility
) {
}
