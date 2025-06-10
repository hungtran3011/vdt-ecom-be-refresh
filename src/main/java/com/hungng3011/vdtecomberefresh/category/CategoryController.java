package com.hungng3011.vdtecomberefresh.category;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> getAll() {
        log.info("Fetching all categories");
        try {
            List<CategoryDto> categories = categoryService.getAll();
            log.info("Successfully retrieved {} categories", categories.size());
            return categories;
        } catch (Exception e) {
            log.error("Error fetching all categories", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable Long id) {
        log.info("Fetching category with ID: {}", id);
        try {
            CategoryDto dto = categoryService.getById(id);
            if (dto != null) {
                log.info("Successfully retrieved category with ID: {}", id);
                return ResponseEntity.ok(dto);
            } else {
                log.warn("Category not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching category with ID: {}", id, e);
            throw e;
        }
    }

    @PostMapping
    public CategoryDto create(@Valid @RequestBody CategoryDto req) {
        log.info("Creating new category: {}", req.getName());
        try {
            CategoryDto result = categoryService.create(req);
            log.info("Successfully created category with ID: {} and name: {}", result.getId(), result.getName());
            return result;
        } catch (Exception e) {
            log.error("Error creating category: {}", req.getName(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<CategoryDto> update(@PathVariable Long id, @Valid @RequestBody CategoryDto req) {
        log.info("Updating category with ID: {} and name: {}", id, req.getName());
        try {
            // Set the ID from the path variable onto the request DTO
            req.setId(id);
            CategoryDto result = categoryService.update(req);
            if (result == null) {
                log.warn("Failed to update category with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.info("Successfully updated category with ID: {}", result.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating category with ID: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        log.info("Deleting category with ID: {}", id);
        try {
            categoryService.delete(id);
            log.info("Successfully deleted category with ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting category with ID: {}", id, e);
            throw e;
        }
    }
}
