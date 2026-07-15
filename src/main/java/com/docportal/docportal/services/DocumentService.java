package com.docportal.docportal.services;

import com.docportal.docportal.dto.DashboardStatsDto;
import com.docportal.docportal.dto.DocumentUpdateRequest;
import com.docportal.docportal.dto.VersionDto;
import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.DocumentVersion;
import com.docportal.docportal.entities.User;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    Document upload(MultipartFile file, String title, String description, String tags,
                    Long categoryId, String visibility, User owner);

    Page<Document> search(String query, Long categoryId, String extension, String status,
                          boolean mine, Long ownerId, User requester, Pageable pageable);

    List<User> visibleOwners(User requester);

    Document findAccessible(Long id, User requester);

    Document updateMetadata(Long id, DocumentUpdateRequest request, User requester);

    void softDelete(Long id, User requester);

    Document archive(Long id, User requester);

    Document restore(Long id, User requester);

    Resource download(Long id, User requester);

    DashboardStatsDto stats(User requester);

    List<VersionDto> versions(Long id, User requester);

    Document addVersion(Long id, MultipartFile file, String comment, User requester);

    DocumentVersion findVersion(Long id, Long versionId, User requester);

    Resource downloadVersion(Long id, Long versionId, User requester);

    Document restoreVersion(Long id, Long versionId, User requester);
}
