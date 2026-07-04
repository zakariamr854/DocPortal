package com.docportal.docportal.services;

import com.docportal.docportal.entities.Document;

import java.util.List;

public interface DocumentService {

    Document save(Document document);

    Document update(Long id, Document document);

    void delete(Long id);

    List<Document> findAll();

    Document findById(Long id);

}
