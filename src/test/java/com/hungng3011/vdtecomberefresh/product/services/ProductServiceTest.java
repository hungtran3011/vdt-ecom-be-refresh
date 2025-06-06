package com.hungng3011.vdtecomberefresh.product.services;

import com.hungng3011.vdtecomberefresh.category.dtos.CategoryDynamicFieldDto;
import com.hungng3011.vdtecomberefresh.category.entities.Category;
import com.hungng3011.vdtecomberefresh.category.entities.CategoryDynamicField;
import com.hungng3011.vdtecomberefresh.category.enums.AppliesTo;
import com.hungng3011.vdtecomberefresh.category.enums.FieldType;
import com.hungng3011.vdtecomberefresh.category.repositories.CategoryRepository;
import com.hungng3011.vdtecomberefresh.product.dtos.*;
import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.entities.ProductDynamicValue;
import com.hungng3011.vdtecomberefresh.product.entities.Variation;
import com.hungng3011.vdtecomberefresh.product.entities.VariationDynamicValue;
import com.hungng3011.vdtecomberefresh.product.mappers.ProductMapper;
import com.hungng3011.vdtecomberefresh.product.repositories.ProductRepository;
import com.hungng3011.vdtecomberefresh.stock.StockService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.hungng3011.vdtecomberefresh.exception.product.ProductProcessingException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private EntityManager entityManager;

    @Mock
    private StockService stockService;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private Product testProduct;
    private ProductDto testProductDto;
    private Category testCategory;
    private CategoryDynamicField field1, field2, field3, field4, field5, field6;

    @BeforeEach
    void setUp() {
        // Set up category
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Smartphones");

        // Set up dynamic fields
        field1 = new CategoryDynamicField();
        field1.setId(1L);
        field1.setFieldName("Screen Size");
        field1.setFieldType(FieldType.TEXT);
        field1.setRequired(true);
        field1.setAppliesTo(AppliesTo.PRODUCT);

        field2 = new CategoryDynamicField();
        field2.setId(2L);
        field2.setFieldName("Processor");
        field2.setFieldType(FieldType.TEXT);
        field2.setRequired(true);
        field2.setAppliesTo(AppliesTo.PRODUCT);

        field3 = new CategoryDynamicField();
        field3.setId(3L);
        field3.setFieldName("Battery");
        field3.setFieldType(FieldType.TEXT);
        field3.setRequired(true);
        field3.setAppliesTo(AppliesTo.PRODUCT);

        field4 = new CategoryDynamicField();
        field4.setId(4L);
        field4.setFieldName("Color Code");
        field4.setFieldType(FieldType.COLOR_HASH);
        field4.setRequired(false);
        field4.setAppliesTo(AppliesTo.VARIATION);

        field5 = new CategoryDynamicField();
        field5.setId(5L);
        field5.setFieldName("Material");
        field5.setFieldType(FieldType.TEXT);
        field5.setRequired(false);
        field5.setAppliesTo(AppliesTo.VARIATION);

        field6 = new CategoryDynamicField();
        field6.setId(6L);
        field6.setFieldName("Capacity");
        field6.setFieldType(FieldType.TEXT);
        field6.setRequired(false);
        field6.setAppliesTo(AppliesTo.VARIATION);

        // Set up basic product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setBasePrice(BigDecimal.valueOf(99.99));
        testProduct.setCategory(testCategory);
        testProduct.setDynamicValues(new ArrayList<>());
        testProduct.setVariations(new ArrayList<>());

        testProductDto = new ProductDto();
        testProductDto.setId(1L);
        testProductDto.setName("Test Product");
        testProductDto.setBasePrice(BigDecimal.valueOf(99.99));
    }

    @Test
    void getAll_ShouldReturnAllProducts() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of(testProduct));
        when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

        // Act
        List<ProductDto> result = productService.getAll();

        // Assert
        assertEquals(1, result.size());
        assertEquals(testProductDto, result.get(0));
        verify(productRepository).findAll();
    }

    @Test
    void getById_WithValidId_ShouldReturnProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

        // Act
        ProductDto result = productService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testProductDto, result);
        verify(productRepository).findById(1L);
    }

    @Test
    void getById_WithInvalidId_ShouldReturnNull() {
        // Arrange
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ProductDto result = productService.getById(99L);

        // Assert
        assertNull(result);
        verify(productRepository).findById(99L);
    }

    @Test
    void delete_WithValidId_ShouldDeleteProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        productService.delete(1L);

        // Assert
        verify(productRepository).delete(testProduct);
        verify(stockService).removeStockByProductId(1L);
    }

    @Test
    void delete_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductProcessingException.class, () -> productService.delete(99L));
    }

    @Test
    void getByCategoryId_WithValidCategoryId_ShouldReturnProducts() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.findByCategory(testCategory)).thenReturn(List.of(testProduct));
        when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

        // Act
        List<ProductDto> result = productService.getByCategoryId(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testProductDto, result.get(0));
    }

    @Test
    void create_CompleteProduct_ShouldCreateProductWithAllRelationships() {
        // Arrange
        // Create request DTO with all components
        ProductDto request = createCompleteProductDto();

        // Mock category lookup
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Mock dynamic field lookups
        when(entityManager.find(eq(CategoryDynamicField.class), eq(1L))).thenReturn(field1);
        when(entityManager.find(eq(CategoryDynamicField.class), eq(2L))).thenReturn(field2);
        when(entityManager.find(eq(CategoryDynamicField.class), eq(3L))).thenReturn(field3);
        when(entityManager.find(eq(CategoryDynamicField.class), eq(4L))).thenReturn(field4);
        when(entityManager.find(eq(CategoryDynamicField.class), eq(5L))).thenReturn(field5);
        when(entityManager.find(eq(CategoryDynamicField.class), eq(6L))).thenReturn(field6);

        // Mock product save
        Product savedProduct = new Product();
        savedProduct.setId(10L);
        savedProduct.setName(request.getName());
        savedProduct.setDescription(request.getDescription());
        savedProduct.setBasePrice(request.getBasePrice());
        savedProduct.setCategory(testCategory);
        savedProduct.setDynamicValues(new ArrayList<>());
        savedProduct.setVariations(new ArrayList<>());

        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(savedProduct);
        
        // Mock final product lookup
        Product finalProduct = new Product();
        finalProduct.setId(10L);
        finalProduct.setName(request.getName());
        finalProduct.setDescription(request.getDescription());
        finalProduct.setBasePrice(request.getBasePrice());
        finalProduct.setCategory(testCategory);
        finalProduct.setDynamicValues(new ArrayList<>());
        finalProduct.setVariations(new ArrayList<>());
        
        // Add some dynamic values to the final product
        ProductDynamicValue pdv1 = new ProductDynamicValue();
        pdv1.setId(1L);
        pdv1.setProduct(finalProduct);
        pdv1.setField(field1);
        pdv1.setValue("6.1 inches");
        finalProduct.getDynamicValues().add(pdv1);
        
        // Add a variation to the final product
        Variation variation = new Variation();
        variation.setId(1L);
        variation.setProduct(finalProduct);
        variation.setName("Natural Titanium");
        variation.setType("color");
        variation.setAdditionalPrice(BigDecimal.ZERO);
        variation.setDynamicValues(new ArrayList<>());
        
        // Add dynamic value to the variation
        VariationDynamicValue vdv1 = new VariationDynamicValue();
        vdv1.setId(1L);
        vdv1.setVariation(variation);
        vdv1.setField(field4);
        vdv1.setValue("#BEBEBE");
        variation.getDynamicValues().add(vdv1);
        
        finalProduct.getVariations().add(variation);
        
        when(productRepository.findById(10L)).thenReturn(Optional.of(finalProduct));
        
        // Mock mapper
        when(productMapper.toDto(finalProduct)).thenReturn(request);

        // Act
        ProductDto result = productService.create(request);

        // Assert
        // Verify basic properties
        assertNotNull(result);
        assertEquals("iPhone 15 Pro", result.getName());
        
        // Verify repository and entity manager interactions
        verify(productRepository, times(1)).saveAndFlush(any(Product.class));
        verify(productRepository, times(1)).findById(10L);
        verify(entityManager, times(request.getDynamicValues().size() + 
                                   getTotalVariationDynamicValues(request)))
            .find(eq(CategoryDynamicField.class), any(Long.class));
        verify(entityManager, atLeastOnce()).flush();
        
        // Verify that the mapper was called with the final product
        verify(productMapper).toDto(finalProduct);
    }

    @Test
    void create_WithInvalidCategoryId_ShouldThrowException() {
        // Arrange
        ProductDto request = new ProductDto();
        request.setName("Test Product");
        request.setCategoryId(99L);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductProcessingException.class, () -> productService.create(request));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void create_WithInvalidDynamicFieldId_ShouldThrowException() {
        // Arrange
        ProductDto request = new ProductDto();
        request.setName("Test Product");
        request.setCategoryId(1L);
        
        CategoryDynamicFieldDto invalidField = new CategoryDynamicFieldDto();
        invalidField.setId(99L);
        invalidField.setFieldName("Invalid Field");
        invalidField.setFieldType(FieldType.TEXT);
        invalidField.setAppliesTo(AppliesTo.PRODUCT);
        
        ProductDynamicValueDto dynamicValue = new ProductDynamicValueDto();
        dynamicValue.setField(invalidField);
        dynamicValue.setValue("Some value");
        
        request.setDynamicValues(List.of(dynamicValue));

        // Mock the necessary dependencies
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        
        // Mock the product save to return a product with ID
        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Test Product");
        savedProduct.setCategory(testCategory);
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(savedProduct);
        
        // Mock entityManager to return null for invalid field ID
        when(entityManager.find(CategoryDynamicField.class, 99L)).thenReturn(null);

        // Act & Assert
        assertThrows(ProductProcessingException.class, () -> productService.create(request));
    }

    @Test
    void update_WithValidProduct_ShouldUpdateProduct() {
        // Arrange
        ProductDto request = new ProductDto();
        request.setId(1L);
        request.setName("Updated Product");
        request.setDescription("Updated description");
        request.setBasePrice(BigDecimal.valueOf(149.99));

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(testProduct);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(request);

        // Act
        ProductDto result = productService.update(request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Product", result.getName());
        verify(productRepository).saveAndFlush(productCaptor.capture());
        assertEquals("Updated Product", productCaptor.getValue().getName());
    }

    @Test
    void update_WithInvalidId_ShouldThrowException() {
        // Arrange
        ProductDto request = new ProductDto();
        request.setId(99L);
        request.setName("Updated Product");

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductProcessingException.class, () -> productService.update(request));
    }

    // Helper methods

    private ProductDto createCompleteProductDto() {
        ProductDto dto = new ProductDto();
        dto.setName("iPhone 15 Pro");
        dto.setDescription("Latest Apple smartphone with advanced features");
        dto.setBasePrice(BigDecimal.valueOf(999.99));
        dto.setImages(Arrays.asList("iphone15_front.jpg", "iphone15_back.jpg", "iphone15_side.jpg"));
        dto.setCategoryId(1L);

        // Add product dynamic values
        List<ProductDynamicValueDto> dynamicValues = new ArrayList<>();
        
        CategoryDynamicFieldDto fieldDto1 = new CategoryDynamicFieldDto();
        fieldDto1.setId(1L);
        fieldDto1.setFieldName("Screen Size");
        fieldDto1.setFieldType(FieldType.TEXT);
        fieldDto1.setRequired(true);
        fieldDto1.setAppliesTo(AppliesTo.PRODUCT);
        ProductDynamicValueDto pv1 = new ProductDynamicValueDto(null, null, fieldDto1, "6.1 inches");
        dynamicValues.add(pv1);
        
        CategoryDynamicFieldDto fieldDto2 = new CategoryDynamicFieldDto();
        fieldDto2.setId(2L);
        fieldDto2.setFieldName("Processor");
        fieldDto2.setFieldType(FieldType.TEXT);
        fieldDto2.setRequired(true);
        fieldDto2.setAppliesTo(AppliesTo.PRODUCT);
        ProductDynamicValueDto pv2 = new ProductDynamicValueDto(null, null, fieldDto2, "A16 Bionic");
        dynamicValues.add(pv2);

        CategoryDynamicFieldDto fieldDto3 = new CategoryDynamicFieldDto();
        fieldDto3.setId(3L);
        fieldDto3.setFieldName("Battery");
        fieldDto3.setFieldType(FieldType.TEXT);
        fieldDto3.setRequired(true);
        fieldDto3.setAppliesTo(AppliesTo.PRODUCT);
        ProductDynamicValueDto pv3 = new ProductDynamicValueDto(null, null, fieldDto3, "3095 mAh");
        dynamicValues.add(pv3);
        
        dto.setDynamicValues(dynamicValues);

        // Add variations
        List<VariationDto> variations = new ArrayList<>();
        
        // Color variation 1
        CategoryDynamicFieldDto colorField = new CategoryDynamicFieldDto();
        colorField.setId(4L);
        colorField.setFieldName("Color Code");
        colorField.setFieldType(FieldType.COLOR_HASH);
        colorField.setRequired(false);
        colorField.setAppliesTo(AppliesTo.VARIATION);

        CategoryDynamicFieldDto materialField = new CategoryDynamicFieldDto();
        materialField.setId(5L);
        materialField.setFieldName("Material");
        materialField.setFieldType(FieldType.TEXT);
        materialField.setRequired(false);
        materialField.setAppliesTo(AppliesTo.VARIATION);
        
        VariationDynamicValueDto vdv1 = new VariationDynamicValueDto(null, null, colorField, "#BEBEBE");
        VariationDynamicValueDto vdv2 = new VariationDynamicValueDto(null, null, materialField, "Titanium");
        
        VariationDto v1 = new VariationDto(null, null, "color", "Natural Titanium", BigDecimal.ZERO, 
                                          Arrays.asList(vdv1, vdv2));
        variations.add(v1);
        
        // Color variation 2
        VariationDynamicValueDto vdv3 = new VariationDynamicValueDto(null, null, colorField, "#0000FF");
        VariationDynamicValueDto vdv4 = new VariationDynamicValueDto(null, null, materialField, "Titanium");
        
        VariationDto v2 = new VariationDto(null, null, "color", "Blue Titanium", BigDecimal.ZERO, 
                                          Arrays.asList(vdv3, vdv4));
        variations.add(v2);
        
        // Storage variations
        CategoryDynamicFieldDto capacityField = new CategoryDynamicFieldDto();
        capacityField.setId(6L);
        capacityField.setFieldName("Capacity");
        capacityField.setFieldType(FieldType.TEXT);
        capacityField.setRequired(false);
        capacityField.setAppliesTo(AppliesTo.VARIATION);
        
        VariationDynamicValueDto vdv5 = new VariationDynamicValueDto(null, null, capacityField, "128 GB");
        VariationDto v3 = new VariationDto(null, null, "storage", "128GB", BigDecimal.ZERO, 
                                          List.of(vdv5));
        variations.add(v3);
        
        VariationDynamicValueDto vdv6 = new VariationDynamicValueDto(null, null, capacityField, "256 GB");
        VariationDto v4 = new VariationDto(null, null, "storage", "256GB", BigDecimal.valueOf(100.00), 
                                          List.of(vdv6));
        variations.add(v4);
        
        dto.setVariations(variations);
        
        return dto;
    }
    
    private int getTotalVariationDynamicValues(ProductDto dto) {
        if (dto.getVariations() == null) {
            return 0;
        }
        
        return dto.getVariations().stream()
            .mapToInt(v -> v.getDynamicValues() == null ? 0 : v.getDynamicValues().size())
            .sum();
    }
}