package com.docportal.docportal.services;

import com.docportal.docportal.dto.DashboardStatsDto;
import com.docportal.docportal.dto.DocumentDto;
import com.docportal.docportal.dto.DocumentUpdateRequest;
import com.docportal.docportal.dto.HistoryDto;
import com.docportal.docportal.entities.Category;
import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.DocumentHistory;
import com.docportal.docportal.entities.User;
import com.docportal.docportal.enums.ActionType;
import com.docportal.docportal.enums.DocumentStatus;
import com.docportal.docportal.enums.Role;
import com.docportal.docportal.enums.Visibility;
import com.docportal.docportal.exceptions.ApiException;
import com.docportal.docportal.repositories.CategoryRepository;
import com.docportal.docportal.repositories.DocumentHistoryRepository;
import com.docportal.docportal.repositories.DocumentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentHistoryRepository historyRepository;
    private final FileStorageService storage;

    private static boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    @Override
    @Transactional
    public Document upload(MultipartFile file, String title, String description, String tags,
                           Long categoryId, String visibility, User owner) {
        if (categoryId == null) {
            throw ApiException.badRequest("La catégorie est obligatoire");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.badRequest("Catégorie introuvable"));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw ApiException.badRequest("Cette catégorie est désactivée");
        }

        FileStorageService.StoredFile stored = storage.store(file);

        Document document = Document.builder()
                .originalFileName(stored.originalName())
                .storedFileName(stored.storedName())
                .filePath(stored.path())
                .extension(stored.extension())
                .mimeType(file.getContentType())
                .size(file.getSize())
                .title(title != null && !title.isBlank() ? title.trim() : stored.originalName())
                .description(description)
                .tags(tags)
                .visibility(parseVisibility(visibility))
                .status(DocumentStatus.ACTIVE)
                .category(category)
                .owner(owner)
                .build();

        try {
            document = documentRepository.save(document);
        } catch (RuntimeException e) {
            // aucune donnée incohérente ne doit rester : on retire le fichier physique si la base a refusé
            storage.deleteQuietly(stored.storedName());
            throw e;
        }

        record(document, owner, ActionType.UPLOAD, "Upload de " + stored.originalName());
        return document;
    }

    @Override
    public Page<Document> search(String query, Long categoryId, String extension, String status,
                                 boolean mine, User requester, Pageable pageable) {

        DocumentStatus wanted = parseStatus(status);

        Specification<Document> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Les documents supprimés n'apparaissent jamais dans la liste
            predicates.add(cb.notEqual(root.get("status"), DocumentStatus.DELETED));
            predicates.add(cb.equal(root.get("status"), wanted));

            // Droits : un utilisateur simple ne voit que les documents publics ou les siens
            if (!isAdmin(requester)) {
                predicates.add(cb.or(
                        cb.equal(root.get("visibility"), Visibility.PUBLIC),
                        cb.equal(root.get("owner").get("id"), requester.getId())
                ));
            }

            if (mine) {
                predicates.add(cb.equal(root.get("owner").get("id"), requester.getId()));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (extension != null && !extension.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("extension")), extension.trim().toLowerCase(Locale.ROOT)));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("originalFileName")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("tags")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return documentRepository.findAll(spec, pageable);
    }

    @Override
    public Document findAccessible(Long id, User requester) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Document introuvable"));
        if (document.getStatus() == DocumentStatus.DELETED) {
            throw ApiException.notFound("Document introuvable");
        }
        boolean owner = document.getOwner() != null && document.getOwner().getId().equals(requester.getId());
        if (!isAdmin(requester) && !owner && document.getVisibility() != Visibility.PUBLIC) {
            throw ApiException.forbidden("Vous n'avez pas accès à ce document");
        }
        return document;
    }

    @Override
    @Transactional
    public Document updateMetadata(Long id, DocumentUpdateRequest request, User requester) {
        Document document = requireOwnerOrAdmin(id, requester, "modifier");
        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw ApiException.badRequest("Un document archivé n'est plus modifiable");
        }

        if (request.title() != null && !request.title().isBlank()) {
            document.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            document.setDescription(request.description());
        }
        if (request.tags() != null) {
            document.setTags(request.tags());
        }
        if (request.visibility() != null && !request.visibility().isBlank()) {
            document.setVisibility(parseVisibility(request.visibility()));
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.badRequest("Catégorie introuvable"));
            document.setCategory(category);
        }

        document = documentRepository.save(document);
        record(document, requester, ActionType.UPDATE, "Modification des métadonnées");
        return document;
    }

    @Override
    @Transactional
    public void softDelete(Long id, User requester) {
        Document document = requireOwnerOrAdmin(id, requester, "supprimer");
        document.setStatus(DocumentStatus.DELETED);
        document.setDeletedAt(LocalDateTime.now());
        documentRepository.save(document);
        record(document, requester, ActionType.DELETE, "Suppression logique");
    }

    @Override
    @Transactional
    public Document archive(Long id, User requester) {
        Document document = requireOwnerOrAdmin(id, requester, "archiver");
        if (document.getStatus() != DocumentStatus.ACTIVE) {
            throw ApiException.badRequest("Seul un document actif peut être archivé");
        }
        document.setStatus(DocumentStatus.ARCHIVED);
        document = documentRepository.save(document);
        record(document, requester, ActionType.ARCHIVE, "Archivage");
        return document;
    }

    @Override
    @Transactional
    public Document restore(Long id, User requester) {
        if (!isAdmin(requester)) {
            throw ApiException.forbidden("Seul un administrateur peut restaurer un document");
        }
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Document introuvable"));
        if (document.getStatus() != DocumentStatus.ARCHIVED) {
            throw ApiException.badRequest("Seul un document archivé peut être restauré");
        }
        document.setStatus(DocumentStatus.ACTIVE);
        document = documentRepository.save(document);
        record(document, requester, ActionType.RESTORE, "Restauration");
        return document;
    }

    @Override
    @Transactional
    public Resource download(Long id, User requester) {
        Document document = findAccessible(id, requester);
        Resource resource = storage.load(document.getStoredFileName());
        record(document, requester, ActionType.DOWNLOAD, "Téléchargement");
        return resource;
    }

    @Override
    public DashboardStatsDto stats(User requester) {
        boolean admin = isAdmin(requester);

        long total = admin
                ? documentRepository.countByStatusNot(DocumentStatus.DELETED)
                : documentRepository.count(visibleSpec(requester));
        long mine = documentRepository.countByOwnerAndStatusNot(requester, DocumentStatus.DELETED);
        long storageUsed = admin
                ? documentRepository.sumSizeByStatusNot(DocumentStatus.DELETED)
                : documentRepository.sumSizeByOwnerAndStatusNot(requester, DocumentStatus.DELETED);

        List<Category> categories = categoryRepository.findAll();
        List<Map<String, Object>> perCategory = new ArrayList<>();
        long activeCategories = 0;
        for (Category category : categories) {
            if (Boolean.TRUE.equals(category.getActive())) {
                activeCategories++;
            }
            long count = documentRepository.countByCategoryAndStatusNot(category, DocumentStatus.DELETED);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", category.getId());
            entry.put("name", category.getName());
            entry.put("count", count);
            perCategory.add(entry);
        }

        List<Document> recent = admin
                ? documentRepository.findTop5ByStatusNotOrderByCreatedAtDesc(DocumentStatus.DELETED)
                : documentRepository.findAll(visibleSpec(requester),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        List<HistoryDto> recentActions = historyRepository
                .findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "actionDate")))
                .map(HistoryDto::from)
                .getContent();

        return new DashboardStatsDto(
                total,
                mine,
                activeCategories,
                storageUsed,
                perCategory,
                recent.stream().map(DocumentDto::from).toList(),
                recentActions
        );
    }

    private Specification<Document> visibleSpec(User requester) {
        return (root, cq, cb) -> cb.and(
                cb.notEqual(root.get("status"), DocumentStatus.DELETED),
                cb.or(
                        cb.equal(root.get("visibility"), Visibility.PUBLIC),
                        cb.equal(root.get("owner").get("id"), requester.getId())
                )
        );
    }

    private Document requireOwnerOrAdmin(Long id, User requester, String action) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Document introuvable"));
        if (document.getStatus() == DocumentStatus.DELETED) {
            throw ApiException.notFound("Document introuvable");
        }
        boolean owner = document.getOwner() != null && document.getOwner().getId().equals(requester.getId());
        if (!isAdmin(requester) && !owner) {
            throw ApiException.forbidden("Vous n'avez pas le droit de " + action + " ce document");
        }
        return document;
    }

    private void record(Document document, User user, ActionType action, String comment) {
        historyRepository.save(DocumentHistory.builder()
                .document(document)
                .user(user)
                .action(action)
                .comment(comment)
                .build());
    }

    private static Visibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return Visibility.PRIVATE;
        }
        try {
            return Visibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Visibilité invalide : " + value);
        }
    }

    private static DocumentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return DocumentStatus.ACTIVE;
        }
        try {
            DocumentStatus status = DocumentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (status == DocumentStatus.DELETED) {
                return DocumentStatus.ACTIVE;
            }
            return status;
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Statut invalide : " + value);
        }
    }
}
