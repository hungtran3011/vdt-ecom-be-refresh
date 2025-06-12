package com.hungng3011.vdtecomberefresh.search.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Constants and utility methods for search functionality
 */
public final class SearchConstants {
    
    // Index names
    public static final String PRODUCTS_INDEX = "products";
    
    // Redis keys for analytics
    public static final String POPULAR_QUERIES_KEY = "search:popular_queries";
    public static final String FAILED_QUERIES_KEY = "search:failed_queries";
    public static final String SEARCH_STATS_KEY_PREFIX = "search:stats:";
    public static final String CLICK_TRACKING_KEY_PREFIX = "search:clicks:";
    
    // Search configuration
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_SUGGESTION_LIMIT = 20;
    public static final int DEFAULT_SUGGESTION_LIMIT = 10;
    
    // Analytics retention (in days)
    public static final int ANALYTICS_RETENTION_DAYS = 30;
    public static final int POPULAR_QUERIES_RETENTION_DAYS = 7;
    
    // Elasticsearch field names
    public static final class Fields {
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String SKU = "sku";
        public static final String BRAND = "brand";
        public static final String CATEGORY_ID = "categoryId";
        public static final String CATEGORY_NAME = "categoryName";
        public static final String BASE_PRICE = "basePrice";
        public static final String SALE_PRICE = "salePrice";
        public static final String IN_STOCK = "inStock";
        public static final String AVERAGE_RATING = "averageRating";
        public static final String TAGS = "tags";
        public static final String AUTOCOMPLETE_TEXT = "autocompleteText";
        public static final String SEARCHABLE_TEXT = "searchableText";
        public static final String POPULARITY_SCORE = "popularityScore";
        public static final String SEARCH_RANKING_SCORE = "searchRankingScore";
        public static final String CREATED_AT = "createdAt";
        public static final String UPDATED_AT = "updatedAt";
        public static final String IS_FEATURED = "isFeatured";
        public static final String IS_NEW_ARRIVAL = "isNewArrival";
        public static final String IS_BEST_SELLER = "isBestSeller";
        
        private Fields() {
            // Utility class
        }
    }
    
    // Search boost values
    public static final class Boost {
        public static final float NAME_BOOST = 3.0f;
        public static final float DESCRIPTION_BOOST = 2.0f;
        public static final float BRAND_BOOST = 2.5f;
        public static final float SKU_BOOST = 4.0f;
        public static final float TAGS_BOOST = 1.5f;
        public static final float AUTOCOMPLETE_BOOST = 2.0f;
        public static final float EXACT_MATCH_BOOST = 5.0f;
        
        private Boost() {
            // Utility class
        }
    }
    
    // Common search synonyms for better search results
    public static final List<String> SYNONYMS = Arrays.asList(
        "laptop,notebook,computer",
        "phone,mobile,smartphone,cellphone",
        "tv,television,telly",
        "headphones,earphones,earbuds,headset",
        "tablet,pad,slate",
        "watch,smartwatch,timepiece",
        "camera,cam,photography",
        "gaming,game,gamer",
        "wireless,cordless",
        "bluetooth,bt",
        "usb,universal serial bus",
        "hdmi,high definition multimedia interface",
        "4k,ultra hd,uhd",
        "led,light emitting diode",
        "oled,organic led",
        "lcd,liquid crystal display",
        "ssd,solid state drive",
        "hdd,hard disk drive,hard drive",
        "ram,memory,random access memory",
        "cpu,processor,central processing unit",
        "gpu,graphics card,video card",
        "wifi,wi-fi,wireless"
    );
    
    // Price range buckets for faceted search
    public static final class PriceRanges {
        public static final String UNDER_50 = "Under $50";
        public static final String RANGE_50_100 = "$50 - $100";
        public static final String RANGE_100_250 = "$100 - $250";
        public static final String RANGE_250_500 = "$250 - $500";
        public static final String RANGE_500_1000 = "$500 - $1000";
        public static final String OVER_1000 = "Over $1000";
        
        private PriceRanges() {
            // Utility class
        }
    }
    
    // Rating ranges for faceted search
    public static final class RatingRanges {
        public static final String FIVE_STARS = "5 Stars";
        public static final String FOUR_PLUS_STARS = "4+ Stars";
        public static final String THREE_PLUS_STARS = "3+ Stars";
        public static final String TWO_PLUS_STARS = "2+ Stars";
        public static final String ONE_PLUS_STARS = "1+ Stars";
        
        private RatingRanges() {
            // Utility class
        }
    }
    
    // Search validation
    public static final int MIN_QUERY_LENGTH = 1;
    public static final int MAX_QUERY_LENGTH = 200;
    public static final int MIN_SUGGESTION_QUERY_LENGTH = 2;
    
    // Error messages
    public static final class ErrorMessages {
        public static final String QUERY_TOO_SHORT = "Search query must be at least " + MIN_QUERY_LENGTH + " characters";
        public static final String QUERY_TOO_LONG = "Search query cannot exceed " + MAX_QUERY_LENGTH + " characters";
        public static final String INVALID_PAGE_SIZE = "Page size must be between 1 and " + MAX_PAGE_SIZE;
        public static final String INVALID_PAGE_NUMBER = "Page number must be 0 or greater";
        public static final String SUGGESTION_QUERY_TOO_SHORT = "Suggestion query must be at least " + MIN_SUGGESTION_QUERY_LENGTH + " characters";
        public static final String ELASTICSEARCH_UNAVAILABLE = "Search service is temporarily unavailable";
        
        private ErrorMessages() {
            // Utility class
        }
    }
    
    private SearchConstants() {
        // Utility class
    }
    
    /**
     * Validate search request parameters
     */
    public static void validateSearchRequest(String query, int page, int size) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(ErrorMessages.QUERY_TOO_LONG);
        }
        
        if (page < 0) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_PAGE_NUMBER);
        }
        
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_PAGE_SIZE);
        }
    }
    
    /**
     * Validate suggestion request parameters
     */
    public static void validateSuggestionRequest(String query, int limit) {
        if (query == null || query.trim().length() < MIN_SUGGESTION_QUERY_LENGTH) {
            throw new IllegalArgumentException(ErrorMessages.SUGGESTION_QUERY_TOO_SHORT);
        }
        
        if (limit < 1 || limit > MAX_SUGGESTION_LIMIT) {
            throw new IllegalArgumentException("Suggestion limit must be between 1 and " + MAX_SUGGESTION_LIMIT);
        }
    }
    
    /**
     * Sanitize search query for security
     */
    public static String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        
        return query.trim()
            .replaceAll("[<>\"'&]", "") // Remove potentially dangerous characters
            .replaceAll("\\s+", " "); // Normalize whitespace
    }
    
    /**
     * Build Redis key for daily search stats
     */
    public static String buildDailyStatsKey(String date) {
        return SEARCH_STATS_KEY_PREFIX + date;
    }
    
    /**
     * Build Redis key for click tracking
     */
    public static String buildClickTrackingKey(String query) {
        return CLICK_TRACKING_KEY_PREFIX + query.toLowerCase().replaceAll("\\s+", "_");
    }
}
