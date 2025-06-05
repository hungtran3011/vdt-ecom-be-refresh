package com.hungng3011.vdtecomberefresh.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Admin Client Configuration for user management operations
 */
@Configuration
public class KeycloakConfig {

    @Value("${app.keycloak.server-url}")
    private String serverUrl;

    @Value("${app.keycloak.realm}")
    private String realm;

    @Value("${app.keycloak.client-id}")
    private String clientId;

    @Value("${app.keycloak.client-secret}")
    private String clientSecret;

    @Value("${app.keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${app.keycloak.admin.password:admin}")
    private String adminPassword;

    /**
     * Creates Keycloak admin client bean for user management operations
     */
    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master") // Use master realm for admin operations
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    /**
     * Gets the target realm for user operations
     */
    public String getTargetRealm() {
        return realm;
    }
}
