package com.codeclinic.analysis.controller;

import com.codeclinic.analysis.service.AnalysisService;
import com.codeclinic.analysis.vo.AnalysisResultVO;
import com.codeclinic.analysis.vo.AiServiceStatusVO;
import com.codeclinic.common.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;
    private final com.codeclinic.analysis.service.DeepSeekClient deepSeekClient;

    public AnalysisController(AnalysisService analysisService,
                              com.codeclinic.analysis.service.DeepSeekClient deepSeekClient) {
        this.analysisService = analysisService;
        this.deepSeekClient = deepSeekClient;
    }

    @GetMapping("/status")
    public ApiResult<AiServiceStatusVO> status() {
        return ApiResult.success(deepSeekClient.status());
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
