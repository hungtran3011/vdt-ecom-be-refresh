package com.hungng3011.vdtecomberefresh.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDto;
import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDynamicFieldDto;
import com.hungng3011.vdtecomberefresh.category.enums.AppliesTo;
import com.hungng3011.vdtecomberefresh.category.enums.FieldType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class CategoryEditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryService categoryService;

    @Test
    public void testCategoryEditWithNewDynamicField() throws Exception {
        // First, create a category
        CategoryDto createRequest = new CategoryDto();
        createRequest.setName("Điện thoại");
        
        List<CategoryDynamicFieldDto> initialFields = new ArrayList<>();
        CategoryDynamicFieldDto existingField = new CategoryDynamicFieldDto();
        existingField.setFieldName("Màu sắc");
        existingField.setFieldType(FieldType.TEXT);
        existingField.setAppliesTo(AppliesTo.PRODUCT);
        existingField.setRequired(false);
        initialFields.add(existingField);
        
        createRequest.setDynamicFields(initialFields);
        
        String createJson = objectMapper.writeValueAsString(createRequest);
        
        String createResponse = mockMvc.perform(post("/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        CategoryDto createdCategory = objectMapper.readValue(createResponse, CategoryDto.class);
        Long categoryId = createdCategory.getId();
        
        // Now test the edit with id=0 for new fields (the scenario that was failing)
        CategoryDto updateRequest = new CategoryDto();
        updateRequest.setId(categoryId);
        updateRequest.setName("Điện thoại");
        
        List<CategoryDynamicFieldDto> updateFields = new ArrayList<>();
        
        // Keep the existing field
        CategoryDynamicFieldDto keepExisting = new CategoryDynamicFieldDto();
        keepExisting.setId(createdCategory.getDynamicFields().get(0).getId());
        keepExisting.setFieldName("Màu sắc");
        keepExisting.setFieldType(FieldType.TEXT);
        keepExisting.setAppliesTo(AppliesTo.PRODUCT);
        keepExisting.setRequired(false);
        updateFields.add(keepExisting);
        
        // Add new field with id=0 (this is what was failing before)
        CategoryDynamicFieldDto newField = new CategoryDynamicFieldDto();
        newField.setId(0L); // This is the key - frontend sends id=0 for new fields
        newField.setFieldName("Hãng");
        newField.setFieldType(FieldType.TEXT);
        newField.setAppliesTo(AppliesTo.PRODUCT);
        newField.setRequired(false);
        updateFields.add(newField);
        
        updateRequest.setDynamicFields(updateFields);
        
        String updateJson = objectMapper.writeValueAsString(updateRequest);
        
        // This should succeed now
        String response = mockMvc.perform(put("/v1/categories/" + categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andDo(result -> {
                    if (result.getResponse().getStatus() != 200) {
                        System.out.println("Error response: " + result.getResponse().getContentAsString());
                        System.out.println("Status: " + result.getResponse().getStatus());
                    } else {
                        System.out.println("Success response: " + result.getResponse().getContentAsString());
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dynamicFields").isArray())
                .andExpect(jsonPath("$.dynamicFields.length()").value(2))
                .andExpect(jsonPath("$.dynamicFields[?(@.fieldName == 'Hãng')]").exists())
                .andExpect(jsonPath("$.dynamicFields[?(@.fieldName == 'Màu sắc')]").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Parse and validate the detailed response
        CategoryDto updatedCategory = objectMapper.readValue(response, CategoryDto.class);
        
        // Stringent validation
        assertThat(updatedCategory.getId()).isEqualTo(categoryId);
        assertThat(updatedCategory.getName()).isEqualTo("Điện thoại");
        assertThat(updatedCategory.getDynamicFields()).hasSize(2);
        assertThat(updatedCategory.getProductCount()).isEqualTo(0L);
        
        // Validate existing field is preserved with proper ID
        CategoryDynamicFieldDto existingFieldResult = updatedCategory.getDynamicFields().stream()
                .filter(f -> "Màu sắc".equals(f.getFieldName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Existing field 'Màu sắc' should be preserved"));
        
        assertThat(existingFieldResult.getId()).isNotNull();
        assertThat(existingFieldResult.getId()).isNotEqualTo(0L);
        assertThat(existingFieldResult.getId()).isGreaterThan(0L);
        assertThat(existingFieldResult.getFieldType()).isEqualTo(FieldType.TEXT);
        assertThat(existingFieldResult.getAppliesTo()).isEqualTo(AppliesTo.PRODUCT);
        assertThat(existingFieldResult.isRequired()).isFalse();
        
        // Validate new field was created with proper ID (not 0)
        CategoryDynamicFieldDto newFieldResult = updatedCategory.getDynamicFields().stream()
                .filter(f -> "Hãng".equals(f.getFieldName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("New field 'Hãng' should be created"));
        
        assertThat(newFieldResult.getId()).isNotNull();
        assertThat(newFieldResult.getId()).isNotEqualTo(0L);
        assertThat(newFieldResult.getId()).isGreaterThan(0L);
        assertThat(newFieldResult.getFieldType()).isEqualTo(FieldType.TEXT);
        assertThat(newFieldResult.getAppliesTo()).isEqualTo(AppliesTo.PRODUCT);
        assertThat(newFieldResult.isRequired()).isFalse();
        
        // Ensure IDs are different
        assertThat(existingFieldResult.getId()).isNotEqualTo(newFieldResult.getId());
    }

    @Test
    public void testCategoryEditWithDuplicateFieldNames() throws Exception {
        // First, create a category
        CategoryDto createRequest = new CategoryDto();
        createRequest.setName("Test Category");
        createRequest.setDynamicFields(new ArrayList<>());
        
        String createJson = objectMapper.writeValueAsString(createRequest);
        
        String createResponse = mockMvc.perform(post("/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        CategoryDto createdCategory = objectMapper.readValue(createResponse, CategoryDto.class);
        Long categoryId = createdCategory.getId();
        
        // Now test the edit with duplicate field names (should fail)
        CategoryDto updateRequest = new CategoryDto();
        updateRequest.setId(categoryId);
        updateRequest.setName("Test Category");
        
        List<CategoryDynamicFieldDto> updateFields = new ArrayList<>();
        
        // Add first field
        CategoryDynamicFieldDto field1 = new CategoryDynamicFieldDto();
        field1.setId(0L);
        field1.setFieldName("Hãng");
        field1.setFieldType(FieldType.TEXT);
        field1.setAppliesTo(AppliesTo.PRODUCT);
        field1.setRequired(false);
        updateFields.add(field1);
        
        // Add duplicate field name (should cause validation error)
        CategoryDynamicFieldDto field2 = new CategoryDynamicFieldDto();
        field2.setId(0L);
        field2.setFieldName("Hãng"); // Same name as field1
        field2.setFieldType(FieldType.TEXT);
        field2.setAppliesTo(AppliesTo.VARIATION);
        field2.setRequired(false);
        updateFields.add(field2);
        
        updateRequest.setDynamicFields(updateFields);
        
        String updateJson = objectMapper.writeValueAsString(updateRequest);
        
        // This should fail with duplicate field name error
        mockMvc.perform(put("/v1/categories/" + categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andDo(result -> {
                    if (result.getResponse().getStatus() != 400) {
                        System.out.println("Error response: " + result.getResponse().getContentAsString());
                        System.out.println("Status: " + result.getResponse().getStatus());
                    }
                })
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_FIELD_NAME"));
    }
    
    @Test
    public void testCategoryEditWithMultipleNewFields() throws Exception {
        // Create a category with one field
        CategoryDto createRequest = new CategoryDto();
        createRequest.setName("Đồ gia dụng");
        
        List<CategoryDynamicFieldDto> initialFields = new ArrayList<>();
        CategoryDynamicFieldDto initialField = new CategoryDynamicFieldDto();
        initialField.setFieldName("Chất liệu");
        initialField.setFieldType(FieldType.TEXT);
        initialField.setAppliesTo(AppliesTo.PRODUCT);
        initialField.setRequired(false);
        initialFields.add(initialField);
        
        createRequest.setDynamicFields(initialFields);
        
        String createJson = objectMapper.writeValueAsString(createRequest);
        
        String createResponse = mockMvc.perform(post("/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        CategoryDto createdCategory = objectMapper.readValue(createResponse, CategoryDto.class);
        Long categoryId = createdCategory.getId();
        
        // Update with multiple new fields (all with id=0)
        CategoryDto updateRequest = new CategoryDto();
        updateRequest.setId(categoryId);
        updateRequest.setName("Đồ gia dụng");
        
        List<CategoryDynamicFieldDto> updateFields = new ArrayList<>();
        
        // Keep existing field
        CategoryDynamicFieldDto keepField = new CategoryDynamicFieldDto();
        keepField.setId(createdCategory.getDynamicFields().get(0).getId());
        keepField.setFieldName("Chất liệu");
        keepField.setFieldType(FieldType.TEXT);
        keepField.setAppliesTo(AppliesTo.PRODUCT);
        keepField.setRequired(false);
        updateFields.add(keepField);
        
        // Add multiple new fields with id=0
        CategoryDynamicFieldDto newField1 = new CategoryDynamicFieldDto();
        newField1.setId(0L);
        newField1.setFieldName("Kích thước");
        newField1.setFieldType(FieldType.TEXT);
        newField1.setAppliesTo(AppliesTo.PRODUCT);
        newField1.setRequired(true);
        updateFields.add(newField1);
        
        CategoryDynamicFieldDto newField2 = new CategoryDynamicFieldDto();
        newField2.setId(0L);
        newField2.setFieldName("Trọng lượng");
        newField2.setFieldType(FieldType.NUMBER);
        newField2.setAppliesTo(AppliesTo.PRODUCT);
        newField2.setRequired(false);
        updateFields.add(newField2);
        
        CategoryDynamicFieldDto newField3 = new CategoryDynamicFieldDto();
        newField3.setId(0L);
        newField3.setFieldName("Bảo hành");
        newField3.setFieldType(FieldType.TEXT);
        newField3.setAppliesTo(AppliesTo.PRODUCT);
        newField3.setRequired(true);
        updateFields.add(newField3);
        
        updateRequest.setDynamicFields(updateFields);
        
        String updateJson = objectMapper.writeValueAsString(updateRequest);
        
        // This should succeed
        String updateResponse = mockMvc.perform(put("/v1/categories/" + categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andDo(result -> {
                    System.out.println("Multiple New Fields Response: " + result.getResponse().getContentAsString());
                })
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        CategoryDto updatedCategory = objectMapper.readValue(updateResponse, CategoryDto.class);
        
        // Validate all fields are present
        assertThat(updatedCategory.getDynamicFields()).hasSize(4);
        
        // Validate each field has a proper ID (not null, not 0)
        for (CategoryDynamicFieldDto field : updatedCategory.getDynamicFields()) {
            assertThat(field.getId()).isNotNull();
            assertThat(field.getId()).isNotEqualTo(0L);
            assertThat(field.getId()).isGreaterThan(0L);
            assertThat(field.getFieldName()).isNotBlank();
        }
        
        // Validate specific fields exist
        List<String> fieldNames = updatedCategory.getDynamicFields().stream()
                .map(CategoryDynamicFieldDto::getFieldName)
                .collect(Collectors.toList());
        
        assertThat(fieldNames).containsExactlyInAnyOrder(
                "Chất liệu", "Kích thước", "Trọng lượng", "Bảo hành"
        );
    }
    
    @Test
    public void testCategoryEditWithNullIdFields() throws Exception {
        // Create a category with one field
        CategoryDto createRequest = new CategoryDto();
        createRequest.setName("Test Null ID");
        createRequest.setDynamicFields(new ArrayList<>());
        
        String createJson = objectMapper.writeValueAsString(createRequest);
        
        String createResponse = mockMvc.perform(post("/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        CategoryDto createdCategory = objectMapper.readValue(createResponse, CategoryDto.class);
        Long categoryId = createdCategory.getId();
        
        // Update with fields that have null ID (should be treated as new)
        CategoryDto updateRequest = new CategoryDto();
        updateRequest.setId(categoryId);
        updateRequest.setName("Test Null ID");
        
        List<CategoryDynamicFieldDto> updateFields = new ArrayList<>();
        
        // Add field with null ID (should be treated as new)
        CategoryDynamicFieldDto nullIdField = new CategoryDynamicFieldDto();
        nullIdField.setId(null); // Explicitly null
        nullIdField.setFieldName("Null ID Field");
        nullIdField.setFieldType(FieldType.TEXT);
        nullIdField.setAppliesTo(AppliesTo.PRODUCT);
        nullIdField.setRequired(false);
        updateFields.add(nullIdField);
        
        updateRequest.setDynamicFields(updateFields);
        
        String updateJson = objectMapper.writeValueAsString(updateRequest);
        
        // This should succeed
        String updateResponse = mockMvc.perform(put("/v1/categories/" + categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andDo(result -> {
                    System.out.println("Null ID Field Response: " + result.getResponse().getContentAsString());
                })
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        CategoryDto updatedCategory = objectMapper.readValue(updateResponse, CategoryDto.class);
        
        // Validate field was created with proper ID
        assertThat(updatedCategory.getDynamicFields()).hasSize(1);
        CategoryDynamicFieldDto resultField = updatedCategory.getDynamicFields().get(0);
        assertThat(resultField.getId()).isNotNull();
        assertThat(resultField.getId()).isGreaterThan(0L);
        assertThat(resultField.getFieldName()).isEqualTo("Null ID Field");
    }
    
    @Test
    public void testCategoryEditReplaceAllFields() throws Exception {
        // Create a category with multiple fields
        CategoryDto createRequest = new CategoryDto();
        createRequest.setName("Thời trang");
        
        List<CategoryDynamicFieldDto> initialFields = new ArrayList<>();
        
        CategoryDynamicFieldDto field1 = new CategoryDynamicFieldDto();
        field1.setFieldName("Size");
        field1.setFieldType(FieldType.TEXT);
        field1.setAppliesTo(AppliesTo.PRODUCT);
        field1.setRequired(true);
        initialFields.add(field1);
        
        CategoryDynamicFieldDto field2 = new CategoryDynamicFieldDto();
        field2.setFieldName("Màu sắc");
        field2.setFieldType(FieldType.TEXT);
        field2.setAppliesTo(AppliesTo.PRODUCT);
        field2.setRequired(false);
        initialFields.add(field2);
        
        createRequest.setDynamicFields(initialFields);
        
        String createJson = objectMapper.writeValueAsString(createRequest);
        
        String createResponse = mockMvc.perform(post("/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        CategoryDto createdCategory = objectMapper.readValue(createResponse, CategoryDto.class);
        Long categoryId = createdCategory.getId();
        
        // Now update to remove all existing fields and add completely new ones
        CategoryDto updateRequest = new CategoryDto();
        updateRequest.setId(categoryId);
        updateRequest.setName("Thời trang");
        
        List<CategoryDynamicFieldDto> updateFields = new ArrayList<>();
        
        // Don't include any existing fields (they should be removed)
        // Add only new fields with id=0
        CategoryDynamicFieldDto newField1 = new CategoryDynamicFieldDto();
        newField1.setId(0L);
        newField1.setFieldName("Thương hiệu");
        newField1.setFieldType(FieldType.TEXT);
        newField1.setAppliesTo(AppliesTo.PRODUCT);
        newField1.setRequired(true);
        updateFields.add(newField1);
        
        CategoryDynamicFieldDto newField2 = new CategoryDynamicFieldDto();
        newField2.setId(0L);
        newField2.setFieldName("Giá");
        newField2.setFieldType(FieldType.NUMBER);
        newField2.setAppliesTo(AppliesTo.PRODUCT);
        newField2.setRequired(true);
        updateFields.add(newField2);
        
        updateRequest.setDynamicFields(updateFields);
        
        String updateJson = objectMapper.writeValueAsString(updateRequest);
        
        // This should succeed - old fields removed, new fields added
        String updateResponse = mockMvc.perform(put("/v1/categories/" + categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andDo(result -> {
                    System.out.println("Replace All Fields Response: " + result.getResponse().getContentAsString());
                })
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        CategoryDto updatedCategory = objectMapper.readValue(updateResponse, CategoryDto.class);
        
        // Validate only the new fields are present
        assertThat(updatedCategory.getDynamicFields()).hasSize(2);
        
        List<String> fieldNames = updatedCategory.getDynamicFields().stream()
                .map(CategoryDynamicFieldDto::getFieldName)
                .collect(Collectors.toList());
        
        assertThat(fieldNames).containsExactlyInAnyOrder("Thương hiệu", "Giá");
        
        // Validate old fields are gone
        assertThat(fieldNames).doesNotContain("Size", "Màu sắc");
        
        // Validate all fields have proper IDs
        for (CategoryDynamicFieldDto field : updatedCategory.getDynamicFields()) {
            assertThat(field.getId()).isNotNull();
            assertThat(field.getId()).isNotEqualTo(0L);
            assertThat(field.getId()).isGreaterThan(0L);
        }
    }
}
