package com.mytravel.common;

import com.mytravel.common.util.EmailCodeCache;
import com.mytravel.common.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时清理内存缓存中的过期条目，防止长期运行内存泄漏。
 */
@Component
@RequiredArgsConstructor
public class CacheCleanupScheduler {

    private final EmailCodeCache emailCodeCache;
    private final RateLimiter rateLimiter;

    /** 每 10 分钟清理一次过期记录 */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void cleanCaches() {
        emailCodeCache.cleanExpired();
        rateLimiter.cleanExpired();
    }
}
