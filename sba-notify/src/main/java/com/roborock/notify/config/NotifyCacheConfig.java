package com.roborock.notify.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class NotifyCacheConfig implements CachingConfigurer {
    @Override
    @Bean
    public CacheManager cacheManager() {
        return new CompositeCacheManager(simpleCacheManager());
    }

    @Bean
    SimpleCacheManager simpleCacheManager() {
        var simpleCacheManager = new SimpleCacheManager();

        var activityBanner = new CaffeineCache(LocalCacheName.PRICE_CHANGE, Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .recordStats()
                .build());

        simpleCacheManager.setCaches(List.of(activityBanner));
        return simpleCacheManager;
    }
}
