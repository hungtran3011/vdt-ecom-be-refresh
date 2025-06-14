package com.hungng3011.vdtecomberefresh.product.entities;

import com.hungng3011.vdtecomberefresh.category.entities.Category;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Đại diện cho sản phẩm trong hệ thống thương mại điện tử.
 *
 * Thiết kế này cho phép mỗi sản phẩm thuộc về một danh mục (\@see Category),
 * đồng thời hỗ trợ các trường động linh hoạt thông qua quan hệ với \@see ProductDynamicValue.
 * Điều này giúp hệ thống dễ dàng mở rộng thuộc tính sản phẩm mà không cần thay đổi cấu trúc bảng sản phẩm.
 *
 * - Trường `dynamicValues` lưu giá trị các thuộc tính động, liên kết với định nghĩa trường động của danh mục (\@see CategoryDynamicField).
 * - Trường `variations` hỗ trợ các biến thể sản phẩm (ví dụ: màu sắc, kích thước).
 * - Trường `images` lưu danh sách URL ảnh sản phẩm dưới dạng JSONB, thuận tiện cho việc lưu trữ nhiều ảnh.
 *
 * Cách tiếp cận này giúp hệ thống dễ bảo trì, mở rộng, thuận tiện thêm/xoá/sửa thuộc tính động cho từng sản phẩm khi nghiệp vụ thay đổi.
 *
 * \@see Category
 * \@see ProductDynamicValue
 * \@see CategoryDynamicField
 * \@see Variation
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_category_id", columnList = "category_id"),
        @Index(name = "idx_product_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String sku;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String metaDescription;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private List<String> images; // List of image URLs

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductDynamicValue> dynamicValues;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Variation> variations;
}

