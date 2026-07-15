package com.docportal.docportal.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Photographie (snapshot) de l'état d'un document AVANT une modification :
 * chaque fois qu'un utilisateur modifie un document (métadonnées, fichier,
 * restauration), l'état précédent est figé ici — fichier ET informations.
 */
@Entity
@Table(name = "document_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private Integer versionNumber;

    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String filePath;

    private String extension;

    private String mimeType;

    private Long size;

    // Métadonnées du document au moment du snapshot
    private String title;

    @Column(length = 2000)
    private String description;

    private String tags;

    private String visibility;

    private String categoryName;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
