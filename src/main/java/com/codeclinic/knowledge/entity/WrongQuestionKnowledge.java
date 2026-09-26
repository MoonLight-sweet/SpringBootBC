package com.codeclinic.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wrong_question_knowledge")
public class WrongQuestionKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wrongQuestionId;
    private Long knowledgePointId;
}
