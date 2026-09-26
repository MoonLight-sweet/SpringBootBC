package com.codeclinic.statistics.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.codeclinic.analysis.entity.AiAnalysis;
import com.codeclinic.analysis.mapper.AiAnalysisMapper;
import com.codeclinic.common.ApiResult;
import com.codeclinic.question.entity.WrongQuestion;
import com.codeclinic.question.mapper.WrongQuestionMapper;
import com.codeclinic.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final WrongQuestionMapper questionMapper;
    private final AiAnalysisMapper analysisMapper;

    public StatisticsController(WrongQuestionMapper questionMapper, AiAnalysisMapper analysisMapper) {
        this.questionMapper = questionMapper;
        this.analysisMapper = analysisMapper;
    }

    @GetMapping
    public ApiResult<Map<String, Object>> statistics() {
        Long userId = CurrentUser.id();
        long total = questionMapper.selectCount(Wrappers.<WrongQuestion>lambdaQuery().eq(WrongQuestion::getUserId, userId));
        long analyzed = questionMapper.selectCount(Wrappers.<WrongQuestion>lambdaQuery().eq(WrongQuestion::getUserId, userId).eq(WrongQuestion::getStatus, 2));
        long pending = questionMapper.selectCount(Wrappers.<WrongQuestion>lambdaQuery().eq(WrongQuestion::getUserId, userId).in(WrongQuestion::getStatus, 0, 3));
        long analyses = analysisMapper.selectCount(Wrappers.<AiAnalysis>lambdaQuery().inSql(AiAnalysis::getWrongQuestionId,
                "SELECT id FROM wrong_question WHERE user_id = " + userId));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalQuestions", total);
        data.put("analyzedQuestions", analyzed);
        data.put("pendingQuestions", pending);
        data.put("totalAnalyses", analyses);
        data.put("completionRate", total == 0 ? 0 : Math.round(analyzed * 10000.0 / total) / 100.0);
        return ApiResult.success(data);
    }
}
