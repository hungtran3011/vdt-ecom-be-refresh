package com.hungng3011.vdtecomberefresh.search.services;

import com.hungng3011.vdtecomberefresh.search.documents.ProductSearchDocument;
import com.hungng3011.vdtecomberefresh.search.dtos.SearchRequestDto;
import com.hungng3011.vdtecomberefresh.search.dtos.SearchResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for tracking and analyzing search behavior
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String SEARCH_ANALYTICS_PREFIX = "search:analytics:";
    private static final String POPULAR_QUERIES_KEY = SEARCH_ANALYTICS_PREFIX + "popular:queries";
    private static final String FAILED_QUERIES_KEY = SEARCH_ANALYTICS_PREFIX + "failed:queries";
    private static final String USER_SEARCHES_PREFIX = SEARCH_ANALYTICS_PREFIX + "user:";
    private static final String DAILY_STATS_PREFIX = SEARCH_ANALYTICS_PREFIX + "daily:";
    
    /**
     * Log a search query and its results
     */
    public void logSearch(SearchRequestDto request, SearchResponseDto<ProductSearchDocument> response) {
        try {
            String query = request.getQuery();
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // Track popular queries
            if (query != null && !query.trim().isEmpty() && response.getMetadata().getTotalHits() > 0) {
                redisTemplate.opsForZSet().incrementScore(POPULAR_QUERIES_KEY, query, 1);
                
                // Keep only top 1000 popular queries
                redisTemplate.opsForZSet().removeRange(POPULAR_QUERIES_KEY, 0, -1001);
            }
            
            // Track failed queries (no results)
            if (response.getMetadata().getTotalHits() == 0) {
                redisTemplate.opsForZSet().incrementScore(FAILED_QUERIES_KEY, query, 1);
                
                // Keep only top 500 failed queries
                redisTemplate.opsForZSet().removeRange(FAILED_QUERIES_KEY, 0, -501);
            }
            
            // Track daily statistics
            String dailyKey = DAILY_STATS_PREFIX + today;
            redisTemplate.opsForHash().increment(dailyKey, "total_searches", 1);
            redisTemplate.opsForHash().increment(dailyKey, "total_results", response.getMetadata().getTotalHits());
            
            if (response.getMetadata().getTotalHits() == 0) {
                redisTemplate.opsForHash().increment(dailyKey, "zero_result_searches", 1);
            }
            
            // Set expiration for daily stats (keep for 90 days)
            redisTemplate.expire(dailyKey, 90, TimeUnit.DAYS);
            
            // Track search time performance
            redisTemplate.opsForHash().increment(dailyKey, "total_search_time", response.getMetadata().getSearchTime());
            
            log.debug("Logged search analytics for query: '{}', results: {}, time: {}ms", 
                query, response.getMetadata().getTotalHits(), response.getMetadata().getSearchTime());
                
        } catch (Exception e) {
            log.error("Failed to log search analytics", e);
        }
    }
    
    /**
     * Log user search behavior
     */
    public void logUserSearch(String userId, String query, List<String> clickedProductIds) {
        try {
            String userKey = USER_SEARCHES_PREFIX + userId;
            
            // Store recent searches for the user
            Map<String, Object> searchData = new HashMap<>();
            searchData.put("query", query);
            searchData.put("timestamp", LocalDateTime.now().toString());
            searchData.put("clicked_products", clickedProductIds);
            
            redisTemplate.opsForList().leftPush(userKey, searchData);
            
            // Keep only last 50 searches per user
            redisTemplate.opsForList().trim(userKey, 0, 49);
            
            // Set expiration for user data (keep for 30 days)
            redisTemplate.expire(userKey, 30, TimeUnit.DAYS);
            
        } catch (Exception e) {
            log.error("Failed to log user search behavior", e);
        }
    }
    
    /**
     * Get popular search queries
     */
    public List<String> getPopularQueries(int limit) {
        try {
            Set<Object> queries = redisTemplate.opsForZSet().reverseRange(POPULAR_QUERIES_KEY, 0, limit - 1);
            return queries != null ? new ArrayList<>(queries.stream().map(Object::toString).toList()) : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get popular queries", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get failed search queries that need attention
     */
    public List<String> getFailedQueries(int limit) {
        try {
            Set<Object> queries = redisTemplate.opsForZSet().reverseRange(FAILED_QUERIES_KEY, 0, limit - 1);
            return queries != null ? new ArrayList<>(queries.stream().map(Object::toString).toList()) : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get failed queries", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get search statistics for a specific date
     */
    public Map<String, Object> getDailyStats(String date) {
        try {
            String dailyKey = DAILY_STATS_PREFIX + date;
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(dailyKey);
            
            Map<String, Object> result = new HashMap<>();
            stats.forEach((k, v) -> result.put(k.toString(), v));
            
            // Calculate derived metrics
            Long totalSearches = (Long) result.getOrDefault("total_searches", 0L);
            Long zeroResultSearches = (Long) result.getOrDefault("zero_result_searches", 0L);
            Long totalSearchTime = (Long) result.getOrDefault("total_search_time", 0L);
            
            if (totalSearches > 0) {
                result.put("zero_result_rate", (double) zeroResultSearches / totalSearches);
                result.put("average_search_time", (double) totalSearchTime / totalSearches);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to get daily stats for date: {}", date, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get user's recent search history
     */
    public List<Map<String, Object>> getUserSearchHistory(String userId, int limit) {
        try {
            String userKey = USER_SEARCHES_PREFIX + userId;
            List<Object> searches = redisTemplate.opsForList().range(userKey, 0, limit - 1);
            
            if (searches == null) return new ArrayList<>();
            
            return searches.stream()
                .filter(search -> search instanceof Map)
                .map(search -> (Map<String, Object>) search)
                .toList();
                
        } catch (Exception e) {
            log.error("Failed to get user search history for user: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get search suggestions based on popular queries
     */
    public List<String> getSearchSuggestions(String prefix, int limit) {
        try {
            List<String> popularQueries = getPopularQueries(100);
            
            return popularQueries.stream()
                .filter(query -> query.toLowerCase().startsWith(prefix.toLowerCase()))
                .limit(limit)
                .toList();
                
        } catch (Exception e) {
            log.error("Failed to get search suggestions for prefix: {}", prefix, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Track when a user clicks on a search result
     */
    public void logSearchResultClick(String query, String productId, int position) {
        try {
            String clickKey = SEARCH_ANALYTICS_PREFIX + "clicks:" + query;
            
            Map<String, Object> clickData = new HashMap<>();
            clickData.put("product_id", productId);
            clickData.put("position", position);
            clickData.put("timestamp", LocalDateTime.now().toString());
            
            redisTemplate.opsForList().leftPush(clickKey, clickData);
            
            // Keep only last 100 clicks per query
            redisTemplate.opsForList().trim(clickKey, 0, 99);
            
            // Set expiration (keep for 30 days)
            redisTemplate.expire(clickKey, 30, TimeUnit.DAYS);
            
        } catch (Exception e) {
            log.error("Failed to log search result click", e);
        }
    }
    
    /**
     * Get aggregated search analytics dashboard data
     */
    public Map<String, Object> getSearchDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // Get today's stats
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            dashboard.put("today_stats", getDailyStats(today));
            
            // Get popular queries
            dashboard.put("popular_queries", getPopularQueries(10));
            
            // Get failed queries that need attention
            dashboard.put("failed_queries", getFailedQueries(10));
            
            // Get query count stats
            Long popularQueryCount = redisTemplate.opsForZSet().count(POPULAR_QUERIES_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            Long failedQueryCount = redisTemplate.opsForZSet().count(FAILED_QUERIES_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            
            dashboard.put("total_unique_queries", popularQueryCount);
            dashboard.put("total_failed_queries", failedQueryCount);
            
            return dashboard;
            
        } catch (Exception e) {
            log.error("Failed to get search dashboard data", e);
            return new HashMap<>();
        }
    }
}
