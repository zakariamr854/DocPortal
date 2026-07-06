package com.docportal.docportal.dto;

import com.docportal.docportal.entities.Category;

public record CategoryDto(Long id, String name, String description, Boolean active, long documentCount) {

    public static CategoryDto from(Category category, long documentCount) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getActive(),
                documentCount
        );
    }
}
