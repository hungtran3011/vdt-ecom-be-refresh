package com.hungng3011.vdtecomberefresh.mail.controllers;

import com.hungng3011.vdtecomberefresh.mail.dtos.EmailNotificationDto;
import com.hungng3011.vdtecomberefresh.mail.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mail/test")
@RequiredArgsConstructor
@Slf4j
public class MailTestController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendTestEmail(@RequestParam String to) {
        try {
            EmailNotificationDto testEmail = EmailNotificationDto.builder()
                .to(to)
                .subject("VDT E-Commerce - Mail System Test")
                .textContent("This is a test email to verify the mail system is working correctly with the internal mailserver.")
                .build();

            emailService.sendEmailSync(testEmail);
            log.info("Test email sent successfully to: {}", to);
            return ResponseEntity.ok("Test email sent successfully to: " + to);
        } catch (Exception e) {
            log.error("Failed to send test email to: {} - Error: {}", to, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Failed to send test email: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Mail test controller is healthy");
    }
}
