package com.docportal.docportal.repositories;

import com.docportal.docportal.entities.Category;
import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.User;
import com.docportal.docportal.enums.DocumentStatus;
import com.docportal.docportal.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findByOwner(User owner);

    List<Document> findByCategory(Category category);

    List<Document> findByStatus(DocumentStatus status);

    List<Document> findByVisibility(Visibility visibility);

    long countByStatusNot(DocumentStatus status);

    long countByOwnerAndStatusNot(User owner, DocumentStatus status);

    long countByCategoryAndStatusNot(Category category, DocumentStatus status);

    List<Document> findTop5ByStatusNotOrderByCreatedAtDesc(DocumentStatus status);

    List<Document> findTop5ByOwnerAndStatusNotOrderByCreatedAtDesc(User owner, DocumentStatus status);

    @Query("select coalesce(sum(d.size), 0) from Document d where d.status <> :excluded")
    long sumSizeByStatusNot(DocumentStatus excluded);

    @Query("select coalesce(sum(d.size), 0) from Document d where d.owner = :owner and d.status <> :excluded")
    long sumSizeByOwnerAndStatusNot(User owner, DocumentStatus excluded);
}
