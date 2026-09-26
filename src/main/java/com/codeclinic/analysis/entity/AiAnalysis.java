package com.codeclinic.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_analysis")
public class AiAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wrongQuestionId;
    private String errorType;
    private String errorLocation;
    private String errorReason;
    private String suggestion;
    private String correctCode;
    private String knowledgePoints;
    private String learningAdvice;
    private String modelName;
    private Integer status;
    private LocalDateTime createTime;
}
