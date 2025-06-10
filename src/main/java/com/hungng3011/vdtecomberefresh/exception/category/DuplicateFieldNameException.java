package com.hungng3011.vdtecomberefresh.exception.category;

import com.hungng3011.vdtecomberefresh.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a duplicate field name within the same category
 */
public class DuplicateFieldNameException extends BaseBusinessException {
    
    public DuplicateFieldNameException(String fieldName, Long categoryId) {
        super("DUPLICATE_FIELD_NAME", 
              String.format("Field name '%s' already exists in category %s", fieldName, categoryId), 
              HttpStatus.BAD_REQUEST, categoryId);
    }
    
    public DuplicateFieldNameException(String fieldName) {
        super("DUPLICATE_FIELD_NAME", 
              String.format("Field name '%s' already exists in this category", fieldName), 
              HttpStatus.BAD_REQUEST);
    }
}
