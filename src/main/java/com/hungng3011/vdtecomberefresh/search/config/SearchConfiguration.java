package com.hungng3011.vdtecomberefresh.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration for full-text search
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.hungng3011.vdtecomberefresh.search.repositories")
public class SearchConfiguration extends ElasticsearchConfiguration {
    
    @Value("${elasticsearch.host:localhost}")
    private String elasticsearchHost;
    
    @Value("${elasticsearch.port:9200}")
    private int elasticsearchPort;
    
    @Value("${elasticsearch.username:}")
    private String username;
    
    @Value("${elasticsearch.password:}")
    private String password;
    
    @Value("${elasticsearch.use-ssl:false}")
    private boolean useSsl;
    
    @Override
    public ClientConfiguration clientConfiguration() {
        var builder = ClientConfiguration.builder()
            .connectedTo(elasticsearchHost + ":" + elasticsearchPort)
            .withConnectTimeout(java.time.Duration.ofSeconds(30))
            .withSocketTimeout(java.time.Duration.ofSeconds(30));
        
        // Add authentication if credentials are provided
        if (!username.isEmpty() && !password.isEmpty()) {
            builder.withBasicAuth(username, password);
        }
        
        return builder.build();
    }
}
