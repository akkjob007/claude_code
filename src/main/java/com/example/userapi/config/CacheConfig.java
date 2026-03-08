package com.example.userapi.config;

import com.example.userapi.cache.LruCache;
import com.example.userapi.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public LruCache<Long, UserResponse> userCache(@Value("${app.cache.capacity:100}") int capacity) {
        return new LruCache<>(capacity);
    }
}
