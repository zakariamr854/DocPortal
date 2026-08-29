package com.docportal.docportal.services;

import com.docportal.docportal.entities.Category;
import com.docportal.docportal.enums.DocumentStatus;
import com.docportal.docportal.exceptions.ApiException;
import com.docportal.docportal.repositories.CategoryRepository;
import com.docportal.docportal.repositories.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;

    @Override
    public Category save(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw ApiException.badRequest("Le nom de la catégorie est obligatoire");
        }
        if (categoryRepository.existsByName(category.getName().trim())) {
            throw ApiException.badRequest("Cette catégorie existe déjà");
        }
        category.setName(category.getName().trim());
        if (category.getActive() == null) {
            category.setActive(true);
        }
        return categoryRepository.save(category);
    }

    @Override
    public Category update(Long id, Category category) {
        Category existing = findById(id);
        if (category.getName() != null && !category.getName().isBlank()) {
            String newName = category.getName().trim();
            if (!newName.equalsIgnoreCase(existing.getName()) && categoryRepository.existsByName(newName)) {
                throw ApiException.badRequest("Une catégorie porte déjà ce nom");
            }
            existing.setName(newName);
        }
        if (category.getDescription() != null) {
            existing.setDescription(category.getDescription());
        }
        if (category.getActive() != null) {
            existing.setActive(category.getActive());
        }
        return categoryRepository.save(existing);
    }


    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        long used = documentRepository.countByCategoryAndStatusNot(category, DocumentStatus.DELETED);
        if (used > 0) {
            category.setActive(false);
            categoryRepository.save(category);
        } else {
            categoryRepository.delete(category);
        }
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Catégorie introuvable"));
    }

    public long documentCount(Category category) {
        return documentRepository.countByCategoryAndStatusNot(category, DocumentStatus.DELETED);
    }
}
