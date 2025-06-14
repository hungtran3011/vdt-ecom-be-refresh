package com.hungng3011.vdtecomberefresh.category.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;

/**
 * Đại diện cho danh mục sản phẩm trong hệ thống.
 *
 * Thiết kế này sử dụng quan hệ một-nhiều với `CategoryDynamicField` để cho phép mỗi danh mục
 * có một tập các trường động linh hoạt. Điều này giúp hệ thống dễ dàng hỗ trợ các danh mục
 * với thuộc tính khác nhau mà không cần thay đổi cấu trúc cơ sở dữ liệu khi có yêu cầu mới.
 *
 * Việc lưu trữ các trường động riêng biệt và liên kết với danh mục giúp mô hình mở rộng,
 * dễ bảo trì, thuận tiện thêm, xoá hoặc sửa thuộc tính riêng của từng danh mục khi nghiệp vụ thay đổi.
 * @see CategoryDynamicField
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_name", columnList = "name"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryDynamicField> dynamicFields;

    private String imageUrl;
}

