package com.codeclinic.analysis.service;

import com.codeclinic.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiQuotaServiceTest {
    @Test
    void enforcesConcurrencyAndDailyAttemptBudget() {
        AiQuotaService quota = new AiQuotaService(2, 1);
        try (AiQuotaService.Permit ignored = quota.acquire(1L)) {
            assertEquals(429, assertThrows(BusinessException.class, () -> quota.acquire(1L)).getCode());
            assertEquals(429, assertThrows(BusinessException.class, () -> quota.acquire(2L)).getCode());
        }
        try (AiQuotaService.Permit ignored = quota.acquire(1L)) {
            assertNotNull(ignored);
        }
        assertEquals(429, assertThrows(BusinessException.class, () -> quota.acquire(1L)).getCode());
        try (AiQuotaService.Permit ignored = quota.acquire(2L)) {
            assertNotNull(ignored);
        }
    }
}
