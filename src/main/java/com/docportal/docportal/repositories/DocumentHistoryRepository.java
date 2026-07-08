package com.docportal.docportal.repositories;

import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.DocumentHistory;
import com.docportal.docportal.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {

    List<DocumentHistory> findByDocument(Document document);

    List<DocumentHistory> findByUser(User user);

    List<DocumentHistory> findTop5ByUserOrderByActionDateDesc(User user);

}
