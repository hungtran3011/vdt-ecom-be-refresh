package com.hungng3011.vdtecomberefresh.mail.services;

import com.hungng3011.vdtecomberefresh.mail.config.MailConfig;
import com.hungng3011.vdtecomberefresh.mail.dtos.EmailNotificationDto;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final EmailService emailService;
    private final MailConfig mailConfig;
    
    public void sendOrderConfirmationEmail(OrderDto order, String customerEmail) {
        log.info("Sending order confirmation email for order: {} to: {}", order.getId(), customerEmail);
        
        String subject = processSubjectTemplate(mailConfig.getSubjects().get("order-confirmation"), order.getId());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", extractCustomerName(customerEmail));
        variables.put("order", order);
        variables.put("totalAmount", order.getTotalPrice());
        variables.put("orderDate", order.getCreatedAt());
        variables.put("orderItems", order.getItems());
        
        EmailNotificationDto emailDto = EmailNotificationDto.builder()
                .to(customerEmail)
                .subject(subject)
                .template(mailConfig.getTemplates().get("order-confirmation"))
                .variables(variables)
                .orderId(order.getId())
                .userId(order.getUserEmail()) // Using email as user identifier
                .type(EmailNotificationDto.EmailType.ORDER_CONFIRMATION)
                .textContent(createOrderConfirmationText(order))
                .build();
        
        if (mailConfig.isAsync()) {
            emailService.sendEmailAsync(emailDto);
        } else {
            emailService.sendEmailSync(emailDto);
        }
    }
    
    public void sendPaymentSuccessEmail(String orderId, String customerEmail, String transactionId, BigDecimal amount) {
        log.info("Sending payment success email for order: {} to: {}", orderId, customerEmail);
        
        String subject = processSubjectTemplate(mailConfig.getSubjects().get("payment-success"), orderId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", extractCustomerName(customerEmail));
        variables.put("amount", amount);
        variables.put("transactionId", transactionId);
        variables.put("paymentDate", java.time.LocalDateTime.now());
        
        EmailNotificationDto emailDto = EmailNotificationDto.builder()
                .to(customerEmail)
                .subject(subject)
                .template(mailConfig.getTemplates().get("payment-success"))
                .variables(variables)
                .orderId(orderId)
                .transactionId(transactionId)
                .type(EmailNotificationDto.EmailType.PAYMENT_SUCCESS)
                .textContent(createPaymentSuccessText(orderId, transactionId, amount))
                .build();
        
        if (mailConfig.isAsync()) {
            emailService.sendEmailAsync(emailDto);
        } else {
            emailService.sendEmailSync(emailDto);
        }
    }
    
    public void sendPaymentFailedEmail(String orderId, String customerEmail, String transactionId, String errorMessage) {
        log.info("Sending payment failed email for order: {} to: {}", orderId, customerEmail);
        
        String subject = processSubjectTemplate(mailConfig.getSubjects().get("payment-failed"), orderId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", extractCustomerName(customerEmail));
        variables.put("errorMessage", errorMessage);
        variables.put("transactionId", transactionId);
        variables.put("failureDate", java.time.LocalDateTime.now());
        
        EmailNotificationDto emailDto = EmailNotificationDto.builder()
                .to(customerEmail)
                .subject(subject)
                .template(mailConfig.getTemplates().get("payment-failed"))
                .variables(variables)
                .orderId(orderId)
                .transactionId(transactionId)
                .type(EmailNotificationDto.EmailType.PAYMENT_FAILED)
                .textContent(createPaymentFailedText(orderId, transactionId, errorMessage))
                .build();
        
        if (mailConfig.isAsync()) {
            emailService.sendEmailAsync(emailDto);
        } else {
            emailService.sendEmailSync(emailDto);
        }
    }
    
    public void sendRefundConfirmationEmail(String orderId, String customerEmail, String refundTransactionId, BigDecimal refundAmount) {
        log.info("Sending refund confirmation email for order: {} to: {}", orderId, customerEmail);
        
        String subject = processSubjectTemplate(mailConfig.getSubjects().get("refund-confirmation"), orderId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", extractCustomerName(customerEmail));
        variables.put("refundAmount", refundAmount);
        variables.put("refundTransactionId", refundTransactionId);
        variables.put("refundDate", java.time.LocalDateTime.now());
        variables.put("estimatedDays", "3-5 business days");
        
        EmailNotificationDto emailDto = EmailNotificationDto.builder()
                .to(customerEmail)
                .subject(subject)
                .template(mailConfig.getTemplates().get("refund-confirmation"))
                .variables(variables)
                .orderId(orderId)
                .transactionId(refundTransactionId)
                .type(EmailNotificationDto.EmailType.REFUND_CONFIRMATION)
                .textContent(createRefundConfirmationText(orderId, refundTransactionId, refundAmount))
                .build();
        
        if (mailConfig.isAsync()) {
            emailService.sendEmailAsync(emailDto);
        } else {
            emailService.sendEmailSync(emailDto);
        }
    }
    
    public void sendOrderShippedEmail(String orderId, String customerEmail, String trackingNumber) {
        log.info("Sending order shipped email for order: {} to: {}", orderId, customerEmail);
        
        String subject = processSubjectTemplate(mailConfig.getSubjects().get("order-shipped"), orderId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", extractCustomerName(customerEmail));
        variables.put("trackingNumber", trackingNumber);
        variables.put("shippedDate", java.time.LocalDateTime.now());
        variables.put("estimatedDelivery", java.time.LocalDateTime.now().plusDays(3));
        
        EmailNotificationDto emailDto = EmailNotificationDto.builder()
                .to(customerEmail)
                .subject(subject)
                .template(mailConfig.getTemplates().get("order-shipped"))
                .variables(variables)
                .orderId(orderId)
                .type(EmailNotificationDto.EmailType.ORDER_SHIPPED)
                .textContent(createOrderShippedText(orderId, trackingNumber))
                .build();
        
        if (mailConfig.isAsync()) {
            emailService.sendEmailAsync(emailDto);
        } else {
            emailService.sendEmailSync(emailDto);
        }
    }
    
    public void sendOrderDeliveredEmail(String orderId, String customerEmail) {
        log.info("Sending order delivered email for order: {} to: {}", orderId, customerEmail);
        
        String subject = processSubjectTemplate(mailConfig.getSubjects().get("order-delivered"), orderId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", extractCustomerName(customerEmail));
        variables.put("deliveredDate", java.time.LocalDateTime.now());
        
        EmailNotificationDto emailDto = EmailNotificationDto.builder()
                .to(customerEmail)
                .subject(subject)
                .template(mailConfig.getTemplates().get("order-delivered"))
                .variables(variables)
                .orderId(orderId)
                .type(EmailNotificationDto.EmailType.ORDER_DELIVERED)
                .textContent(createOrderDeliveredText(orderId))
                .build();
        
        if (mailConfig.isAsync()) {
            emailService.sendEmailAsync(emailDto);
        } else {
            emailService.sendEmailSync(emailDto);
        }
    }
    
    public void sendOrderCancellationEmail(String orderId, String customerEmail) {
        log.info("Sending order cancellation email for order: {} to: {}", orderId, customerEmail);
        
        String subject = processSubjectTemplate(mailConfig.getSubjects().get("order-cancellation"), orderId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", extractCustomerName(customerEmail));
        variables.put("cancellationDate", java.time.LocalDateTime.now());
        
        EmailNotificationDto emailDto = EmailNotificationDto.builder()
                .to(customerEmail)
                .subject(subject)
                .template(mailConfig.getTemplates().get("order-cancellation"))
                .variables(variables)
                .orderId(orderId)
                .type(EmailNotificationDto.EmailType.GENERAL_NOTIFICATION)
                .textContent(createOrderCancellationText(orderId))
                .build();
        
        if (mailConfig.isAsync()) {
            emailService.sendEmailAsync(emailDto);
        } else {
            emailService.sendEmailSync(emailDto);
        }
    }

    private String processSubjectTemplate(String template, String orderId) {
        if (template != null) {
            return template.replace("#{orderId}", orderId);
        }
        return "Order Update - " + orderId;
    }
    
    private String extractCustomerName(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return "Valued Customer";
    }
    
    private String createOrderConfirmationText(OrderDto order) {
        StringBuilder text = new StringBuilder();
        text.append("Dear Customer,\n\n");
        text.append("Thank you for your order!\n\n");
        text.append("Order Details:\n");
        text.append("Order ID: ").append(order.getId()).append("\n");
        text.append("Total Amount: ").append(order.getTotalPrice()).append("\n");
        text.append("Order Date: ").append(order.getCreatedAt()).append("\n\n");
        text.append("We will process your order and send you updates shortly.\n\n");
        text.append("Best regards,\n");
        text.append("VDT E-Commerce Team");
        return text.toString();
    }
    
    private String createPaymentSuccessText(String orderId, String transactionId, BigDecimal amount) {
        StringBuilder text = new StringBuilder();
        text.append("Dear Customer,\n\n");
        text.append("Your payment has been processed successfully!\n\n");
        text.append("Payment Details:\n");
        text.append("Order ID: ").append(orderId).append("\n");
        text.append("Transaction ID: ").append(transactionId).append("\n");
        text.append("Amount: ").append(amount).append("\n\n");
        text.append("Your order is now being processed.\n\n");
        text.append("Best regards,\n");
        text.append("VDT E-Commerce Team");
        return text.toString();
    }
    
    private String createPaymentFailedText(String orderId, String transactionId, String errorMessage) {
        StringBuilder text = new StringBuilder();
        text.append("Dear Customer,\n\n");
        text.append("Unfortunately, your payment could not be processed.\n\n");
        text.append("Details:\n");
        text.append("Order ID: ").append(orderId).append("\n");
        text.append("Transaction ID: ").append(transactionId).append("\n");
        text.append("Reason: ").append(errorMessage).append("\n\n");
        text.append("Please try again or contact our support team.\n\n");
        text.append("Best regards,\n");
        text.append("VDT E-Commerce Team");
        return text.toString();
    }
    
    private String createRefundConfirmationText(String orderId, String refundTransactionId, BigDecimal refundAmount) {
        StringBuilder text = new StringBuilder();
        text.append("Dear Customer,\n\n");
        text.append("Your refund has been processed successfully!\n\n");
        text.append("Refund Details:\n");
        text.append("Order ID: ").append(orderId).append("\n");
        text.append("Refund Transaction ID: ").append(refundTransactionId).append("\n");
        text.append("Refund Amount: ").append(refundAmount).append("\n\n");
        text.append("You should see the refund in your account within 3-5 business days.\n\n");
        text.append("Best regards,\n");
        text.append("VDT E-Commerce Team");
        return text.toString();
    }
    
    private String createOrderShippedText(String orderId, String trackingNumber) {
        StringBuilder text = new StringBuilder();
        text.append("Dear Customer,\n\n");
        text.append("Great news! Your order has been shipped.\n\n");
        text.append("Shipping Details:\n");
        text.append("Order ID: ").append(orderId).append("\n");
        text.append("Tracking Number: ").append(trackingNumber).append("\n\n");
        text.append("You can track your package using the tracking number above.\n\n");
        text.append("Best regards,\n");
        text.append("VDT E-Commerce Team");
        return text.toString();
    }
    
    private String createOrderDeliveredText(String orderId) {
        StringBuilder text = new StringBuilder();
        text.append("Dear Customer,\n\n");
        text.append("Your order has been delivered successfully!\n\n");
        text.append("Order ID: ").append(orderId).append("\n\n");
        text.append("We hope you enjoy your purchase. Please let us know if you have any feedback!\n\n");
        text.append("Best regards,\n");
        text.append("VDT E-Commerce Team");
        return text.toString();
    }

    private String createOrderCancellationText(String orderId) {
        StringBuilder text = new StringBuilder();
        text.append("Dear Customer,\n\n");
        text.append("We regret to inform you that your order has been cancelled.\n\n");
        text.append("Order ID: ").append(orderId).append("\n\n");
        text.append("If you have any questions or need further assistance, please contact our support team.\n\n");
        text.append("Best regards,\n");
        text.append("VDT E-Commerce Team");
        return text.toString();
    }
}
