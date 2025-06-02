package com.hungng3011.vdtecomberefresh.stock.entities;

import com.hungng3011.vdtecomberefresh.product.entities.Product;
import com.hungng3011.vdtecomberefresh.product.entities.Variation;
import com.hungng3011.vdtecomberefresh.stock.enums.StockActionState;
import com.hungng3011.vdtecomberefresh.stock.enums.StockStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock", indexes = {
        @Index(name = "idx_stock_sku", columnList = "sku"),
        @Index(name = "idx_stock_product_id", columnList = "product_id"),
        @Index(name = "idx_stock_status", columnList = "status")
})
@Data
@NoArgsConstructor
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "stock_variation",
            joinColumns = @JoinColumn(name = "stock_id"),
            inverseJoinColumns = @JoinColumn(name = "variation_id")
    )
    private final List<Variation> variations = new ArrayList<>();

    @Column(nullable = false)
    @PositiveOrZero
    private Integer quantity;

    @Column(nullable = false)
    @PositiveOrZero
    private Integer lowStockThreshold;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StockStatus status = StockStatus.IN_STOCK;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDate expectedRestockDate;

    @Column
    @PositiveOrZero
    private Integer maxPreOrderQuantity;

    @Column
    @PositiveOrZero
    private Integer preOrderCount = 0;

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<StockHistory> history = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void prePersist() {
        updatedAt = LocalDateTime.now();
        updateStatus();
    }

    public void updateStatus() {
        if (quantity <= 0) {
            status = StockStatus.OUT_OF_STOCK;
        } else if (quantity <= lowStockThreshold) {
            status = StockStatus.LOW_STOCK;
        } else {
            status = StockStatus.IN_STOCK;
        }
    }

    // Add these helper methods
    public boolean canPreOrder() {
        return status == StockStatus.PRE_ORDER &&
                preOrderCount < maxPreOrderQuantity;
    }

    public boolean incrementPreOrderCount(int quantity) {
        if (canPreOrder() && (preOrderCount + quantity) <= maxPreOrderQuantity) {
            preOrderCount += quantity;
            return true;
        }
        return false;
    }

    public StockHistory addHistory(StockActionState action, Integer quantityBefore, String reference, String updatedBy) {
        StockHistory historyEntry = new StockHistory();
        historyEntry.setStock(this);
        historyEntry.setAction(action);
        historyEntry.setQuantityBefore(quantityBefore);
        historyEntry.setQuantityAfter(this.quantity);
        historyEntry.setReference(reference);
        historyEntry.setUpdatedBy(updatedBy);
        historyEntry.setTimestamp(LocalDateTime.now());

        history.add(historyEntry);
        return historyEntry;
    }
}