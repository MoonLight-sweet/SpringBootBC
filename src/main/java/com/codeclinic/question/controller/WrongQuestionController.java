package com.codeclinic.question.controller;

import com.codeclinic.analysis.service.AnalysisService;
import com.codeclinic.analysis.vo.AnalysisResultVO;
import com.codeclinic.common.ApiResult;
import com.codeclinic.common.PageResult;
import com.codeclinic.question.dto.WrongQuestionRequest;
import com.codeclinic.question.service.WrongQuestionService;
import com.codeclinic.question.vo.WrongQuestionDetailVO;
import com.codeclinic.question.vo.WrongQuestionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wrong-question")
public class WrongQuestionController {
    private final WrongQuestionService questionService;
    private final AnalysisService analysisService;

    public WrongQuestionController(WrongQuestionService questionService, AnalysisService analysisService) {
        this.questionService = questionService;
        this.analysisService = analysisService;
    }

    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody WrongQuestionRequest request) {
        return ApiResult.success(questionService.create(request));
    }

    @GetMapping
    public ApiResult<PageResult<WrongQuestionVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String errorType,
            @RequestParam(required = false) Integer status) {
        return ApiResult.success(questionService.page(page, size, keyword, language, errorType, status));
    }

    @GetMapping("/{id}")
    public ApiResult<WrongQuestionDetailVO> detail(@PathVariable Long id) {
        return ApiResult.success(questionService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody WrongQuestionRequest request) {
        questionService.update(id, request);
        return ApiResult.successMessage("错题已更新");
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResult.successMessage("错题已删除");
    }

    @PostMapping("/{id}/analyze")
    public ApiResult<AnalysisResultVO> analyze(@PathVariable Long id) {
        return ApiResult.success(analysisService.analyze(id));
    }

    @PostMapping("/{id}/reanalyze")
    public ApiResult<AnalysisResultVO> reanalyze(@PathVariable Long id) {
        return ApiResult.success(analysisService.analyze(id));
    }
}
