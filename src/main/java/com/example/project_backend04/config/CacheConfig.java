package com.example.project_backend04.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1)) // Default TTL of 1 hour
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        // Configure specific cache settings
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Trainer availability cache - short TTL for real-time updates
        cacheConfigurations.put("trainer_availability", 
            defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Trainer conflicts cache - medium TTL
        cacheConfigurations.put("trainer_conflicts", 
            defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Trainer specialties cache - longer TTL as it changes less frequently
        cacheConfigurations.put("trainer_specialties", 
            defaultConfig.entryTtl(Duration.ofHours(6)));
        
        // Service registration cache - medium TTL
        cacheConfigurations.put("service_registrations", 
            defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Booking statistics cache - longer TTL
        cacheConfigurations.put("booking_statistics", 
            defaultConfig.entryTtl(Duration.ofHours(4)));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}