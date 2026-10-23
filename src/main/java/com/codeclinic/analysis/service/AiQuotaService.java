package com.codeclinic.analysis.service;

import com.codeclinic.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Component
public class AiQuotaService {
    private final Semaphore global;
    private final int dailyLimit;
    private final Map<Long, Semaphore> perUser = new HashMap<>();
    private final Map<Long, Usage> usage = new HashMap<>();

    public AiQuotaService(@Value("${ai.daily-limit:20}") int dailyLimit,
                          @Value("${ai.global-concurrency:3}") int globalConcurrency) {
        if (dailyLimit < 1 || globalConcurrency < 1) throw new IllegalArgumentException("AI调用额度配置必须大于0");
        this.dailyLimit = dailyLimit;
        this.global = new Semaphore(globalConcurrency);
    }

    public Permit acquire(long userId) {
        Semaphore user;
        synchronized (this) {
            user = perUser.computeIfAbsent(userId, ignored -> new Semaphore(1));
        }
        if (!user.tryAcquire()) throw new BusinessException(429, "你的上一项分析仍在运行");
        if (!global.tryAcquire()) {
            user.release();
            throw new BusinessException(429, "AI服务繁忙，请稍后重试");
        }
        synchronized (this) {
            LocalDate today = LocalDate.now();
            Usage current = usage.get(userId);
            int count = current != null && current.date.equals(today) ? current.count : 0;
            if (count >= dailyLimit) {
                global.release();
                user.release();
                throw new BusinessException(429, "今日AI分析次数已用完，请明天再试");
            }
            usage.put(userId, new Usage(today, count + 1));
            if (usage.size() > 10000) usage.entrySet().removeIf(entry -> entry.getValue().date.isBefore(today));
        }
        return new Permit(user);
    }

    public final class Permit implements AutoCloseable {
        private final Semaphore user;
        private boolean closed;

        private Permit(Semaphore user) { this.user = user; }

        @Override
        public synchronized void close() {
            if (!closed) {
                closed = true;
                global.release();
                user.release();
            }
        }
    }

    private record Usage(LocalDate date, int count) {}
}
