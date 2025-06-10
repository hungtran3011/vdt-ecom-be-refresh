package com.hungng3011.vdtecomberefresh.payment.controllers;

import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import com.hungng3011.vdtecomberefresh.payment.config.ViettelPaymentConfig;
import com.hungng3011.vdtecomberefresh.payment.dtos.partner.OrderConfirmationRequest;
import com.hungng3011.vdtecomberefresh.payment.dtos.partner.OrderConfirmationResponse;
import com.hungng3011.vdtecomberefresh.payment.dtos.partner.TransactionResultRequest;
import com.hungng3011.vdtecomberefresh.payment.dtos.partner.TransactionResultResponse;
import com.hungng3011.vdtecomberefresh.payment.security.ViettelSignatureHandler;
import com.hungng3011.vdtecomberefresh.payment.services.ViettelPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import org.springframework.web.util.HtmlUtils;

/**
 * Partner API controllers for Viettel Money integration
 * These endpoints are called by Viettel's system
 */
@RestController
@RequestMapping("/v1/viettel/partner")
@RequiredArgsConstructor
@Slf4j
public class ViettelPartnerController {
    
    private final ViettelPaymentService viettelPaymentService;
    private final ViettelSignatureHandler signatureHandler;
    private final ViettelPaymentConfig config;
    private final OrderRepository orderRepository;
    
    /**
     * Order Confirmation API - Called by Viettel to confirm order details
     */
    @PostMapping("/order-confirmation")
    public ResponseEntity<OrderConfirmationResponse> confirmOrder(
            @RequestBody OrderConfirmationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Received order confirmation request from Viettel. OrderId: {}, VtRequestId: {}", 
                    request.getOrderId(), request.getVtRequestId());
            
            // Verify signature
            if (!verifySignature(httpRequest, request.toString())) {
                log.error("Invalid signature for order confirmation. OrderId: {}", request.getOrderId());
                return ResponseEntity.ok(OrderConfirmationResponse.builder()
                        .code("01")
                        .orderId(request.getOrderId())
                        .message("Invalid signature")
                        .build());
            }
            
            // Find and validate order
            Optional<Order> orderOpt = orderRepository.findById(request.getOrderId());
            if (orderOpt.isEmpty()) {
                log.error("Order not found for confirmation. OrderId: {}", request.getOrderId());
                return ResponseEntity.ok(OrderConfirmationResponse.builder()
                        .code("02")
                        .orderId(request.getOrderId())
                        .message("Order not found")
                        .build());
            }
            
            Order order = orderOpt.get();
            
            // Validate order status
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                log.error("Order is not in pending payment status. OrderId: {}, Status: {}", 
                        request.getOrderId(), order.getStatus());
                return ResponseEntity.ok(OrderConfirmationResponse.builder()
                        .code("03")
                        .orderId(request.getOrderId())
                        .message("Order is not in pending payment status")
                        .build());
            }
            
            // Validate amount
            long expectedAmount = order.getTotalPrice().multiply(new java.math.BigDecimal("100")).longValue();
            if (!request.getTransAmount().equals(expectedAmount)) {
                log.error("Amount mismatch. OrderId: {}, Expected: {}, Received: {}", 
                        request.getOrderId(), expectedAmount, request.getTransAmount());
                return ResponseEntity.ok(OrderConfirmationResponse.builder()
                        .code("04")
                        .orderId(request.getOrderId())
                        .message("Amount mismatch")
                        .build());
            }
            
            log.info("Order confirmation successful. OrderId: {}", request.getOrderId());
            
            // Return success response
            return ResponseEntity.ok(OrderConfirmationResponse.builder()
                    .code("00")
                    .orderId(request.getOrderId())
                    .message("Success")
                    .build());
            
        } catch (Exception e) {
            log.error("Error processing order confirmation. OrderId: {}", request.getOrderId(), e);
            return ResponseEntity.ok(OrderConfirmationResponse.builder()
                    .code("99")
                    .orderId(request.getOrderId())
                    .message("System error")
                    .build());
        }
    }
    
    /**
     * IPN (Instant Payment Notification) API - Called by Viettel to notify payment results
     */
    @PostMapping("/ipn")
    public ResponseEntity<TransactionResultResponse> processTransactionResult(
            @RequestBody TransactionResultRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Received IPN from Viettel. OrderId: {}, VtRequestId: {}, Status: {}", 
                    request.getOrderId(), request.getVtRequestId(), request.getTransactionStatus());
            
            // Verify signature
            if (!verifySignature(httpRequest, request.toString())) {
                log.error("Invalid signature for IPN. OrderId: {}", request.getOrderId());
                return ResponseEntity.ok(TransactionResultResponse.builder()
                        .code("01")
                        .orderId(request.getOrderId())
                        .message("Invalid signature")
                        .build());
            }
            
            // Update order payment status
            viettelPaymentService.updateOrderPaymentStatus(
                    request.getOrderId(),
                    request.getTransactionStatus(),
                    request.getErrorCode(),
                    request.getVtRequestId()
            );
            
            log.info("IPN processed successfully. OrderId: {}, Status: {}", 
                    request.getOrderId(), request.getTransactionStatus());
            
            // Return success response
            return ResponseEntity.ok(TransactionResultResponse.builder()
                    .code("00")
                    .orderId(request.getOrderId())
                    .message("Success")
                    .build());
            
        } catch (Exception e) {
            log.error("Error processing IPN. OrderId: {}", request.getOrderId(), e);
            return ResponseEntity.ok(TransactionResultResponse.builder()
                    .code("99")
                    .orderId(request.getOrderId())
                    .message("System error")
                    .build());
        }
    }
    
    /**
     * Redirect URL handler for web payments
     */
    @GetMapping("/redirect")
    public ResponseEntity<String> handleRedirect(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String vtRequestId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Received redirect callback. OrderId: {}, Status: {}, VtRequestId: {}", 
                    orderId, status, vtRequestId);
            
            if (orderId != null) {
                // Query latest transaction status from Viettel
                var transactionData = viettelPaymentService.queryTransactionStatus(orderId);
                if (transactionData != null) {
                    viettelPaymentService.updateOrderPaymentStatus(
                            orderId,
                            transactionData.getTransactionStatus(),
                            transactionData.getErrorCode(),
                            transactionData.getVtRequestId()
                    );
                }
            }
            
            // Return a simple HTML page or redirect to frontend
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Payment Result</title>
                        <meta charset="UTF-8">
                    </head>
                    <body>
                        <h1>Payment Processing</h1>
                        <p>Your payment has been processed. You can now close this window.</p>
                        <script>
                            // You can redirect to your frontend application here
                            // window.location.href = 'your-frontend-url/payment-result?orderId=' + '%s';
                            setTimeout(function() { window.close(); }, 3000);
                        </script>
                    </body>
                    </html>
                    """.formatted(orderId != null ? HtmlUtils.htmlEscape(orderId) : "");
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
            
        } catch (Exception e) {
            log.error("Error handling redirect. OrderId: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payment result");
        }
    }
    
    /**
     * Verify request signature from Viettel
     */
    private boolean verifySignature(HttpServletRequest httpRequest, String requestBody) {
        try {
            String signature = httpRequest.getHeader("X-Signature");
            if (signature == null || signature.isEmpty()) {
                log.warn("Missing signature header in request");
                return false;
            }
            
            ViettelPaymentConfig.EnvironmentConfig env = config.getCurrentEnvironment();
            
            return signatureHandler.verifySignature(requestBody, signature, env.getPartnerPublicKey());
            
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }
}
