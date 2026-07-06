package com.docportal.docportal.dto;

import com.docportal.docportal.entities.DocumentHistory;

import java.time.LocalDateTime;

public record HistoryDto(
        Long id,
        Long documentId,
        String documentName,
        String userName,
        String action,
        String comment,
        LocalDateTime actionDate
) {

    public static HistoryDto from(DocumentHistory h) {
        return new HistoryDto(
                h.getId(),
                h.getDocument() != null ? h.getDocument().getId() : null,
                h.getDocument() != null
                        ? (h.getDocument().getTitle() != null ? h.getDocument().getTitle() : h.getDocument().getOriginalFileName())
                        : null,
                h.getUser() != null
                        ? (h.getUser().getFullName() != null ? h.getUser().getFullName() : h.getUser().getUsername())
                        : null,
                h.getAction() != null ? h.getAction().name() : null,
                h.getComment(),
                h.getActionDate()
        );
    }
}
