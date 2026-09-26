package com.codeclinic.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "\\d{6}", message = "验证码应为6位数字")
        String code,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "新密码长度应为6到64个字符")
        String newPassword) {
}
