package com.codeclinic.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    @Test
    void httpStatusMatchesBusinessCode() {
        var response = new GlobalExceptionHandler().handleBusiness(new BusinessException(429, "too many"));
        assertEquals(429, response.getStatusCode().value());
        assertEquals(429, response.getBody().code());
    }
}
