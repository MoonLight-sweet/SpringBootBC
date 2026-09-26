package com.codeclinic.analysis.controller;

import com.codeclinic.analysis.service.AnalysisService;
import com.codeclinic.analysis.vo.AnalysisResultVO;
import com.codeclinic.common.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/{questionId}")
    public ApiResult<AnalysisResultVO> latest(@PathVariable Long questionId) {
        return ApiResult.success(analysisService.latest(questionId));
    }

    @GetMapping("/{questionId}/history")
    public ApiResult<List<AnalysisResultVO>> history(@PathVariable Long questionId) {
        return ApiResult.success(analysisService.history(questionId));
    }
}
