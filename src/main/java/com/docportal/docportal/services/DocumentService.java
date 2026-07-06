package com.docportal.docportal.services;

import com.docportal.docportal.dto.DashboardStatsDto;
import com.docportal.docportal.dto.DocumentUpdateRequest;
import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.User;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    Document upload(MultipartFile file, String title, String description, String tags,
                    Long categoryId, String visibility, User owner);

    Page<Document> search(String query, Long categoryId, String extension, String status,
                          boolean mine, User requester, Pageable pageable);

    Document findAccessible(Long id, User requester);

    Document updateMetadata(Long id, DocumentUpdateRequest request, User requester);

    void softDelete(Long id, User requester);

    Document archive(Long id, User requester);

    Document restore(Long id, User requester);

    Resource download(Long id, User requester);

    DashboardStatsDto stats(User requester);
}
