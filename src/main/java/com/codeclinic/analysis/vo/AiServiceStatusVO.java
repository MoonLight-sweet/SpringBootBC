package com.codeclinic.analysis.vo;

public record AiServiceStatusVO(
        boolean available,
        String model,
        String message) {
}
