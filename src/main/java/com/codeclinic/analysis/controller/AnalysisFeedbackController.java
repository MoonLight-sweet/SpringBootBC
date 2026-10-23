package com.codeclinic.analysis.controller;

import com.codeclinic.analysis.service.AnalysisFeedbackService;
import com.codeclinic.common.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis/{analysisId}/feedback")
public class AnalysisFeedbackController {
    private final AnalysisFeedbackService service;

    public AnalysisFeedbackController(AnalysisFeedbackService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<Map<String, Object>> get(@PathVariable long analysisId) {
        return ApiResult.success(service.get(analysisId));
    }

    @PutMapping
    public ApiResult<Map<String, Object>> save(@PathVariable long analysisId, @Valid @RequestBody FeedbackRequest request) {
        return ApiResult.success(service.save(analysisId, request.verdict(), request.comment()));
    }

    public record FeedbackRequest(@NotBlank String verdict, @Size(max = 1000) String comment) {}
}
