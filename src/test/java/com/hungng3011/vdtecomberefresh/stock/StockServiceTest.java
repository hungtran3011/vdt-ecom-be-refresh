package com.hungng3011.vdtecomberefresh.stock;

import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.entities.Variation;
import com.hungng3011.vdtecomberefresh.stock.dtos.StockDto;
import com.hungng3011.vdtecomberefresh.stock.dtos.StockHistoryDto;
import com.hungng3011.vdtecomberefresh.stock.entities.Stock;
import com.hungng3011.vdtecomberefresh.stock.entities.StockHistory;
import com.hungng3011.vdtecomberefresh.stock.enums.StockActionState;
import com.hungng3011.vdtecomberefresh.stock.enums.StockStatus;
import com.hungng3011.vdtecomberefresh.stock.mappers.StockHistoryMapper;
import com.hungng3011.vdtecomberefresh.stock.mappers.StockMapper;
import com.hungng3011.vdtecomberefresh.stock.repositories.StockHistoryRepository;
import com.hungng3011.vdtecomberefresh.stock.repositories.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    @Mock
    private StockHistoryMapper stockHistoryMapper;

    @InjectMocks
    private StockService stockService;

    @Captor
    private ArgumentCaptor<Stock> stockCaptor;
    
    @Captor
    private ArgumentCaptor<StockHistory> stockHistoryCaptor;

    private Stock testStock;
    private StockDto testStockDto;
    private Product testProduct;
    private List<Variation> testVariations;
    private StockHistory testStockHistory;
    private StockHistoryDto testStockHistoryDto;

    @BeforeEach
    void setUp() {
        // Set up test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");

        // Set up test variations
        testVariations = new ArrayList<>();
        Variation variation = new Variation();
        variation.setId(1L);
        variation.setName("Test Variation");
        testVariations.add(variation);

        // Set up test stock
        testStock = new Stock();
        testStock.setId(1L);
        testStock.setSku("TEST-SKU-001");
        testStock.setProduct(testProduct);
        testStock.getVariations().add(variation);
        testStock.setQuantity(10);
        testStock.setLowStockThreshold(5);
        testStock.setStatus(StockStatus.IN_STOCK);
        testStock.setUpdatedAt(LocalDateTime.now());

        // Set up test stock DTO
        testStockDto = new StockDto();
        testStockDto.setId(1L);
        testStockDto.setSku("TEST-SKU-001");
        testStockDto.setProductId(1L);
        testStockDto.setProductName("Test Product");
        testStockDto.setQuantity(10);
        testStockDto.setLowStockThreshold(5);
        testStockDto.setStatus(StockStatus.IN_STOCK);
        testStockDto.setUpdatedAt(LocalDateTime.now());

        // Set up test stock history
        testStockHistory = new StockHistory();
        testStockHistory.setId(1L);
        testStockHistory.setStock(testStock);
        testStockHistory.setQuantityBefore(0);
        testStockHistory.setQuantityAfter(10);
        testStockHistory.setAction(StockActionState.RESTOCK);
        testStockHistory.setReference("Initial stock");
        testStockHistory.setTimestamp(LocalDateTime.now());

        // Set up test stock history DTO
        testStockHistoryDto = new StockHistoryDto();
        testStockHistoryDto.setId(1L);
        testStockHistoryDto.setStockId(1L);
        testStockHistoryDto.setQuantityBefore(0);
        testStockHistoryDto.setQuantityAfter(10);
        testStockHistoryDto.setAction(StockActionState.RESTOCK);
        testStockHistoryDto.setReference("Initial stock");
        testStockHistoryDto.setTimestamp(LocalDateTime.now());
    }

    @Test
    void getAll_ShouldReturnAllStocks() {
        // Arrange
        when(stockRepository.findAll()).thenReturn(List.of(testStock));
        when(stockMapper.toDto(testStock)).thenReturn(testStockDto);

        // Act
        List<StockDto> result = stockService.getAll();

        // Assert
        assertEquals(1, result.size());
        assertEquals(testStockDto, result.get(0));
        verify(stockRepository).findAll();
    }

    @Test
    void getById_WithValidId_ShouldReturnStock() {
        // Arrange
        when(stockRepository.findById(1L)).thenReturn(Optional.of(testStock));
        when(stockMapper.toDto(testStock)).thenReturn(testStockDto);

        // Act
        StockDto result = stockService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testStockDto, result);
        verify(stockRepository).findById(1L);
    }

    @Test
    void getById_WithInvalidId_ShouldReturnNull() {
        // Arrange
        when(stockRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        StockDto result = stockService.getById(99L);

        // Assert
        assertNull(result);
        verify(stockRepository).findById(99L);
    }

    @Test
    void create_ShouldCreateStockAndHistoryEntry() {
        // Arrange
        when(stockMapper.toEntity(testStockDto)).thenReturn(testStock);
        when(stockRepository.save(testStock)).thenReturn(testStock);
        when(stockMapper.toDto(testStock)).thenReturn(testStockDto);

        // Act
        StockDto result = stockService.create(testStockDto);

        // Assert
        assertNotNull(result);
        assertEquals(testStockDto, result);
        
        // Verify stock was saved
        verify(stockRepository).save(testStock);
        
        // Verify history entry was created
        verify(stockHistoryRepository).save(stockHistoryCaptor.capture());
        StockHistory capturedHistory = stockHistoryCaptor.getValue();
        assertEquals(0, capturedHistory.getQuantityBefore());
        assertEquals(testStock.getQuantity(), capturedHistory.getQuantityAfter());
        assertEquals(StockActionState.RESTOCK, capturedHistory.getAction());
        assertEquals("Initial stock creation", capturedHistory.getReference());
    }

    @Test
    void update_WithValidId_ShouldUpdateStock() {
        // Arrange
        Integer oldQuantity = 10;
        Integer newQuantity = 15;
        
        testStock.setQuantity(oldQuantity);
        
        when(stockRepository.existsById(1L)).thenReturn(true);
        when(stockRepository.findById(1L)).thenReturn(Optional.of(testStock));
        
        // Update the stock quantity in the DTO
        testStockDto.setQuantity(newQuantity);
        
        Stock updatedStock = new Stock();
        updatedStock.setId(1L);
        updatedStock.setQuantity(newQuantity);
        
        when(stockMapper.toEntity(testStockDto)).thenReturn(updatedStock);
        when(stockRepository.save(any(Stock.class))).thenReturn(updatedStock);
        when(stockMapper.toDto(updatedStock)).thenReturn(testStockDto);

        // Act
        StockDto result = stockService.update(1L, testStockDto);

        // Assert
        assertNotNull(result);
        assertEquals(testStockDto, result);
        
        // Verify stock was updated
        verify(stockRepository).save(any(Stock.class));
        
        // Verify history entry was created due to quantity change
        verify(stockHistoryRepository).save(stockHistoryCaptor.capture());
        StockHistory capturedHistory = stockHistoryCaptor.getValue();
        assertEquals(oldQuantity, capturedHistory.getQuantityBefore());
        assertEquals(newQuantity, capturedHistory.getQuantityAfter());
        assertEquals(StockActionState.ADJUSTMENT, capturedHistory.getAction());
    }

    @Test
    void update_WithInvalidId_ShouldReturnNull() {
        // Arrange
        when(stockRepository.existsById(99L)).thenReturn(false);

        // Act
        StockDto result = stockService.update(99L, testStockDto);

        // Assert
        assertNull(result);
        verify(stockRepository, never()).save(any(Stock.class));
        verify(stockHistoryRepository, never()).save(any(StockHistory.class));
    }

    @Test
    void delete_ShouldDeleteStock() {
        // Arrange
        Long stockId = 1L;

        // Act
        stockService.delete(stockId);

        // Assert
        verify(stockRepository).deleteById(stockId);
    }

    @Test
    void performStockAction_Restock_ShouldIncreaseQuantity() {
        // Arrange
        Long stockId = 1L;
        Integer quantity = 5;
        Integer oldQuantity = 10;
        Integer expectedNewQuantity = 15;
        StockActionState action = StockActionState.RESTOCK;
        String reference = "Test restock";
        
        testStock.setQuantity(oldQuantity);
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(stockCaptor.capture())).thenReturn(testStock);
        when(stockMapper.toDto(testStock)).thenReturn(testStockDto);

        // Act
        StockDto result = stockService.performStockAction(stockId, quantity, action, reference);

        // Assert
        assertNotNull(result);
        
        // Verify stock quantity was increased
        Stock capturedStock = stockCaptor.getValue();
        assertEquals(expectedNewQuantity, capturedStock.getQuantity());
        
        // Verify history entry was created
        verify(stockHistoryRepository).save(stockHistoryCaptor.capture());
        StockHistory capturedHistory = stockHistoryCaptor.getValue();
        assertEquals(oldQuantity, capturedHistory.getQuantityBefore());
        assertEquals(expectedNewQuantity, capturedHistory.getQuantityAfter());
        assertEquals(action, capturedHistory.getAction());
        assertEquals(reference, capturedHistory.getReference());
    }

    @Test
    void performStockAction_Sale_ShouldDecreaseQuantity() {
        // Arrange
        Long stockId = 1L;
        Integer quantity = 3;
        Integer oldQuantity = 10;
        Integer expectedNewQuantity = 7;
        StockActionState action = StockActionState.SALE;
        String reference = "Test sale";
        
        testStock.setQuantity(oldQuantity);
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(stockCaptor.capture())).thenReturn(testStock);
        when(stockMapper.toDto(testStock)).thenReturn(testStockDto);

        // Act
        StockDto result = stockService.performStockAction(stockId, quantity, action, reference);

        // Assert
        assertNotNull(result);
        
        // Verify stock quantity was decreased
        Stock capturedStock = stockCaptor.getValue();
        assertEquals(expectedNewQuantity, capturedStock.getQuantity());
        
        // Verify history entry was created
        verify(stockHistoryRepository).save(stockHistoryCaptor.capture());
        StockHistory capturedHistory = stockHistoryCaptor.getValue();
        assertEquals(oldQuantity, capturedHistory.getQuantityBefore());
        assertEquals(expectedNewQuantity, capturedHistory.getQuantityAfter());
        assertEquals(action, capturedHistory.getAction());
        assertEquals(reference, capturedHistory.getReference());
    }

    @Test
    void performStockAction_Sale_WithInsufficientStock_ShouldThrowException() {
        // Arrange
        Long stockId = 1L;
        Integer quantity = 15; // More than available (10)
        StockActionState action = StockActionState.SALE;
        String reference = "Test insufficient stock";
        
        testStock.setQuantity(10);
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            stockService.performStockAction(stockId, quantity, action, reference));
        
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        
        // Verify no stock update or history entry
        verify(stockRepository, never()).save(any(Stock.class));
        verify(stockHistoryRepository, never()).save(any(StockHistory.class));
    }

    @Test
    void performStockAction_Return_ShouldIncreaseQuantity() {
        // Arrange
        Long stockId = 1L;
        Integer quantity = 2;
        Integer oldQuantity = 10;
        Integer expectedNewQuantity = 12;
        StockActionState action = StockActionState.RETURN;
        String reference = "Test return";
        
        testStock.setQuantity(oldQuantity);
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(stockCaptor.capture())).thenReturn(testStock);
        when(stockMapper.toDto(testStock)).thenReturn(testStockDto);

        // Act
        StockDto result = stockService.performStockAction(stockId, quantity, action, reference);

        // Assert
        assertNotNull(result);
        
        // Verify stock quantity was increased
        Stock capturedStock = stockCaptor.getValue();
        assertEquals(expectedNewQuantity, capturedStock.getQuantity());
        
        // Verify history entry was created
        verify(stockHistoryRepository).save(stockHistoryCaptor.capture());
        StockHistory capturedHistory = stockHistoryCaptor.getValue();
        assertEquals(oldQuantity, capturedHistory.getQuantityBefore());
        assertEquals(expectedNewQuantity, capturedHistory.getQuantityAfter());
        assertEquals(action, capturedHistory.getAction());
        assertEquals(reference, capturedHistory.getReference());
    }

    @Test
    void performStockAction_Adjustment_ShouldSetQuantityDirectly() {
        // Arrange
        Long stockId = 1L;
        Integer newQuantity = 20;
        Integer oldQuantity = 10;
        StockActionState action = StockActionState.ADJUSTMENT;
        String reference = "Test adjustment";
        
        testStock.setQuantity(oldQuantity);
        
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(testStock));
        when(stockRepository.save(stockCaptor.capture())).thenReturn(testStock);
        when(stockMapper.toDto(testStock)).thenReturn(testStockDto);

        // Act
        StockDto result = stockService.performStockAction(stockId, newQuantity, action, reference);

        // Assert
        assertNotNull(result);
        
        // Verify stock quantity was set directly
        Stock capturedStock = stockCaptor.getValue();
        assertEquals(newQuantity, capturedStock.getQuantity());
        
        // Verify history entry was created
        verify(stockHistoryRepository).save(stockHistoryCaptor.capture());
        StockHistory capturedHistory = stockHistoryCaptor.getValue();
        assertEquals(oldQuantity, capturedHistory.getQuantityBefore());
        assertEquals(newQuantity, capturedHistory.getQuantityAfter());
        assertEquals(action, capturedHistory.getAction());
        assertEquals(reference, capturedHistory.getReference());
    }

    @Test
    void getStockHistory_ShouldReturnHistoryForStock() {
        // Arrange
        Long stockId = 1L;
        List<StockHistory> histories = Arrays.asList(testStockHistory);
        
        when(stockHistoryRepository.findByStockIdOrderByTimestampDesc(stockId)).thenReturn(histories);
        when(stockHistoryMapper.toDto(testStockHistory)).thenReturn(testStockHistoryDto);

        // Act
        List<StockHistoryDto> result = stockService.getStockHistory(stockId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testStockHistoryDto, result.get(0));
        verify(stockHistoryRepository).findByStockIdOrderByTimestampDesc(stockId);
    }

    @Test
    void removeStockByProductId_ShouldDeleteAllStocksForProduct() {
        // Arrange
        Long productId = 1L;
        List<Stock> stocks = Arrays.asList(testStock);
        
        when(stockRepository.findByProductId(productId)).thenReturn(stocks);

        // Act
        stockService.removeStockByProductId(productId);

        // Assert
        verify(stockRepository).findByProductId(productId);
        verify(stockRepository).delete(testStock);
    }

    @Test
    void removeStockByProductId_WithNoStocks_ShouldDoNothing() {
        // Arrange
        Long productId = 99L;
        
        when(stockRepository.findByProductId(productId)).thenReturn(new ArrayList<>());

        // Act
        stockService.removeStockByProductId(productId);

        // Assert
        verify(stockRepository).findByProductId(productId);
        verify(stockRepository, never()).delete(any(Stock.class));
    }
}