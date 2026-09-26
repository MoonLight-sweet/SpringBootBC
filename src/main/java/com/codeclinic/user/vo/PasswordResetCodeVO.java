package com.codeclinic.user.vo;

public record PasswordResetCodeVO(String maskedEmail, long expiresInSeconds) {
}
