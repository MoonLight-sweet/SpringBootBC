package com.codeclinic.user.service;

import com.codeclinic.common.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class PasswordResetRequestLimiter {
    private final Map<String, Window> windows = new HashMap<>();

    public synchronized void acquire(String email, String address) {
        long now = Instant.now().getEpochSecond();
        if (windows.size() > 10000) windows.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        if (windows.size() > 20000) throw new BusinessException(429, "请求过于频繁，请稍后重试");
        String emailKey = "email:" + email;
        String addressKey = "address:" + address;
        if (count(emailKey, now) >= 3 || count(addressKey, now) >= 10) {
            throw new BusinessException(429, "请求过于频繁，请稍后重试");
        }
        increment(emailKey, now, 3600);
        increment(addressKey, now, 900);
    }

    private int count(String key, long now) {
        Window old = windows.get(key);
        return old == null || old.expiresAt <= now ? 0 : old.count;
    }

    private void increment(String key, long now, long seconds) {
        Window old = windows.get(key);
        if (old == null || old.expiresAt <= now) {
            windows.put(key, new Window(1, now + seconds));
        } else {
            windows.put(key, new Window(old.count + 1, old.expiresAt));
        }
    }

    private record Window(int count, long expiresAt) {}
}
