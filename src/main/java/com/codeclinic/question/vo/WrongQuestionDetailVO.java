package com.codeclinic.question.vo;

import com.codeclinic.analysis.vo.AnalysisResultVO;

public record WrongQuestionDetailVO(WrongQuestionVO question, AnalysisResultVO analysis) {
}
