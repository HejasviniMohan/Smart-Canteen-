package com.canteen.service;

import com.canteen.dto.CategoryDtos;
import com.canteen.entity.Category;
import com.canteen.exception.ApiException;
import com.canteen.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repo;

    @Transactional(readOnly = true)
    public List<CategoryDtos.Response> findAll() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDtos.Response findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public CategoryDtos.Response create(CategoryDtos.CreateRequest req) {
        if (repo.existsByName(req.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Category already exists: " + req.getName());
        }
        Category c = Category.builder()
                .name(req.getName())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .displayOrder(req.getDisplayOrder() == null ? 0 : req.getDisplayOrder())
                .build();
        return toDto(repo.save(c));
    }

    @Transactional
    public CategoryDtos.Response update(Long id, CategoryDtos.UpdateRequest req) {
        Category c = getOrThrow(id);
        if (req.getName() != null && !req.getName().equals(c.getName()) && repo.existsByName(req.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Category name already in use");
        }
        if (req.getName() != null)        c.setName(req.getName());
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getImageUrl() != null)    c.setImageUrl(req.getImageUrl());
        if (req.getDisplayOrder() != null) c.setDisplayOrder(req.getDisplayOrder());
        return toDto(repo.save(c));
    }

    @Transactional
    public void delete(Long id) {
        Category c = getOrThrow(id);
        repo.delete(c);
    }

    private Category getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found: " + id));
    }

    private CategoryDtos.Response toDto(Category c) {
        return CategoryDtos.Response.builder()
                .id(c.getId()).name(c.getName())
                .description(c.getDescription()).imageUrl(c.getImageUrl())
                .displayOrder(c.getDisplayOrder())
                .build();
    }
}