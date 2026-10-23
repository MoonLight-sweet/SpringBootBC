package com.codeclinic.user.service;

import com.codeclinic.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {
    @Test
    void locksAccountAfterFiveFailuresAndSuccessClearsAccountWindow() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.check("student", "127.0.0.1");
            limiter.failed("student", "127.0.0.1");
        }
        assertEquals(429, assertThrows(BusinessException.class,
                () -> limiter.check("student", "127.0.0.1")).getCode());
        limiter.succeeded("student");
        assertDoesNotThrow(() -> limiter.check("student", "127.0.0.1"));
    }
}
