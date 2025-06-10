package com.hungng3011.vdtecomberefresh.payment.services;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import com.hungng3011.vdtecomberefresh.payment.dtos.PaymentHistoryDto;
import com.hungng3011.vdtecomberefresh.payment.entities.PaymentHistory;
import com.hungng3011.vdtecomberefresh.payment.mappers.PaymentHistoryMapper;
import com.hungng3011.vdtecomberefresh.payment.repositories.PaymentHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentHistoryMapper paymentHistoryMapper;

    @Transactional
    public PaymentHistoryDto createPaymentHistory(PaymentHistoryDto paymentHistoryDto) {
        log.info("Creating payment history for order: {} and user: {}", 
                paymentHistoryDto.getOrderId(), paymentHistoryDto.getUserId());
        try {
            PaymentHistory paymentHistory = paymentHistoryMapper.toEntity(paymentHistoryDto);
            PaymentHistory savedPaymentHistory = paymentHistoryRepository.save(paymentHistory);
            PaymentHistoryDto savedDto = paymentHistoryMapper.toDto(savedPaymentHistory);
            log.info("Successfully created payment history with ID: {}", savedDto.getId());
            return savedDto;
        } catch (Exception e) {
            log.error("Error creating payment history for order: {}", paymentHistoryDto.getOrderId(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryDto> getAllPaymentHistory() {
        return paymentHistoryRepository.findAll().stream()
                .map(paymentHistoryMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get payment history with cursor-based pagination
     * @param pageable Pageable information
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing payment history and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentHistoryDto> getAllPaymentHistoryWithPagination(Pageable pageable, String cursor) {
        log.info("Finding payment history with pagination - page: {}, size: {}, cursor: {}", 
                pageable.getPageNumber(), pageable.getPageSize(), cursor);
        
        Long cursorLong = cursor != null ? Long.parseLong(cursor) : null;
        List<PaymentHistory> paymentHistories;
        
        if (cursorLong != null) {
            // Use cursor-based pagination
            paymentHistories = paymentHistoryRepository.findWithCursorAfter(cursorLong, pageable);
        } else {
            // First page - use standard pagination
            paymentHistories = paymentHistoryRepository.findWithCursorAfter(null, pageable);
        }

        List<PaymentHistoryDto> paymentHistoryDtos = paymentHistories.stream()
                .map(paymentHistoryMapper::toDto)
                .collect(Collectors.toList());

        // Get total count for pagination metadata
        long totalElements = paymentHistoryRepository.countAllPaymentHistory();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        // Calculate cursors
        Object nextCursor = null;
        Object previousCursor = null;
        
        if (!paymentHistories.isEmpty()) {
            nextCursor = paymentHistories.get(paymentHistories.size() - 1).getId().toString();
            if (cursor != null) {
                previousCursor = cursor;
            }
        }

        // Build pagination metadata
        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(paymentHistories.size() == pageable.getPageSize() && (pageable.getPageNumber() + 1) * pageable.getPageSize() < totalElements)
                .hasPrevious(pageable.getPageNumber() > 0 || cursor != null)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        log.info("Retrieved {} payment history records (page: {}, total: {})", 
                paymentHistoryDtos.size(), pageable.getPageNumber(), totalElements);

        return PagedResponse.<PaymentHistoryDto>builder()
                .content(paymentHistoryDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get payment history with cursor-based pagination (previous page)
     * @param pageable Pageable information
     * @param cursor Cursor for pagination (ID of first item from current page)
     * @return PagedResponse containing payment history and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentHistoryDto> getAllPaymentHistoryWithPreviousCursor(Pageable pageable, String cursor) {
        log.info("Finding previous payment history with cursor: {}", cursor);
        
        Long cursorLong = Long.parseLong(cursor);
        List<PaymentHistory> paymentHistories = paymentHistoryRepository.findWithCursorBefore(cursorLong, pageable);
        
        // Reverse the order since we queried in DESC order
        java.util.Collections.reverse(paymentHistories);

        List<PaymentHistoryDto> paymentHistoryDtos = paymentHistories.stream()
                .map(paymentHistoryMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = paymentHistoryRepository.countAllPaymentHistory();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        Object nextCursor = cursor;
        Object previousCursor = !paymentHistories.isEmpty() ? paymentHistories.get(0).getId().toString() : null;

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(Math.max(0, pageable.getPageNumber() - 1))
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(true)
                .hasPrevious(paymentHistories.size() == pageable.getPageSize())
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        return PagedResponse.<PaymentHistoryDto>builder()
                .content(paymentHistoryDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get payment history for a specific user with cursor-based pagination
     * @param userId User ID to filter by
     * @param pageable Pageable information
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing payment history and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentHistoryDto> getPaymentHistoryByUserWithPagination(String userId, Pageable pageable, String cursor) {
        log.info("Finding payment history for user: {} with pagination - page: {}, size: {}, cursor: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize(), cursor);
        
        Long cursorLong = cursor != null ? Long.parseLong(cursor) : null;
        List<PaymentHistory> paymentHistories;
        
        if (cursorLong != null) {
            paymentHistories = paymentHistoryRepository.findByUserIdWithCursorAfter(userId, cursorLong, pageable);
        } else {
            paymentHistories = paymentHistoryRepository.findByUserIdWithCursorAfter(userId, null, pageable);
        }

        List<PaymentHistoryDto> paymentHistoryDtos = paymentHistories.stream()
                .map(paymentHistoryMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = paymentHistoryRepository.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        Object nextCursor = null;
        Object previousCursor = null;
        
        if (!paymentHistories.isEmpty()) {
            nextCursor = paymentHistories.get(paymentHistories.size() - 1).getId().toString();
            if (cursor != null) {
                previousCursor = cursor;
            }
        }

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(paymentHistories.size() == pageable.getPageSize() && (pageable.getPageNumber() + 1) * pageable.getPageSize() < totalElements)
                .hasPrevious(pageable.getPageNumber() > 0 || cursor != null)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        log.info("Retrieved {} payment history records for user: {} (page: {}, total: {})", 
                paymentHistoryDtos.size(), userId, pageable.getPageNumber(), totalElements);

        return PagedResponse.<PaymentHistoryDto>builder()
                .content(paymentHistoryDtos)
                .pagination(pagination)
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentHistoryDto getPaymentHistoryById(String id) {
        Long idLong = Long.parseLong(id);
        PaymentHistory paymentHistory = paymentHistoryRepository.findById(idLong)
                .orElseThrow(() -> new EntityNotFoundException("Payment history not found with id: " + id));
        return paymentHistoryMapper.toDto(paymentHistory);
    }

    @Transactional
    public PaymentHistoryDto updatePaymentHistory(String id, PaymentHistoryDto paymentHistoryDto) {
        Long idLong = Long.parseLong(id);
        PaymentHistory existingPaymentHistory = paymentHistoryRepository.findById(idLong)
                .orElseThrow(() -> new EntityNotFoundException("Payment history not found with id: " + id));

        paymentHistoryMapper.updateEntityFromDto(paymentHistoryDto, existingPaymentHistory);
        existingPaymentHistory.setUpdatedAt(LocalDateTime.now());

        PaymentHistory updatedPaymentHistory = paymentHistoryRepository.save(existingPaymentHistory);
        return paymentHistoryMapper.toDto(updatedPaymentHistory);
    }

    @Transactional
    public void deletePaymentHistory(String id) {
        Long idLong = Long.parseLong(id);
        if (!paymentHistoryRepository.existsById(idLong)) {
            throw new EntityNotFoundException("Payment history not found with id: " + id);
        }
        paymentHistoryRepository.deleteById(idLong);
    }

    /**
     * Get payment history for a specific user with cursor-based pagination (previous page)
     * @param userId User ID to filter by
     * @param pageable Pageable information
     * @param cursor Cursor for pagination (ID of first item from current page)
     * @return PagedResponse containing payment history and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentHistoryDto> getPaymentHistoryByUserWithPreviousCursor(String userId, Pageable pageable, String cursor) {
        log.info("Finding previous payment history for user: {} with cursor: {}", userId, cursor);
        
        Long cursorLong = Long.parseLong(cursor);
        List<PaymentHistory> paymentHistories = paymentHistoryRepository.findByUserIdWithCursorBefore(userId, cursorLong, pageable);
        
        // Reverse the order since we queried in DESC order
        java.util.Collections.reverse(paymentHistories);

        List<PaymentHistoryDto> paymentHistoryDtos = paymentHistories.stream()
                .map(paymentHistoryMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = paymentHistoryRepository.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        Object nextCursor = cursor;
        Object previousCursor = !paymentHistories.isEmpty() ? paymentHistories.get(0).getId().toString() : null;

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(Math.max(0, pageable.getPageNumber() - 1))
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(true)
                .hasPrevious(paymentHistories.size() == pageable.getPageSize())
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        return PagedResponse.<PaymentHistoryDto>builder()
                .content(paymentHistoryDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get payment history by status with cursor-based pagination
     * @param status Payment status to filter by
     * @param pageable Pageable information
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing payment history and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentHistoryDto> getPaymentHistoryByStatusWithPagination(PaymentStatus status, Pageable pageable, String cursor) {
        log.info("Finding payment history by status: {} with pagination - page: {}, size: {}, cursor: {}", 
                status, pageable.getPageNumber(), pageable.getPageSize(), cursor);
        
        Long cursorLong = cursor != null ? Long.parseLong(cursor) : null;
        List<PaymentHistory> paymentHistories;
        
        if (cursorLong != null) {
            paymentHistories = paymentHistoryRepository.findByStatusWithCursorAfter(status, cursorLong, pageable);
        } else {
            paymentHistories = paymentHistoryRepository.findByStatusWithCursorAfter(status, null, pageable);
        }

        List<PaymentHistoryDto> paymentHistoryDtos = paymentHistories.stream()
                .map(paymentHistoryMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = paymentHistoryRepository.countByStatus(status);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        Object nextCursor = null;
        Object previousCursor = null;
        
        if (!paymentHistories.isEmpty()) {
            nextCursor = paymentHistories.get(paymentHistories.size() - 1).getId().toString();
            if (cursor != null) {
                previousCursor = cursor;
            }
        }

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(paymentHistories.size() == pageable.getPageSize() && (pageable.getPageNumber() + 1) * pageable.getPageSize() < totalElements)
                .hasPrevious(pageable.getPageNumber() > 0 || cursor != null)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        log.info("Retrieved {} payment history records by status: {} (page: {}, total: {})", 
                paymentHistoryDtos.size(), status, pageable.getPageNumber(), totalElements);

        return PagedResponse.<PaymentHistoryDto>builder()
                .content(paymentHistoryDtos)
                .pagination(pagination)
                .build();
    }

    /**
     * Get payment history by method with cursor-based pagination
     * @param method Payment method to filter by
     * @param pageable Pageable information
     * @param cursor Optional cursor for pagination (ID of last item from previous page)
     * @return PagedResponse containing payment history and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentHistoryDto> getPaymentHistoryByMethodWithPagination(PaymentMethod method, Pageable pageable, String cursor) {
        log.info("Finding payment history by method: {} with pagination - page: {}, size: {}, cursor: {}", 
                method, pageable.getPageNumber(), pageable.getPageSize(), cursor);
        
        Long cursorLong = cursor != null ? Long.parseLong(cursor) : null;
        List<PaymentHistory> paymentHistories;
        
        if (cursorLong != null) {
            paymentHistories = paymentHistoryRepository.findByPaymentMethodWithCursorAfter(method, cursorLong, pageable);
        } else {
            paymentHistories = paymentHistoryRepository.findByPaymentMethodWithCursorAfter(method, null, pageable);
        }

        List<PaymentHistoryDto> paymentHistoryDtos = paymentHistories.stream()
                .map(paymentHistoryMapper::toDto)
                .collect(Collectors.toList());

        long totalElements = paymentHistoryRepository.countByPaymentMethod(method);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        Object nextCursor = null;
        Object previousCursor = null;
        
        if (!paymentHistories.isEmpty()) {
            nextCursor = paymentHistories.get(paymentHistories.size() - 1).getId().toString();
            if (cursor != null) {
                previousCursor = cursor;
            }
        }

        PagedResponse.PaginationMetadata pagination = PagedResponse.PaginationMetadata.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(paymentHistories.size() == pageable.getPageSize() && (pageable.getPageNumber() + 1) * pageable.getPageSize() < totalElements)
                .hasPrevious(pageable.getPageNumber() > 0 || cursor != null)
                .nextCursor(nextCursor)
                .previousCursor(previousCursor)
                .build();

        log.info("Retrieved {} payment history records by method: {} (page: {}, total: {})", 
                paymentHistoryDtos.size(), method, pageable.getPageNumber(), totalElements);

        return PagedResponse.<PaymentHistoryDto>builder()
                .content(paymentHistoryDtos)
                .pagination(pagination)
                .build();
    }
}
