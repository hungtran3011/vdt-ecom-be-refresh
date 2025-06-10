package com.hungng3011.vdtecomberefresh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder; // New import
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.security.KeyPair; // New import
import java.security.KeyPairGenerator; // New import
import java.security.interfaces.RSAPublicKey; // New import
import java.util.Collection; // New import
import java.util.Map; // New import
import org.springframework.security.core.authority.AuthorityUtils; // New import

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Bean
    @Profile("!test")
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints
                        .requestMatchers("/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        
                        // Viettel payment webhook endpoints (external callbacks)
                        .requestMatchers("/api/viettel/partner/order-confirmation").permitAll()
                        .requestMatchers("/api/viettel/partner/ipn").permitAll()
                        .requestMatchers("/api/viettel/partner/redirect").permitAll()
                        
                        // Public read access
                        .requestMatchers(HttpMethod.GET, "/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/media/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/address/**").permitAll()

                        // Authenticated endpoints
                        .requestMatchers("/v1/carts/**").authenticated()
                        .requestMatchers("/v1/orders/**").authenticated()
                        .requestMatchers("/v1/profiles/**").authenticated()

                        // Role-based access
                        .requestMatchers(HttpMethod.POST, "/v1/products/**").hasAnyRole("seller", "admin")
                        .requestMatchers(HttpMethod.PUT, "/v1/products/**").hasAnyRole("seller", "admin")
                        .requestMatchers(HttpMethod.DELETE, "/v1/products/**").hasAnyRole("seller", "admin")
                        .requestMatchers("/v1/stats/**").hasRole("admin")
                        .requestMatchers(HttpMethod.GET, "/v1/profiles").hasRole("admin")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        return http.build();
    }

    @Bean
    @Profile("test")
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v1/profiles/**").authenticated()
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.getWriter().write("Unauthorized");
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(testJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );  
        return http.build();
    }

    @Bean
    @Profile("!test")
    public JwtDecoder jwtDecoder() {
        // Create a JWT decoder that fetches keys from the JWK Set URI
        // This allows us to accept JWTs with localhost issuer while fetching keys from Docker network
        if (jwkSetUri != null && !jwkSetUri.isEmpty()) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else {
            return JwtDecoders.fromIssuerLocation(issuerUri);
        }
    }

    @Bean
    @Profile("test")
    public JwtDecoder testJwtDecoder() {
        // For testing purposes, a NimbusJwtDecoder with a generated RSA key pair is used.
        // The public key is used for decoding. If your tests involve generating signed JWTs,
        // you would use the corresponding private key (keyPair.getPrivate()) to sign them.
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048); // Using 2048-bit key size
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception ex) {
            // Handle potential exceptions during key generation or decoder setup
            throw new RuntimeException("Failed to initialize test JwtDecoder", ex);
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object realmAccess = jwt.getClaims().getOrDefault("realm_access", Map.of());
            Object rolesObj = null;
            if (realmAccess instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
                rolesObj = realmAccessMap.getOrDefault("roles", List.of());
            }
            
            Collection<String> roles;
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) rolesObj;
                roles = rolesList.stream()
                       .map(r -> "ROLE_" + r)
                       .toList();
            } else {
                roles = List.of();
            }
            return AuthorityUtils.createAuthorityList(roles.toArray(new String[0]));
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
