package com.codeclinic.analysis.service;

import com.codeclinic.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class AiResultParser {
    private final ObjectMapper objectMapper;

    public AiResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedAnalysis parse(String raw) {
        try {
            String json = stripCodeFence(raw);
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) throw new BusinessException("AI分析结果必须是JSON对象");
            List<String> parsedPoints = new ArrayList<>();
            JsonNode pointNode = root.get("knowledgePoints");
            if (pointNode != null && pointNode.isArray() && pointNode.size() >= 1 && pointNode.size() <= 6) {
                pointNode.forEach(node -> {
                    if (!node.isTextual() || node.asText().isBlank() || node.asText().length() > 100)
                        throw new BusinessException("AI知识点必须是1到100个字符的文本");
                    String value = node.asText().trim();
                    if (!value.isBlank()) parsedPoints.add(value);
                });
            } else throw new BusinessException("AI知识点必须包含1到6项");
            List<String> points = new ArrayList<>(new LinkedHashSet<>(parsedPoints));
            ParsedAnalysis result = new ParsedAnalysis(required(root, "errorType"), required(root, "errorLocation"),
                    required(root, "errorReason"), required(root, "suggestion"), required(root, "correctCode"),
                    points, required(root, "learningAdvice"));
            if (result.knowledgePoints().isEmpty()) {
                throw new BusinessException("AI分析结果缺少知识点");
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI分析结果格式错误");
        }
    }

    private String required(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new BusinessException("AI分析结果缺少字段：" + field);
        }
        int max = field.equals("errorType") ? 100 : 30000;
        if (value.asText().length() > max) throw new BusinessException("AI分析字段过长：" + field);
        return value.asText().trim();
    }

    private String stripCodeFence(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("```")) {
            int firstLine = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLine > 0 && lastFence > firstLine) value = value.substring(firstLine + 1, lastFence).trim();
        }
        return value;
    }

    public record ParsedAnalysis(String errorType, String errorLocation, String errorReason, String suggestion,
                                 String correctCode, List<String> knowledgePoints, String learningAdvice) {
    }
}
