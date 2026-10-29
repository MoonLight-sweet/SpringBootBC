package com.codeclinic.question.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.codeclinic.analysis.service.AnalysisService;
import com.codeclinic.common.BusinessException;
import com.codeclinic.question.dto.WrongQuestionRequest;
import com.codeclinic.question.entity.WrongQuestion;
import com.codeclinic.question.mapper.WrongQuestionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WrongQuestionServiceTest {
    private final WrongQuestionMapper mapper = mock(WrongQuestionMapper.class);
    private final WrongQuestionService service = new WrongQuestionService(mapper, mock(AnalysisService.class));

    @BeforeEach
    void login() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), WrongQuestion.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(7L, null));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void detailRemainsReadableDuringAnalysis() {
        WrongQuestion question = question(1);
        when(mapper.selectOne(any())).thenReturn(question);
        assertEquals(42L, service.detail(42L).question().id());
    }

    @Test
    void updateRejectsWhenConditionalWriteLosesRace() {
        when(mapper.selectOne(any())).thenReturn(question(0));
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(0);
        var request = new WrongQuestionRequest("题目", null, "Java", "code", null, null, null, null);
        BusinessException error = assertThrows(BusinessException.class, () -> service.update(42L, request));
        assertEquals(409, error.getCode());
    }

    @Test
    void deleteRejectsWhenConditionalDeleteLosesRace() {
        when(mapper.selectOne(any())).thenReturn(question(0));
        when(mapper.delete(any(Wrapper.class))).thenReturn(0);
        BusinessException error = assertThrows(BusinessException.class, () -> service.delete(42L));
        assertEquals(409, error.getCode());
    }

    private WrongQuestion question(int status) {
        WrongQuestion question = new WrongQuestion();
        question.setId(42L);
        question.setUserId(7L);
        question.setStatus(status);
        return question;
    }
}
