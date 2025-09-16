package com.amar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Configuration
public class RateLimiterConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(auth -> {
                    if (auth != null && auth.getPrincipal() instanceof Jwt) {
                        String userId = ((Jwt) auth.getPrincipal()).getSubject();
                        logger.debug("Rate limiting key: user:{}", userId);
                        return Mono.just("user:" + userId);
                    }
                    if (auth != null && auth.getPrincipal() instanceof Principal) {
                        String name = ((Principal) auth.getPrincipal()).getName();
                        logger.debug("Rate limiting key: principal:{}", name);
                        return Mono.just("principal:" + name);
                    }
                    // Fallback to IP address for anonymous users
                    if (exchange.getRequest().getRemoteAddress() != null) {
                        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                        logger.debug("Rate limiting key: ip:{}", ip);
                        return Mono.just("ip:" + ip);
                    }
                    // As a last resort, use a generic key for unidentified requests
                    logger.debug("Rate limiting key: anonymous");
                    return Mono.just("anonymous");
                })
                .defaultIfEmpty("unknown");
    }
    
    /**
     * Custom Redis Rate Limiter configuration with monitoring
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }
}
