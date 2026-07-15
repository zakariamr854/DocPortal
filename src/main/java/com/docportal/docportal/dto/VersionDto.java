package com.docportal.docportal.dto;

import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.DocumentVersion;

import java.time.LocalDateTime;

public record VersionDto(
        Long id,
        Integer versionNumber,
        String originalFileName,
        String extension,
        Long size,
        String title,
        String description,
        String tags,
        String visibility,
        String categoryName,
        String comment,
        String uploadedBy,
        LocalDateTime createdAt,
        boolean current
) {

    public static VersionDto from(DocumentVersion v) {
        return new VersionDto(
                v.getId(),
                v.getVersionNumber(),
                v.getOriginalFileName(),
                v.getExtension(),
                v.getSize(),
                v.getTitle(),
                v.getDescription(),
                v.getTags(),
                v.getVisibility(),
                v.getCategoryName(),
                v.getComment(),
                v.getUploadedBy() != null
                        ? (v.getUploadedBy().getFullName() != null ? v.getUploadedBy().getFullName() : v.getUploadedBy().getUsername())
                        : null,
                v.getCreatedAt(),
                false
        );
    }

    /** L'état actuel du document, présenté comme dernière version de la liste. */
    public static VersionDto currentFrom(Document d, int versionNumber) {
        return new VersionDto(
                null,
                versionNumber,
                d.getOriginalFileName(),
                d.getExtension(),
                d.getSize(),
                d.getTitle(),
                d.getDescription(),
                d.getTags(),
                d.getVisibility() != null ? d.getVisibility().name() : null,
                d.getCategory() != null ? d.getCategory().getName() : null,
                "Version actuelle",
                d.getOwner() != null
                        ? (d.getOwner().getFullName() != null ? d.getOwner().getFullName() : d.getOwner().getUsername())
                        : null,
                d.getUpdatedAt() != null ? d.getUpdatedAt() : d.getCreatedAt(),
                true
        );
    }
}
