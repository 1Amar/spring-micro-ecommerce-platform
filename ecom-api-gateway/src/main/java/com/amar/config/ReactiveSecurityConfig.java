package com.amar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class ReactiveSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public ReactiveSecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Disable CSRF for stateless API
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Configure authorization
            .authorizeExchange(exchanges -> exchanges
                // CORS preflight requests - Must be first
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public endpoints - No authentication required
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/actuator/health/**").permitAll()
                .pathMatchers("/actuator/prometheus/**").permitAll()
                .pathMatchers("/api/v1/public/**").permitAll()
                .pathMatchers("/api/v1/auth/**").permitAll()
                .pathMatchers("/swagger-ui/**").permitAll()
                .pathMatchers("/v3/api-docs/**").permitAll()
                .pathMatchers("/error").permitAll()
                .pathMatchers("/fallback").permitAll()
                
                // Admin-only endpoints - Requires admin role
                .pathMatchers("/api/v1/admin/**").hasRole("admin")
                
                // Manager endpoints - Requires manager or admin role
                .pathMatchers("/api/v1/manager/**").hasAnyRole("admin", "manager")
                
                // All microservice endpoints - Requires authentication (any valid user)
                .pathMatchers("/api/v1/order/**").authenticated()
                .pathMatchers("/api/v1/orders/**").authenticated()
                .pathMatchers("/api/v1/simulation/**").authenticated() // Requires authentication
                .pathMatchers("/api/v1/inventory/**").authenticated()
                .pathMatchers("/api/v1/products/**").authenticated()
                .pathMatchers("/api/v1/payments/**").authenticated()
                .pathMatchers("/api/v1/notifications/**").authenticated()
                .pathMatchers("/api/v1/catalog/**").authenticated()
                .pathMatchers("/api/v1/search/**").authenticated()
                
                // Catch-all - Requires authentication
                .anyExchange().authenticated()
            )
            
            // OAuth2 Resource Server for JWT authentication
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter authenticationConverter = new ReactiveJwtAuthenticationConverter();
        
        // Custom authorities converter for Keycloak realm roles
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract roles from realm_access.roles claim
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map) {
                Object roles = ((java.util.Map<?, ?>) realmAccess).get("roles");
                if (roles instanceof java.util.List) {
                    return Flux.fromIterable((java.util.List<?>) roles)
                        .cast(String.class)
                        .map(role -> (GrantedAuthority) () -> "ROLE_" + role.toUpperCase())
                        .cast(GrantedAuthority.class);
                }
            }
            return Flux.empty();
        });
        
        authenticationConverter.setPrincipalClaimName("preferred_username");
        return authenticationConverter;
    }
}