package com.codeclinic.question.vo;

import com.codeclinic.question.entity.WrongQuestion;

import java.time.LocalDateTime;

public record WrongQuestionVO(Long id, String title, String description, String language, String code,
                              String errorMessage, String actualOutput, String expectedOutput, String userNote,
                              Integer status, LocalDateTime createTime, LocalDateTime updateTime) {
    public static WrongQuestionVO from(WrongQuestion question) {
        return new WrongQuestionVO(question.getId(), question.getTitle(), question.getDescription(),
                question.getLanguage(), question.getCode(), question.getErrorMessage(), question.getActualOutput(),
                question.getExpectedOutput(), question.getUserNote(), question.getStatus(),
                question.getCreateTime(), question.getUpdateTime());
    }
}
