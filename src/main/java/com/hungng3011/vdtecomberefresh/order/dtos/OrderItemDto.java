package com.hungng3011.vdtecomberefresh.order.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Double price;
    private Double totalPrice;
}
