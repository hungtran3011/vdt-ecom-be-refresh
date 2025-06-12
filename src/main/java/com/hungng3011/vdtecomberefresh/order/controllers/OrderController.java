package com.hungng3011.vdtecomberefresh.order.controllers;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.order.dtos.OrderDto;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto orderDto) {
        log.info("Creating new order for user: {}", orderDto.getUserEmail() != null ? orderDto.getUserEmail() : "guest");
        try {
            OrderDto createdOrder = orderService.createOrder(orderDto);
            log.info("Successfully created order with ID: {}", createdOrder.getId());
            return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating order for user: {}", orderDto.getUserEmail() != null ? orderDto.getUserEmail() : "guest", e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrderDto>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor) {
        log.info("Fetching paginated orders - page: {}, size: {}, cursor: {}", page, size, cursor);
        try {
            PagedResponse<OrderDto> response = orderService.getAllOrdersWithPagination(page, size, cursor);
            log.info("Successfully retrieved {} orders (page: {})", 
                    response.getContent().size(), page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching paginated orders", e);
            throw e;
        }
    }

    @GetMapping("/previous")
    public ResponseEntity<PagedResponse<OrderDto>> getAllOrdersWithPreviousCursor(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String cursor) {
        log.info("Fetching previous page of orders with cursor: {}", cursor);
        try {
            PagedResponse<OrderDto> response = orderService.getAllOrdersWithPreviousCursor(page, size, cursor);
            log.info("Successfully retrieved {} orders for previous page", 
                    response.getContent().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching previous page of orders", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable String id) {
        log.info("Fetching order with ID: {}", id);
        try {
            OrderDto order = orderService.getOrderById(id);
            log.info("Successfully retrieved order with ID: {}", id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error fetching order with ID: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<OrderDto>> getCurrentUserOrders(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        log.info("Fetching orders for user: {}", userEmail);
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("No email found in JWT token for user: {}", jwt.getSubject());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<OrderDto> orders = orderService.getAllOrdersByUserEmail(userEmail);
            log.info("Successfully retrieved {} orders for user: {}", orders.size(), userEmail);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders for user: {}", userEmail, e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(@PathVariable String id, @RequestBody OrderDto orderDto) {
        log.info("Updating order with ID: {}", id);
        try {
            OrderDto updatedOrder = orderService.updateOrder(id, orderDto);
            log.info("Successfully updated order with ID: {}", id);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            log.error("Error updating order with ID: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        log.info("Deleting order with ID: {}", id);
        try {
            orderService.deleteOrder(id);
            log.info("Successfully deleted order with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting order with ID: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable String id, 
            @RequestBody Map<String, String> statusRequest,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Updating order status for order ID: {}", id);
        
        // Extract roles from JWT properly
        List<String> roles = extractRoles(jwt);
        log.debug("User {} has roles: {}", jwt.getClaimAsString("email"), roles);
        if (roles == null || !roles.contains("admin")) {
            log.warn("Unauthorized attempt to update order status by user: {} with roles: {}", 
                    jwt.getClaimAsString("email"), roles);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            String statusValue = statusRequest.get("status");
            if (statusValue == null || statusValue.trim().isEmpty()) {
                log.error("Status value is required for order status update");
                return ResponseEntity.badRequest().build();
            }
            
            OrderStatus newStatus;
            try {
                newStatus = OrderStatus.valueOf(statusValue.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid order status: {}", statusValue);
                return ResponseEntity.badRequest().build();
            }
            
            OrderDto updatedOrder = orderService.updateOrderStatus(id, newStatus);
            log.info("Successfully updated order status for ID: {} to {}", id, newStatus);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            log.error("Error updating order status for ID: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        log.info("Cancelling order with ID: {} for user: {}", id, userEmail);
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("No email found in JWT token for user: {}", jwt.getSubject());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OrderDto cancelledOrder = orderService.cancelOrder(id, userEmail);
            log.info("Successfully cancelled order with ID: {}", id);
            return ResponseEntity.ok(cancelledOrder);
        } catch (Exception e) {
            log.error("Error cancelling order with ID: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<OrderDto> getOrderTracking(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        log.info("Getting tracking info for order: {} for user: {}", id, userEmail);
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("No email found in JWT token for user: {}", jwt.getSubject());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OrderDto order = orderService.getOrderTracking(id, userEmail);
            log.info("Successfully retrieved tracking info for order: {}", id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error getting tracking info for order: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/reorder")
    public ResponseEntity<OrderDto> reorderOrder(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        log.info("Reordering order: {} for user: {}", id, userEmail);
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("No email found in JWT token for user: {}", jwt.getSubject());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OrderDto newOrder = orderService.reorderOrder(id, userEmail);
            log.info("Successfully created reorder with ID: {} from original order: {}", newOrder.getId(), id);
            return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);
        } catch (Exception e) {
            log.error("Error reordering order: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String userEmail = jwt.getClaimAsString("email");
        log.info("Exporting orders for user: {} with filters - status: {}, dateRange: {} to {}", 
                userEmail, status, startDate, endDate);
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("No email found in JWT token for user: {}", jwt.getSubject());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;
            
            String csvContent = orderService.exportOrdersToCSV(userEmail, orderStatus, start, end);
            
            log.info("Successfully exported orders for user: {}", userEmail);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=\"orders_export.csv\"")
                    .body(csvContent);
        } catch (Exception e) {
            log.error("Error exporting orders for user: {}", userEmail, e);
            throw e;
        }
    }

    /**
     * Extract roles from JWT token properly handling Keycloak's nested structure
     * @param jwt The JWT token
     * @return List of role names (without ROLE_ prefix)
     */
    private List<String> extractRoles(Jwt jwt) {
        try {
            // Debug: Log all claims to understand JWT structure
            log.debug("JWT Claims: {}", jwt.getClaims());
            
            Object realmAccess = jwt.getClaims().getOrDefault("realm_access", Map.of());
            log.debug("Realm access: {}", realmAccess);
            
            if (realmAccess instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
                Object rolesObj = realmAccessMap.getOrDefault("roles", List.of());
                log.debug("Roles object: {}", rolesObj);
                
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) rolesObj;
                    log.debug("Extracted roles: {}", rolesList);
                    return rolesList;
                }
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error extracting roles from JWT", e);
            return List.of();
        }
    }
}