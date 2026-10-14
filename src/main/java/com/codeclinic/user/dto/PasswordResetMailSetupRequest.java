package com.codeclinic.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetMailSetupRequest(
        @NotBlank(message = "发件邮箱不能为空")
        @Email(message = "发件邮箱格式不正确")
        String email,
        @NotBlank(message = "邮箱授权码不能为空")
        @Size(max = 128, message = "邮箱授权码长度不能超过128个字符")
        String authorizationCode
) {
}
