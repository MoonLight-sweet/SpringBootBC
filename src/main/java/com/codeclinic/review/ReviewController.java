package com.codeclinic.review;

import com.codeclinic.common.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(defaultValue = "false") boolean dueOnly) {
        return ApiResult.success(service.list(dueOnly));
    }

    @GetMapping("/page")
    public ApiResult<com.codeclinic.common.PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "false") boolean dueOnly,
            @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) {
        return ApiResult.success(service.page(dueOnly, page, size));
    }

    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summary() {
        return ApiResult.success(service.summary());
    }

    @GetMapping("/{questionId}/history")
    public ApiResult<List<Map<String, Object>>> history(@PathVariable long questionId, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) {
        return ApiResult.success(service.history(questionId, page, size));
    }

    @PostMapping("/{questionId}")
    public ApiResult<Map<String, Object>> record(@PathVariable long questionId, @Valid @RequestBody ReviewRequest request) {
        return ApiResult.success(service.record(questionId, request.remembered(), request.note()));
    }

    public record ReviewRequest(@NotNull Boolean remembered, @Size(max = 500) String note) {}
}
