package com.tiki.product.config;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisCacheConfig implements CachingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);
    private final RedisConnectionFactory connectionFactory;
    private final Duration ttl;
    public RedisCacheConfig(RedisConnectionFactory connectionFactory,
            @Value("${product.cache-ttl:60s}") Duration ttl) {
        this.connectionFactory = connectionFactory;
        this.ttl = ttl;
    }

    @Bean @Override public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl).disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
    }

    @Bean @Override public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            public void handleCacheGetError(RuntimeException e, Cache c, Object key) {
                log.warn("Redis GET lỗi key={}; fallback DB: {}", key, e.getMessage());
            }
            public void handleCachePutError(RuntimeException e, Cache c, Object key, Object value) {
                log.warn("Redis PUT lỗi key={}; response DB vẫn hợp lệ: {}", key, e.getMessage());
            }
            public void handleCacheEvictError(RuntimeException e, Cache c, Object key) {
                log.error("Redis EVICT lỗi key={}; TTL/retry event sẽ sửa cache: {}", key, e.getMessage());
            }
            public void handleCacheClearError(RuntimeException e, Cache c) {
                log.error("Redis CLEAR lỗi: {}", e.getMessage());
            }
        };
    }
}
