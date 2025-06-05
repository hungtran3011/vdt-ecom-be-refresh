package com.hungng3011.vdtecomberefresh.mail.services;

import com.hungng3011.vdtecomberefresh.mail.config.MailConfig;
import com.hungng3011.vdtecomberefresh.mail.dtos.EmailNotificationDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailConfig mailConfig;
    
    @Async
    public void sendEmailAsync(EmailNotificationDto emailDto) {
        if (!mailConfig.isEnabled()) {
            log.info("Mail sending is disabled. Skipping email to: {}", emailDto.getTo());
            return;
        }
        
        sendEmail(emailDto);
    }
    
    public void sendEmailSync(EmailNotificationDto emailDto) {
        if (!mailConfig.isEnabled()) {
            log.info("Mail sending is disabled. Skipping email to: {}", emailDto.getTo());
            return;
        }
        
        sendEmail(emailDto);
    }
    
    private void sendEmail(EmailNotificationDto emailDto) {
        try {
            log.info("Preparing to send email to: {} with subject: {}", emailDto.getTo(), emailDto.getSubject());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message, 
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                StandardCharsets.UTF_8.name()
            );
            
            // Set email headers
            helper.setFrom(mailConfig.getFrom());
            helper.setTo(emailDto.getTo());
            helper.setSubject(emailDto.getSubject());
            
            if (emailDto.getCc() != null && !emailDto.getCc().trim().isEmpty()) {
                helper.setCc(emailDto.getCc());
            }
            
            if (emailDto.getBcc() != null && !emailDto.getBcc().trim().isEmpty()) {
                helper.setBcc(emailDto.getBcc());
            }
            
            // Process content
            String htmlContent = processEmailContent(emailDto);
            helper.setText(emailDto.getTextContent(), htmlContent);
            
            // Send email
            mailSender.send(message);
            log.info("Email sent successfully to: {}", emailDto.getTo());
            
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to: {} - Error: {}", emailDto.getTo(), e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    private String processEmailContent(EmailNotificationDto emailDto) {
        if (emailDto.getTemplate() != null && !emailDto.getTemplate().trim().isEmpty()) {
            return processTemplate(emailDto.getTemplate(), emailDto);
        } else if (emailDto.getHtmlContent() != null) {
            return emailDto.getHtmlContent();
        } else {
            return createDefaultHtmlContent(emailDto);
        }
    }
    
    private String processTemplate(String templateName, EmailNotificationDto emailDto) {
        try {
            Context context = new Context(Locale.getDefault());
            
            // Add common variables
            context.setVariable("orderId", emailDto.getOrderId());
            context.setVariable("transactionId", emailDto.getTransactionId());
            context.setVariable("userId", emailDto.getUserId());
            context.setVariable("subject", emailDto.getSubject());
            
            // Add custom variables
            if (emailDto.getVariables() != null) {
                emailDto.getVariables().forEach(context::setVariable);
            }
            
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.warn("Failed to process template: {} - Using default content. Error: {}", 
                     templateName, e.getMessage());
            return createDefaultHtmlContent(emailDto);
        }
    }
    
    private String createDefaultHtmlContent(EmailNotificationDto emailDto) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head><body>");
        html.append("<h2>").append(emailDto.getSubject()).append("</h2>");
        
        if (emailDto.getOrderId() != null) {
            html.append("<p><strong>Order ID:</strong> ").append(emailDto.getOrderId()).append("</p>");
        }
        
        if (emailDto.getTransactionId() != null) {
            html.append("<p><strong>Transaction ID:</strong> ").append(emailDto.getTransactionId()).append("</p>");
        }
        
        if (emailDto.getTextContent() != null) {
            html.append("<div>").append(emailDto.getTextContent().replace("\n", "<br>")).append("</div>");
        }
        
        html.append("<hr>");
        html.append("<p><small>This is an automated message from VDT E-Commerce.</small></p>");
        html.append("</body></html>");
        
        return html.toString();
    }
}
