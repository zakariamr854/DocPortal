package com.docportal.docportal.repositories;

import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentOrderByVersionNumberDesc(Document document);

    long countByDocument(Document document);

    Optional<DocumentVersion> findByIdAndDocument(Long id, Document document);

    @Query("select coalesce(max(v.versionNumber), 0) from DocumentVersion v where v.document = :document")
    int maxVersionNumber(Document document);
}
