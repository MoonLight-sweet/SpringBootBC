package com.codeclinic.knowledge.controller;

import com.codeclinic.common.ApiResult;
import com.codeclinic.knowledge.entity.KnowledgePoint;
import com.codeclinic.knowledge.service.KnowledgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-point")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public ApiResult<List<KnowledgePoint>> list() {
        return ApiResult.success(knowledgeService.list());
    }
}
