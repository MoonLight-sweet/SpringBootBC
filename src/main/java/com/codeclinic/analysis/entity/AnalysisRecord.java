package com.codeclinic.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_record")
public class AnalysisRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wrongQuestionId;
    private String modelName;
    private String prompt;
    private String response;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createTime;
}
