package com.hungng3011.vdtecomberefresh.stock.entities;

import com.hungng3011.vdtecomberefresh.stock.enums.StockActionState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_history", indexes = {
        @Index(name = "idx_stock_history_stock_id", columnList = "stock_id"),
        @Index(name = "idx_stock_history_action", columnList = "action"),
        @Index(name = "idx_stock_history_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private Integer quantityBefore;

    @Column(nullable = false)
    private Integer quantityAfter;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StockActionState action;

    @Column
    private String reference;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private String updatedBy;
    
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}