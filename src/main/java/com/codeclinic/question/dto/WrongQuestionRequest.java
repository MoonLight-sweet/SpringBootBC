package com.codeclinic.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WrongQuestionRequest(
        @NotBlank(message = "题目标题不能为空") @Size(max = 200, message = "题目标题不能超过200个字符") String title,
        @Size(max = 5000, message = "题目描述不能超过5000个字符") String description,
        @NotBlank(message = "编程语言不能为空") @Size(max = 50, message = "编程语言不能超过50个字符") String language,
        @NotBlank(message = "代码不能为空") @Size(max = 30000, message = "代码不能超过30000个字符") String code,
        @Size(max = 10000, message = "错误信息不能超过10000个字符") String errorMessage,
        @Size(max = 10000, message = "实际输出不能超过10000个字符") String actualOutput,
        @Size(max = 10000, message = "期望输出不能超过10000个字符") String expectedOutput,
        @Size(max = 3000, message = "备注不能超过3000个字符") String userNote) {
}
