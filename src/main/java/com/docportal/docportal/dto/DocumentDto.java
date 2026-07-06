package com.docportal.docportal.dto;

import com.docportal.docportal.entities.Document;

import java.time.LocalDateTime;

public record DocumentDto(
        Long id,
        String title,
        String originalFileName,
        String extension,
        String mimeType,
        Long size,
        String description,
        String tags,
        String visibility,
        String status,
        Long categoryId,
        String categoryName,
        Long ownerId,
        String ownerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentDto from(Document d) {
        return new DocumentDto(
                d.getId(),
                d.getTitle(),
                d.getOriginalFileName(),
                d.getExtension(),
                d.getMimeType(),
                d.getSize(),
                d.getDescription(),
                d.getTags(),
                d.getVisibility() != null ? d.getVisibility().name() : null,
                d.getStatus() != null ? d.getStatus().name() : null,
                d.getCategory() != null ? d.getCategory().getId() : null,
                d.getCategory() != null ? d.getCategory().getName() : null,
                d.getOwner() != null ? d.getOwner().getId() : null,
                d.getOwner() != null
                        ? (d.getOwner().getFullName() != null ? d.getOwner().getFullName() : d.getOwner().getUsername())
                        : null,
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
