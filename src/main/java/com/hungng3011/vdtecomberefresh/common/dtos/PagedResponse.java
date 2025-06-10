package com.hungng3011.vdtecomberefresh.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class PagedResponse<T> {
//     private List<T> content;
//     private PaginationMetadata pagination;

//     @Data
//     @Builder
//     @NoArgsConstructor
//     @AllArgsConstructor
//     public static class PaginationMetadata {
//         private int page;
//         private int size;
//         private long totalElements;
//         private int totalPages;
//         private boolean hasNext;
//         private boolean hasPrevious;
//         private Long nextCursor;
//         private Long previousCursor;
//     }
// }
// import lombok.NoArgsConstructor;

// import java.util.List;

/**
 * Generic paginated response wrapper for API endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    
    private List<T> content;
    private PaginationMetadata pagination;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationMetadata {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private Object nextCursor;
        private Object previousCursor;
    }
}
