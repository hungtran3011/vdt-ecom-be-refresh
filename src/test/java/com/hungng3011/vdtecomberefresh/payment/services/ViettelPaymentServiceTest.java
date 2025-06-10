// filepath: /home/andrew/IdeaProjects/vdt-ecom-be-refresh/src/test/java/com/hungng3011/vdtecomberefresh/payment/services/ViettelPaymentServiceTest.java
package com.hungng3011.vdtecomberefresh.payment.services;

import com.hungng3011.vdtecomberefresh.exception.payment.PaymentProcessingException;
import com.hungng3011.vdtecomberefresh.mail.services.NotificationService;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import com.hungng3011.vdtecomberefresh.payment.config.ViettelPaymentConfig;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ViettelPaymentServiceTest {

    @Mock
    private ViettelApiClient viettelApiClient;

    @Mock
    private ViettelPaymentConfig config;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ViettelPaymentService viettelPaymentService;

    private Order testOrder;
    private ViettelPaymentConfig.SettingsConfig configSettings;

    @BeforeEach
    void setUp() {
        // Setup test order
        testOrder = new Order();
        testOrder.setId("test-order-123");
        testOrder.setUserEmail("test@example.com");
        testOrder.setTotalPrice(BigDecimal.valueOf(100.00));
        testOrder.setPhone("+84123456789");
        testOrder.setAddress("123 Test Street, Ho Chi Minh City");
        testOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        // Setup config settings
        configSettings = new ViettelPaymentConfig.SettingsConfig();
        configSettings.setDefaultReturnType("json");
        configSettings.setDefaultExpireAfterMinutes(30);
        
        when(config.getSettings()).thenReturn(configSettings);
        when(config.getRedirectUrl()).thenReturn("https://example.com/return");
    }

    @Test
    void initiatePayment_shouldInitiateSuccessfully_whenOrderExists() {
        // Arrange
        ViettelTransactionInitiationResponse mockResponse = createMockInitiationResponse();
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));
        when(viettelApiClient.createTransaction(any(ViettelTransactionInitiationRequest.class)))
                .thenReturn(mockResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        ViettelTransactionInitiationResponse response = viettelPaymentService.initiatePayment(
                "test-order-123", "json");

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("VT123456789", response.getData().getVtRequestId());
        
        // Verify order was updated
        verify(orderRepository, times(1)).save(any(Order.class));
        
        // Verify API call
        verify(viettelApiClient, times(1)).createTransaction(argThat(request ->
            "test-order-123".equals(request.getOrderId()) &&
            request.getTransAmount().equals(10000L) && // 100.00 * 100
            request.getDescription().contains("Payment for order test-order-123")
        ));
    }

    @Test
    void initiatePayment_shouldThrowException_whenOrderNotFound() {
        // Arrange
        when(orderRepository.findById("non-existent-order")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> viettelPaymentService.initiatePayment("non-existent-order", "json"));
        
        assertTrue(exception.getMessage().contains("Failed to initiate payment"));
        verify(viettelApiClient, never()).createTransaction(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void initiatePayment_shouldThrowException_whenOrderNotPendingPayment() {
        // Arrange
        testOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> viettelPaymentService.initiatePayment("test-order-123", "json"));
        
        assertTrue(exception.getMessage().contains("Failed to initiate payment"));
        verify(viettelApiClient, never()).createTransaction(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void initiatePayment_shouldThrowException_whenViettelApiFails() {
        // Arrange
        ViettelTransactionInitiationResponse failureResponse = new ViettelTransactionInitiationResponse();
        failureResponse.setStatus("FAILED");
        failureResponse.setMessage("Payment gateway error");
        
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));
        when(viettelApiClient.createTransaction(any(ViettelTransactionInitiationRequest.class)))
                .thenReturn(failureResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> viettelPaymentService.initiatePayment("test-order-123", "json"));
        
        assertTrue(exception.getMessage().contains("Failed to initiate payment"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderPaymentStatus_shouldSendPaymentSuccessEmail_whenPaymentSuccessful() {
        // Arrange
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(notificationService).sendPaymentSuccessEmail(anyString(), anyString(), anyString(), any(BigDecimal.class));

        // Act
        viettelPaymentService.updateOrderPaymentStatus("test-order-123", 1, "00", "VT123456789");

        // Assert
        verify(orderRepository, times(1)).save(any(Order.class));
        
        verify(notificationService, times(1)).sendPaymentSuccessEmail(
                eq("test-order-123"),
                eq("test@example.com"),
                eq("VT123456789"),
                any(BigDecimal.class)
        );
    }

    @Test
    void updateOrderPaymentStatus_shouldSendPaymentFailedEmail_whenPaymentFailed() {
        // Arrange
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(notificationService).sendPaymentFailedEmail(anyString(), anyString(), anyString(), anyString());

        // Act
        viettelPaymentService.updateOrderPaymentStatus("test-order-123", 0, "99", "VT123456789");

        // Assert
        verify(orderRepository, times(1)).save(any(Order.class));
        
        verify(notificationService, times(1)).sendPaymentFailedEmail(
                eq("test-order-123"),
                eq("test@example.com"),
                eq("VT123456789"),
                contains("Payment processing failed with error code: 99")
        );
    }

    @Test
    void updateOrderPaymentStatus_shouldNotSendEmail_whenProfileNotFound() {
        // Arrange
        testOrder.setUserEmail(null); // No email address
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        viettelPaymentService.updateOrderPaymentStatus("test-order-123", 1, "00", "VT123456789");

        // Assert
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(notificationService, never()).sendPaymentSuccessEmail(anyString(), anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void updateOrderPaymentStatus_shouldNotFailPayment_whenEmailFails() {
        // Arrange
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doThrow(new RuntimeException("Email service unavailable"))
                .when(notificationService).sendPaymentSuccessEmail(anyString(), anyString(), anyString(), any(BigDecimal.class));

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> 
            viettelPaymentService.updateOrderPaymentStatus("test-order-123", 1, "00", "VT123456789")
        );
        
        // Payment should still be processed successfully
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void processRefund_shouldSendRefundConfirmationEmail_whenRefundSuccessful() {
        // Arrange
        testOrder.setPaymentId("VT123456789");
        testOrder.setStatus(OrderStatus.PAID);
        
        ViettelRefundResponse mockResponse = createMockRefundResponse();
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));
        when(viettelApiClient.refundTransaction(any(ViettelRefundRequest.class))).thenReturn(mockResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(notificationService).sendRefundConfirmationEmail(anyString(), anyString(), anyString(), any(BigDecimal.class));

        // Act
        ViettelRefundResponse response = viettelPaymentService.processRefund(
                "test-order-123", 5000L, "Customer requested refund");

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        
        verify(orderRepository, times(1)).save(any(Order.class));
        
        verify(notificationService, times(1)).sendRefundConfirmationEmail(
                eq("test-order-123"),
                eq("test@example.com"),
                eq("VT_REFUND_123"),
                any(BigDecimal.class)
        );
        
        verify(viettelApiClient, times(1)).refundTransaction(argThat(request ->
            request.getTransAmount().equals(5000L) &&
            request.getOriginalRequestId().equals("VT123456789") &&
            request.getDescription().equals("Customer requested refund")
        ));
    }

    @Test
    void processRefund_shouldThrowException_whenOrderNotFound() {
        // Arrange
        when(orderRepository.findById("non-existent-order")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> viettelPaymentService.processRefund("non-existent-order", 5000L, "Test refund"));
        
        assertTrue(exception.getMessage().contains("Failed to process refund"));
        verify(viettelApiClient, never()).refundTransaction(any());
        verify(notificationService, never()).sendRefundConfirmationEmail(anyString(), anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void processRefund_shouldThrowException_whenOrderHasNoPaymentId() {
        // Arrange
        testOrder.setPaymentId(null);
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> viettelPaymentService.processRefund("test-order-123", 5000L, "Test refund"));
        
        assertTrue(exception.getMessage().contains("Failed to process refund"));
        verify(viettelApiClient, never()).refundTransaction(any());
        verify(notificationService, never()).sendRefundConfirmationEmail(anyString(), anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void processRefund_shouldNotSendEmail_whenEmailAddressEmpty() {
        // Arrange
        testOrder.setPaymentId("VT123456789");
        testOrder.setUserEmail(""); // Empty email
        
        ViettelRefundResponse mockResponse = createMockRefundResponse();
        when(orderRepository.findById("test-order-123")).thenReturn(Optional.of(testOrder));
        when(viettelApiClient.refundTransaction(any(ViettelRefundRequest.class))).thenReturn(mockResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        ViettelRefundResponse response = viettelPaymentService.processRefund(
                "test-order-123", 5000L, "Customer requested refund");

        // Assert
        assertNotNull(response);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(notificationService, never()).sendRefundConfirmationEmail(anyString(), anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void queryTransactionStatus_shouldReturnTransactionData_whenFound() {
        // Arrange
        ViettelQueryTransactionResponse mockResponse = createMockQueryResponse();
        when(viettelApiClient.queryTransaction(any(ViettelQueryTransactionRequest.class)))
                .thenReturn(mockResponse);

        // Act
        ViettelQueryTransactionResponse.TransactionQueryData result = 
                viettelPaymentService.queryTransactionStatus("test-order-123");

        // Assert
        assertNotNull(result);
        assertEquals("test-order-123", result.getOrderId());
        assertEquals(1, result.getTransactionStatus());
        
        verify(viettelApiClient, times(1)).queryTransaction(argThat(request ->
            "test-order-123".equals(request.getOrderId())
        ));
    }

    @Test
    void queryTransactionStatus_shouldReturnNull_whenNoDataFound() {
        // Arrange
        ViettelQueryTransactionResponse mockResponse = new ViettelQueryTransactionResponse();
        mockResponse.setData(List.of()); // Empty list
        when(viettelApiClient.queryTransaction(any(ViettelQueryTransactionRequest.class)))
                .thenReturn(mockResponse);

        // Act
        ViettelQueryTransactionResponse.TransactionQueryData result = 
                viettelPaymentService.queryTransactionStatus("test-order-123");

        // Assert
        assertNull(result);
        verify(viettelApiClient, times(1)).queryTransaction(any());
    }

    @Test
    void queryTransactionStatus_shouldReturnNull_whenExceptionOccurs() {
        // Arrange
        when(viettelApiClient.queryTransaction(any(ViettelQueryTransactionRequest.class)))
                .thenThrow(new RuntimeException("API error"));

        // Act
        ViettelQueryTransactionResponse.TransactionQueryData result = 
                viettelPaymentService.queryTransactionStatus("test-order-123");

        // Assert
        assertNull(result);
        verify(viettelApiClient, times(1)).queryTransaction(any());
    }

    @Test
    void getCurrentEnvironment_shouldReturnConfiguredEnvironment() {
        // Arrange
        when(config.getActiveEnvironment()).thenReturn("TEST");

        // Act
        String environment = viettelPaymentService.getCurrentEnvironment();

        // Assert
        assertEquals("TEST", environment);
        verify(config, times(1)).getActiveEnvironment();
    }

    @Test
    void updateOrderPaymentStatus_shouldSkipEmail_whenOrderNotFound() {
        // Arrange
        when(orderRepository.findById("non-existent-order")).thenReturn(Optional.empty());

        // Act
        viettelPaymentService.updateOrderPaymentStatus("non-existent-order", 1, "00", "VT123456789");

        // Assert
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).sendPaymentSuccessEmail(anyString(), anyString(), anyString(), any(BigDecimal.class));
        verify(notificationService, never()).sendPaymentFailedEmail(anyString(), anyString(), anyString(), anyString());
    }

    // Helper methods to create mock responses
    private ViettelTransactionInitiationResponse createMockInitiationResponse() {
        ViettelTransactionInitiationResponse response = new ViettelTransactionInitiationResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Transaction created successfully");
        
        ViettelTransactionInitiationResponse.TransactionData data = 
                new ViettelTransactionInitiationResponse.TransactionData();
        data.setVtRequestId("VT123456789");
        data.setUrl("https://payment.viettel.vn/payment?id=VT123456789");
        data.setOrderId("test-order-123");
        data.setTransAmount(10000L);
        
        response.setData(data);
        return response;
    }

    private ViettelRefundResponse createMockRefundResponse() {
        ViettelRefundResponse response = new ViettelRefundResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Refund processed successfully");
        
        ViettelRefundResponse.RefundData data = new ViettelRefundResponse.RefundData();
        data.setVtRequestId("VT_REFUND_123");
        data.setTransAmount(5000L);
        data.setOrderId("REFUND_test-order-123_" + System.currentTimeMillis());
        data.setOriginalRequestId("VT123456789");
        data.setTransactionStatus(1);
        data.setErrorCode("00");
        
        response.setData(data);
        return response;
    }

    private ViettelQueryTransactionResponse createMockQueryResponse() {
        ViettelQueryTransactionResponse response = new ViettelQueryTransactionResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Query successful");
        
        ViettelQueryTransactionResponse.TransactionQueryData data = 
                new ViettelQueryTransactionResponse.TransactionQueryData();
        data.setOrderId("test-order-123");
        data.setTransactionStatus(1);
        data.setVtRequestId("VT123456789");
        data.setTransAmount(10000L);
        data.setErrorCode("00");
        data.setType("PAYMENT");
        data.setCurrency("VND");
        data.setPaymentMethod("VIETTEL_MONEY");
        
        response.setData(List.of(data));
        return response;
    }
}
