package com.codeclinic.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 30, message = "用户名长度应为3到30个字符") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 64, message = "密码长度应为6到64个字符") String password,
        @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") @Size(max = 120, message = "邮箱不能超过120个字符") String email,
        @Size(max = 80, message = "昵称不能超过80个字符") String displayName) {
}
