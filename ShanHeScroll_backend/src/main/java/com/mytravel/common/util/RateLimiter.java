package com.mytravel.common.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简易内存限流器，适用于单实例部署。
 * 支持「距上次请求最小间隔」和「每日最大次数」两个维度。
 */
@Component
public class RateLimiter {

    // ----- 最小间隔限流：key -> 上次请求的时间戳(ms) -----
    private final ConcurrentHashMap<String, AtomicLong> lastRequestTime = new ConcurrentHashMap<>();

    // ----- 每日次数限流：key -> DailyWindow -----
    private final ConcurrentHashMap<String, DailyWindow> dailyCounters = new ConcurrentHashMap<>();

    /**
     * 检查距上次同 key 请求是否已超过 minIntervalMs 毫秒。
     * 如果通过，记录本次时间戳并返回 true；否则返回 false。
     */
    public boolean tryAcquireInterval(String key, long minIntervalMs) {
        long now = System.currentTimeMillis();
        AtomicLong holder = lastRequestTime.computeIfAbsent(key, k -> new AtomicLong(0));
        long last = holder.get();
        if (now - last < minIntervalMs) {
            return false;
        }
        // CAS 更新，防止并发绕过
        return holder.compareAndSet(last, now);
    }

    /**
     * 检查 key 在 windowMs 窗口内的请求次数是否小于 maxCount。
     * 如果通过，计数 +1 并返回 true；否则返回 false。
     */
    public boolean tryAcquireDaily(String key, long windowMs, int maxCount) {
        long now = System.currentTimeMillis();
        DailyWindow window = dailyCounters.computeIfAbsent(key, k -> new DailyWindow(now));
        synchronized (window) {
            // 窗口过期则重置
            if (now - window.windowStart > windowMs) {
                window.windowStart = now;
                window.count.set(0);
            }
            if (window.count.get() >= maxCount) {
                return false;
            }
            window.count.incrementAndGet();
            return true;
        }
    }

    // ---- 清理（可定时调用，防止 Map 无限增长） ----

    /**
     * 清理过期条目，建议每 10 分钟调用一次。
     */
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        long oneDay = 24 * 60 * 60_000L;

        // 清理超过 1 天未更新的间隔记录
        lastRequestTime.entrySet().removeIf(e -> now - e.getValue().get() > oneDay);

        // 清理过期的每日窗口
        dailyCounters.entrySet().removeIf(e -> now - e.getValue().windowStart > oneDay);
    }

    /** 清除所有记录。 */
    public void clear() {
        lastRequestTime.clear();
        dailyCounters.clear();
    }

    // ---- 内部类 ----

    private static class DailyWindow {
        volatile long windowStart;
        final AtomicInteger count;

        DailyWindow(long start) {
            this.windowStart = start;
            this.count = new AtomicInteger(0);
        }
    }
}
