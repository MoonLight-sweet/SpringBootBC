package com.codeclinic.analysis.service;

import com.codeclinic.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AiResultParserTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AiResultParser parser = new AiResultParser(json);
    private String valid(String code, Object points) throws Exception {
        return json.writeValueAsString(Map.of("errorType","运行时错误","errorLocation","line 1",
                "errorReason","原因","suggestion","建议","correctCode",code,
                "knowledgePoints",points,"learningAdvice","建议"));
    }
    @Test void acceptsChineseBoundaryAndFencedJson() throws Exception {
        assertEquals(30000, parser.parse("```json\n"+valid("中".repeat(30000), java.util.List.of("数组"))+"\n```").correctCode().length());
    }
    @Test void rejectsOverlongOutputAndEmptyKnowledge() throws Exception {
        String over = valid("中".repeat(30001),java.util.List.of("数组"));
        assertThrows(BusinessException.class,()->parser.parse(over));
        String empty = valid("code",java.util.List.of());
        assertThrows(BusinessException.class,()->parser.parse(empty));
    }
    @Test void rejectsMissingFieldsMalformedAndNonObject() {
        for(String raw:new String[]{"", "[]", "{", "{\"errorType\":\"x\"}"})
            assertThrows(BusinessException.class,()->parser.parse(raw));
    }
}
