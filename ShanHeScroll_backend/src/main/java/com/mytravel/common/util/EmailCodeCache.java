package com.mytravel.common.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmailCodeCache {

    private final ConcurrentHashMap<String, CodeEntry> cache = new ConcurrentHashMap<>();

    public void put(String email, String code, long expireMillis) {
        cache.put(email, new CodeEntry(code, System.currentTimeMillis() + expireMillis));
    }

    public boolean validate(String email, String code) {
        CodeEntry entry = cache.get(email);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expireAt) {
            cache.remove(email);
            return false;
        }
        return entry.code.equals(code);
    }

    public void remove(String email) {
        cache.remove(email);
    }

    /**
     * 清理已过期的验证码条目，防止内存泄漏。
     */
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> now > e.getValue().expireAt);
    }

    private static class CodeEntry {
        String code;
        long expireAt;

        CodeEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
