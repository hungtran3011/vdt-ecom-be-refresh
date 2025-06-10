package com.hungng3011.vdtecomberefresh.payment.controllers;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import com.hungng3011.vdtecomberefresh.payment.dtos.PaymentHistoryDto;
import com.hungng3011.vdtecomberefresh.payment.services.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;

    @GetMapping
    public ResponseEntity<PagedResponse<PaymentHistoryDto>> getAllPaymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor) {
        
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<PaymentHistoryDto> response = paymentHistoryService.getAllPaymentHistoryWithPagination(pageable, cursor);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/previous")
    public ResponseEntity<PagedResponse<PaymentHistoryDto>> getAllPaymentHistoryPrevious(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String cursor) {
        
        Pageable pageable = PageRequest.of(0, size);
        PagedResponse<PaymentHistoryDto> response = paymentHistoryService.getAllPaymentHistoryWithPreviousCursor(pageable, cursor);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<PaymentHistoryDto>> getPaymentHistoryByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor) {
        
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<PaymentHistoryDto> response = paymentHistoryService.getPaymentHistoryByUserWithPagination(userId, pageable, cursor);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/previous")
    public ResponseEntity<PagedResponse<PaymentHistoryDto>> getPaymentHistoryByUserPrevious(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String cursor) {
        
        Pageable pageable = PageRequest.of(0, size);
        PagedResponse<PaymentHistoryDto> response = paymentHistoryService.getPaymentHistoryByUserWithPreviousCursor(userId, pageable, cursor);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<PaymentHistoryDto>> getPaymentHistoryByStatus(
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor) {
        
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<PaymentHistoryDto> response = paymentHistoryService.getPaymentHistoryByStatusWithPagination(status, pageable, cursor);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/method/{method}")
    public ResponseEntity<PagedResponse<PaymentHistoryDto>> getPaymentHistoryByMethod(
            @PathVariable PaymentMethod method,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor) {
        
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<PaymentHistoryDto> response = paymentHistoryService.getPaymentHistoryByMethodWithPagination(method, pageable, cursor);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentHistoryDto> getPaymentHistoryById(@PathVariable String id) {
        PaymentHistoryDto paymentHistory = paymentHistoryService.getPaymentHistoryById(id);
        return ResponseEntity.ok(paymentHistory);
    }

    @PostMapping
    public ResponseEntity<PaymentHistoryDto> createPaymentHistory(@RequestBody PaymentHistoryDto paymentHistoryDto) {
        PaymentHistoryDto createdPaymentHistory = paymentHistoryService.createPaymentHistory(paymentHistoryDto);
        return ResponseEntity.ok(createdPaymentHistory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentHistoryDto> updatePaymentHistory(
            @PathVariable String id, 
            @RequestBody PaymentHistoryDto paymentHistoryDto) {
        PaymentHistoryDto updatedPaymentHistory = paymentHistoryService.updatePaymentHistory(id, paymentHistoryDto);
        return ResponseEntity.ok(updatedPaymentHistory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentHistory(@PathVariable String id) {
        paymentHistoryService.deletePaymentHistory(id);
        return ResponseEntity.noContent().build();
    }
}
