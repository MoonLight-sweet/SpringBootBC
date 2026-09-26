package com.codeclinic.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_question")
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String language;
    private String code;
    private String errorMessage;
    private String actualOutput;
    private String expectedOutput;
    private String userNote;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
