package com.docportal.docportal.services;

import com.docportal.docportal.entities.DocumentHistory;

import java.util.List;

public interface DocumentHistoryService {

    DocumentHistory save(DocumentHistory history);

    List<DocumentHistory> findAll();

    List<DocumentHistory> findByDocument(Long documentId);

}