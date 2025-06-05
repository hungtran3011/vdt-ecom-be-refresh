package com.hungng3011.vdtecomberefresh.payment.controllers;

import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelTransactionInitiationResponse;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelQueryTransactionResponse;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelRefundResponse;
import com.hungng3011.vdtecomberefresh.payment.services.ViettelPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Main payment controller for Viettel Money integration
 * These endpoints are called by the frontend application
 */
@RestController
@RequestMapping("/api/payment/viettel")
@RequiredArgsConstructor
@Slf4j
public class ViettelPaymentController {
    
    private final ViettelPaymentService viettelPaymentService;
    private final OrderRepository orderRepository;
    
    /**
     * Initiate payment for an order
     */
    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<?> initiatePayment(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "WEB") String returnType,
            @RequestParam(required = false) String returnUrl) {
        
        try {
            log.info("Initiating Viettel payment for order: {}, returnType: {}", orderId, returnType);
            
            // Validate order exists and is in correct status
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.error("Order not found: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found", "orderId", orderId));
            }
            
            Order order = orderOpt.get();
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                log.error("Order {} is not in pending payment status. Current status: {}", 
                        orderId, order.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Order is not in pending payment status", 
                                    "orderId", orderId,
                                    "currentStatus", order.getStatus().toString()));
            }
            
            // Validate return type
            if (!isValidReturnType(returnType)) {
                log.error("Invalid return type: {}", returnType);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid return type. Must be WEB, QR, or DEEPLINK", 
                                    "returnType", returnType));
            }
            
            // Initiate payment with Viettel
            ViettelTransactionInitiationResponse response = viettelPaymentService.initiatePayment(
                    orderId, returnType);
            
            if (response == null || (!"SUCCESS".equals(response.getStatus()) && !"00".equals(response.getStatus()))) {
                log.error("Failed to initiate payment with Viettel for order: {}. Response: {}", 
                        orderId, response);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to initiate payment", 
                                    "orderId", orderId,
                                    "viettelResponse", response != null ? response.getMessage() : "No response"));
            }
            
            log.info("Payment initiated successfully for order: {}. VtRequestId: {}", 
                    orderId, response.getData().getVtRequestId());
            
            // Return response based on return type
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", orderId,
                    "vtRequestId", response.getData().getVtRequestId(),
                    "returnType", returnType,
                    "paymentUrl", response.getData().getUrl() != null ? response.getData().getUrl() : "",
                    "qrCode", response.getData().getQrCode() != null ? response.getData().getQrCode() : "",
                    "deepLink", response.getData().getDeepLink() != null ? response.getData().getDeepLink() : "",
                    "message", response.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("Error initiating payment for order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", 
                                "orderId", orderId,
                                "message", e.getMessage()));
        }
    }
    
    /**
     * Check payment status for an order
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String orderId) {
        
        try {
            log.info("Checking payment status for order: {}", orderId);
            
            // Check if order exists
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.error("Order not found: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found", "orderId", orderId));
            }
            
            Order order = orderOpt.get();
            
            // Query transaction status from Viettel
            ViettelQueryTransactionResponse.TransactionQueryData transactionData = 
                    viettelPaymentService.queryTransactionStatus(orderId);
            
            if (transactionData != null) {
                // Update order status based on Viettel response
                viettelPaymentService.updateOrderPaymentStatus(
                        orderId,
                        transactionData.getTransactionStatus(),
                        transactionData.getErrorCode(),
                        transactionData.getVtRequestId()
                );
                
                // Refresh order from database
                order = orderRepository.findById(orderId).orElse(order);
            }
            
            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "orderStatus", order.getStatus().toString(),
                    "transactionStatus", transactionData != null ? transactionData.getTransactionStatus() : "UNKNOWN",
                    "vtRequestId", transactionData != null ? transactionData.getVtRequestId() : "",
                    "errorCode", transactionData != null ? transactionData.getErrorCode() : "",
                    "lastUpdated", order.getUpdatedAt()
            ));
            
        } catch (Exception e) {
            log.error("Error checking payment status for order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", 
                                "orderId", orderId,
                                "message", e.getMessage()));
        }
    }
    
    /**
     * Refund a payment
     */
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<?> refundPayment(
            @PathVariable String orderId,
            @RequestParam(required = false) Long refundAmount,
            @RequestParam(required = false) String reason) {
        
        try {
            log.info("Initiating refund for order: {}, amount: {}, reason: {}", 
                    orderId, refundAmount, reason);
            
            // Validate order exists
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.error("Order not found: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found", "orderId", orderId));
            }
            
            Order order = orderOpt.get();
            
            // Validate order status
            if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.CONFIRMED) {
                log.error("Order {} cannot be refunded. Current status: {}", 
                        orderId, order.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Order cannot be refunded", 
                                    "orderId", orderId,
                                    "currentStatus", order.getStatus().toString()));
            }
            
            // Process refund
            ViettelRefundResponse response = viettelPaymentService.processRefund(
                    orderId, refundAmount, reason);
            
            if (response == null || (!"SUCCESS".equals(response.getStatus()) && !"00".equals(response.getStatus()))) {
                log.error("Failed to process refund for order: {}. Response: {}", 
                        orderId, response);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to process refund", 
                                    "orderId", orderId,
                                    "viettelResponse", response != null ? response.getMessage() : "No response"));
            }
            
            log.info("Refund processed successfully for order: {}. RefundRequestId: {}", 
                    orderId, response.getData().getVtRequestId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", orderId,
                    "refundRequestId", response.getData().getVtRequestId(),
                    "refundAmount", response.getData().getTransAmount(),
                    "message", response.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("Error processing refund for order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", 
                                "orderId", orderId,
                                "message", e.getMessage()));
        }
    }
    
    /**
     * Get all supported payment methods and configurations
     */
    @GetMapping("/config")
    public ResponseEntity<?> getPaymentConfig() {
        
        try {
            return ResponseEntity.ok(Map.of(
                    "supportedReturnTypes", new String[]{"WEB", "QR", "DEEPLINK"},
                    "supportedCurrencies", new String[]{"VND"},
                    "environment", viettelPaymentService.getCurrentEnvironment(),
                    "isEnabled", true
            ));
            
        } catch (Exception e) {
            log.error("Error getting payment configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", 
                                "message", e.getMessage()));
        }
    }
    
    private boolean isValidReturnType(String returnType) {
        return "WEB".equalsIgnoreCase(returnType) || 
               "QR".equalsIgnoreCase(returnType) || 
               "DEEPLINK".equalsIgnoreCase(returnType);
    }
}
