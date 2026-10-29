package com.codeclinic.analysis.vo;

import java.time.LocalDateTime;

public record AnalysisFailureVO(Long id, LocalDateTime createTime, String reason) {}
