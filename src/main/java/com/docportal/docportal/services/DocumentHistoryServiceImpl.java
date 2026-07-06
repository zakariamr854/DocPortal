package com.docportal.docportal.services;

import com.docportal.docportal.entities.Document;
import com.docportal.docportal.entities.DocumentHistory;
import com.docportal.docportal.exceptions.ApiException;
import com.docportal.docportal.repositories.DocumentHistoryRepository;
import com.docportal.docportal.repositories.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentHistoryServiceImpl implements DocumentHistoryService {

    private final DocumentHistoryRepository historyRepository;
    private final DocumentRepository documentRepository;

    @Override
    public DocumentHistory save(DocumentHistory history) {
        return historyRepository.save(history);
    }

    @Override
    public List<DocumentHistory> findAll() {
        return historyRepository.findAll();
    }

    @Override
    public List<DocumentHistory> findByDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("Document introuvable"));
        return historyRepository.findByDocument(document);
    }
}
