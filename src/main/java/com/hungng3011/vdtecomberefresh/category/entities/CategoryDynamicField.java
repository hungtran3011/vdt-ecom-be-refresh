package com.hungng3011.vdtecomberefresh.category.entities;

import com.hungng3011.vdtecomberefresh.category.enums.AppliesTo;
import com.hungng3011.vdtecomberefresh.category.enums.FieldType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đại diện cho định nghĩa trường động của một danh mục sản phẩm.
 *
 * Thiết kế này cho phép mỗi danh mục có tập trường động linh hoạt, giúp mở rộng hệ thống
 * mà không cần thay đổi cấu trúc cơ sở dữ liệu khi phát sinh yêu cầu mới. Mỗi trường động
 * liên kết với một danh mục cụ thể, cho phép các danh mục khác nhau có thuộc tính riêng biệt.
 * Việc sử dụng enum cho `fieldType` và `appliesTo` đảm bảo an toàn kiểu và rõ ràng trong nghiệp vụ.
 *
 * - `fieldType`: Kiểu dữ liệu của trường (ví dụ: text, number, boolean).
 * - `appliesTo`: Xác định trường áp dụng cho sản phẩm hay biến thể.
 *
 * Cách tiếp cận này giúp hệ thống dễ bảo trì, mở rộng, thuận tiện thêm/xoá/sửa thuộc tính động
 * cho từng danh mục khi nghiệp vụ thay đổi.
 *
 * @see Category
 * @see FieldType
 * @see AppliesTo
 */
@Entity
@Table(name = "category_dynamic_fields", indexes = {
        @Index(name = "idx_category_dynamic_field_category_id", columnList = "category_id"),
        @Index(name = "idx_category_dynamic_field_field_name", columnList = "field_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDynamicField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType; // Enum for text, number, boolean, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppliesTo appliesTo; // Enum: PRODUCT or VARIATION

    private boolean required = false;
}

