package com.codeclinic.review;

import com.codeclinic.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {
    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void refusesReviewOfAnotherUsersQuestion() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(7L, null));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(42L), eq(7L))).thenReturn(0L);
        BusinessException error = assertThrows(BusinessException.class,
                () -> new ReviewService(jdbc).record(42L, true, null));
        assertEquals(404, error.getCode());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void refusesEarlyReviewEvenWhenCalledDirectly() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(7L, null));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(42L), eq(7L))).thenReturn(1L);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(42L), eq(7L)))
                .thenReturn(2, 0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ReviewService(jdbc).record(42L, true, null));

        assertEquals(409, error.getCode());
        verify(jdbc, never()).update(startsWith("UPDATE question_review"), any(Object[].class));
        verify(jdbc, never()).update(startsWith("INSERT INTO review_record"), any(Object[].class));
    }
}
