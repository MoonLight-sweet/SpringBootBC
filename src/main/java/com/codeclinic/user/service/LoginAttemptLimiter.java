package com.codeclinic.user.service;

import com.codeclinic.common.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginAttemptLimiter {
    private static final long WINDOW_SECONDS = 15 * 60;
    private final Map<String, Window> windows = new HashMap<>();

    public synchronized void check(String account, String address) {
        long now = Instant.now().getEpochSecond();
        cleanup(now);
        if (count("account:" + account, now) >= 5 || count("address:" + address, now) >= 30) {
            throw new BusinessException(429, "登录尝试过于频繁，请15分钟后重试");
        }
    }

    public synchronized void failed(String account, String address) {
        long now = Instant.now().getEpochSecond();
        add("account:" + account, now);
        add("address:" + address, now);
    }

    public synchronized void succeeded(String account) {
        windows.remove("account:" + account);
    }

    private int count(String key, long now) {
        Window window = windows.get(key);
        return window == null || window.expiresAt <= now ? 0 : window.count;
    }

    private void add(String key, long now) {
        Window old = windows.get(key);
        windows.put(key, old == null || old.expiresAt <= now
                ? new Window(1, now + WINDOW_SECONDS) : new Window(old.count + 1, old.expiresAt));
    }

    private void cleanup(long now) {
        if (windows.size() > 10000) windows.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        if (windows.size() > 20000) throw new BusinessException(429, "登录尝试过于频繁，请稍后重试");
    }

    private record Window(int count, long expiresAt) {}
}
