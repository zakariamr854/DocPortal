package com.docportal.docportal.repositories;

import com.docportal.docportal.entities.Category;
import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.User;
import com.docportal.docportal.enums.DocumentStatus;
import com.docportal.docportal.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwner(User owner);

    List<Document> findByCategory(Category category);

    List<Document> findByStatus(DocumentStatus status);

    List<Document> findByVisibility(Visibility visibility);

}
