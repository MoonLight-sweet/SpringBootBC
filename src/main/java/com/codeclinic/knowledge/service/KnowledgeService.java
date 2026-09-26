package com.codeclinic.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.codeclinic.knowledge.entity.KnowledgePoint;
import com.codeclinic.knowledge.entity.WrongQuestionKnowledge;
import com.codeclinic.knowledge.mapper.KnowledgePointMapper;
import com.codeclinic.knowledge.mapper.WrongQuestionKnowledgeMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeService {
    private final KnowledgePointMapper knowledgeMapper;
    private final WrongQuestionKnowledgeMapper relationMapper;

    public KnowledgeService(KnowledgePointMapper knowledgeMapper, WrongQuestionKnowledgeMapper relationMapper) {
        this.knowledgeMapper = knowledgeMapper;
        this.relationMapper = relationMapper;
    }

    public void bind(Long questionId, List<String> names) {
        relationMapper.delete(Wrappers.<WrongQuestionKnowledge>lambdaQuery()
                .eq(WrongQuestionKnowledge::getWrongQuestionId, questionId));
        names.stream().map(String::trim).filter(name -> !name.isBlank()).distinct().limit(6).forEach(name -> {
            KnowledgePoint point = knowledgeMapper.selectOne(Wrappers.<KnowledgePoint>lambdaQuery()
                    .eq(KnowledgePoint::getName, name));
            if (point == null) {
                point = new KnowledgePoint();
                point.setName(name);
                point.setDescription("由错题分析自动归纳的知识点");
                try {
                    knowledgeMapper.insert(point);
                } catch (DuplicateKeyException ignored) {
                    point = knowledgeMapper.selectOne(Wrappers.<KnowledgePoint>lambdaQuery()
                            .eq(KnowledgePoint::getName, name));
                }
            }
            WrongQuestionKnowledge relation = new WrongQuestionKnowledge();
            relation.setWrongQuestionId(questionId);
            relation.setKnowledgePointId(point.getId());
            relationMapper.insert(relation);
        });
    }

    public List<KnowledgePoint> list() {
        return knowledgeMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
                .apply("id IN (SELECT r.knowledge_point_id FROM wrong_question_knowledge r JOIN wrong_question q ON q.id = r.wrong_question_id WHERE q.user_id = {0})",
                        com.codeclinic.security.CurrentUser.id())
                .orderByAsc(KnowledgePoint::getName));
    }
}
