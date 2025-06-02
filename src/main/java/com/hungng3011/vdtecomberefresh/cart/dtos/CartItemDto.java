package com.hungng3011.vdtecomberefresh.cart.dtos;

import com.hungng3011.vdtecomberefresh.product.dtos.VariationDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private List<VariationDto> selectedVariations = new ArrayList<>();
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String stockSku;
    private LocalDateTime addedAt;
}
