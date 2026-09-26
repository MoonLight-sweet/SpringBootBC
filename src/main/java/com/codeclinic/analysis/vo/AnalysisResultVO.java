package com.codeclinic.analysis.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AnalysisResultVO(Long id, String errorType, String errorLocation, String errorReason,
                               String suggestion, String correctCode, List<String> knowledgePoints,
                               String learningAdvice, String modelName, LocalDateTime createTime) {
}
