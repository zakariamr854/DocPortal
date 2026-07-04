package com.docportal.docportal.services;

import com.docportal.docportal.entities.Category;

import java.util.List;

public interface CategoryService {

    Category save(Category category);

    Category update(Long id, Category category);

    void delete(Long id);

    List<Category> findAll();

    Category findById(Long id);

}
