package com.docportal.docportal.controllers;

import com.docportal.docportal.dto.DashboardStatsDto;
import com.docportal.docportal.dto.DocumentDto;
import com.docportal.docportal.dto.DocumentUpdateRequest;
import com.docportal.docportal.dto.HistoryDto;
import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.User;
import com.docportal.docportal.exceptions.ApiException;
import com.docportal.docportal.repositories.UserRepository;
import com.docportal.docportal.services.DocumentHistoryService;
import com.docportal.docportal.services.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentHistoryService historyService;
    private final UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> ApiException.forbidden("Utilisateur introuvable"));
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentDto> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String title,
                                              @RequestParam(required = false) String description,
                                              @RequestParam(required = false) String tags,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) String visibility,
                                              Authentication authentication) {
        User user = currentUser(authentication);
        Document document = documentService.upload(file, title, description, tags, categoryId, visibility, user);
        return ResponseEntity.ok(DocumentDto.from(document));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String extension,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "false") boolean mine,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(defaultValue = "createdAt") String sort,
                                                    @RequestParam(defaultValue = "desc") String dir,
                                                    Authentication authentication) {
        User user = currentUser(authentication);

        String sortProperty = switch (sort) {
            case "name" -> "title";
            case "size" -> "size";
            case "category" -> "category.name";
            default -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<Document> result = documentService.search(q, categoryId, extension, status, mine, user,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, sortProperty)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", result.getContent().stream().map(DocumentDto::from).toList());
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> stats(Authentication authentication) {
        return ResponseEntity.ok(documentService.stats(currentUser(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> detail(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(DocumentDto.from(documentService.findAccessible(id, currentUser(authentication))));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Document document = documentService.findAccessible(id, user);
        Resource resource = documentService.download(id, user);

        String fileName = URLEncoder.encode(document.getOriginalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentType = document.getMimeType() != null
                ? document.getMimeType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<HistoryDto>> history(@PathVariable Long id, Authentication authentication) {
        // vérifie l'accès avant d'exposer l'historique
        documentService.findAccessible(id, currentUser(authentication));
        return ResponseEntity.ok(historyService.findByDocument(id).stream()
                .map(HistoryDto::from)
                .sorted((a, b) -> b.actionDate().compareTo(a.actionDate()))
                .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDto> update(@PathVariable Long id,
                                              @RequestBody DocumentUpdateRequest request,
                                              Authentication authentication) {
        return ResponseEntity.ok(DocumentDto.from(
                documentService.updateMetadata(id, request, currentUser(authentication))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, Authentication authentication) {
        documentService.softDelete(id, currentUser(authentication));
        return ResponseEntity.ok(Map.of("message", "Document supprimé"));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<DocumentDto> archive(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(DocumentDto.from(documentService.archive(id, currentUser(authentication))));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<DocumentDto> restore(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(DocumentDto.from(documentService.restore(id, currentUser(authentication))));
    }
}
