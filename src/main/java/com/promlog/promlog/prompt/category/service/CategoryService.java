package com.promlog.promlog.prompt.category.service;

import com.promlog.promlog.prompt.category.dto.CategoryResponse;
import com.promlog.promlog.prompt.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
