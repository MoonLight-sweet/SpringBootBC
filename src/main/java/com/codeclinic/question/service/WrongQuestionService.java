package com.codeclinic.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeclinic.analysis.service.AnalysisService;
import com.codeclinic.analysis.entity.AiAnalysis;
import com.codeclinic.analysis.mapper.AiAnalysisMapper;
import com.codeclinic.knowledge.entity.WrongQuestionKnowledge;
import com.codeclinic.knowledge.mapper.WrongQuestionKnowledgeMapper;
import com.codeclinic.common.BusinessException;
import com.codeclinic.common.PageResult;
import com.codeclinic.question.dto.WrongQuestionRequest;
import com.codeclinic.question.entity.WrongQuestion;
import com.codeclinic.question.mapper.WrongQuestionMapper;
import com.codeclinic.question.vo.WrongQuestionDetailVO;
import com.codeclinic.question.vo.WrongQuestionVO;
import com.codeclinic.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class WrongQuestionService {
    private final WrongQuestionMapper questionMapper;
    private final AnalysisService analysisService;
    private final AiAnalysisMapper analysisMapper;
    private final WrongQuestionKnowledgeMapper knowledgeMapper;

    public WrongQuestionService(WrongQuestionMapper questionMapper, AnalysisService analysisService,
                                AiAnalysisMapper analysisMapper, WrongQuestionKnowledgeMapper knowledgeMapper) {
        this.questionMapper = questionMapper;
        this.analysisService = analysisService;
        this.analysisMapper = analysisMapper;
        this.knowledgeMapper = knowledgeMapper;
    }

    public Long create(WrongQuestionRequest request) {
        WrongQuestion question = new WrongQuestion();
        apply(question, request);
        question.setUserId(CurrentUser.id());
        question.setStatus(0);
        questionMapper.insert(question);
        return question.getId();
    }

    public PageResult<WrongQuestionVO> page(long current, long size, String keyword, String language,
                                             String errorType, Integer status) {
        if (current < 1 || size < 1 || size > 100) throw new BusinessException("分页参数不正确");
        LambdaQueryWrapper<WrongQuestion> query = Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, CurrentUser.id())
                .eq(StringUtils.hasText(language), WrongQuestion::getLanguage, language)
                .eq(status != null, WrongQuestion::getStatus, status)
                .and(StringUtils.hasText(keyword), q -> q.like(WrongQuestion::getTitle, keyword)
                        .or().like(WrongQuestion::getDescription, keyword)
                        .or().like(WrongQuestion::getCode, keyword))
                .orderByDesc(WrongQuestion::getUpdateTime).orderByDesc(WrongQuestion::getId);
        if (StringUtils.hasText(errorType)) {
            query.ne(WrongQuestion::getStatus, 0).apply("id IN (SELECT a.wrong_question_id FROM ai_analysis a WHERE a.status = 1 AND a.error_type = {0} AND a.id = (SELECT MAX(b.id) FROM ai_analysis b WHERE b.wrong_question_id = a.wrong_question_id AND b.status = 1))", errorType);
        }
        IPage<WrongQuestion> result = questionMapper.selectPage(new Page<>(current, size), query);
        return new PageResult<>(result.getRecords().stream().map(WrongQuestionVO::from).toList(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    public WrongQuestionDetailVO detail(Long id) {
        WrongQuestion question = owned(id);
        return new WrongQuestionDetailVO(WrongQuestionVO.from(question), question.getStatus() == 0 ? null : analysisService.latestOrNull(id));
    }

    @Transactional
    public void update(Long id, WrongQuestionRequest request) {
        WrongQuestion question = questionMapper.selectOne(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getId, id).eq(WrongQuestion::getUserId, CurrentUser.id()).last("FOR UPDATE"));
        if (question == null) throw new BusinessException(404, "错题不存在");
        boolean changed = !Objects.equals(question.getTitle(), request.title().trim())
                || !Objects.equals(question.getDescription(), request.description())
                || !Objects.equals(question.getLanguage(), request.language().trim())
                || !Objects.equals(question.getCode(), request.code())
                || !Objects.equals(question.getErrorMessage(), request.errorMessage())
                || !Objects.equals(question.getActualOutput(), request.actualOutput())
                || !Objects.equals(question.getExpectedOutput(), request.expectedOutput());
        apply(question, request);
        int updated = questionMapper.update(null, Wrappers.<WrongQuestion>lambdaUpdate()
                .eq(WrongQuestion::getId, id).eq(WrongQuestion::getUserId, CurrentUser.id())
                .ne(WrongQuestion::getStatus, 1)
                .set(WrongQuestion::getTitle, question.getTitle())
                .set(WrongQuestion::getDescription, question.getDescription())
                .set(WrongQuestion::getLanguage, question.getLanguage())
                .set(WrongQuestion::getCode, question.getCode())
                .set(WrongQuestion::getErrorMessage, question.getErrorMessage())
                .set(WrongQuestion::getActualOutput, question.getActualOutput())
                .set(WrongQuestion::getExpectedOutput, question.getExpectedOutput())
                .set(WrongQuestion::getUserNote, question.getUserNote())
                .set(changed, WrongQuestion::getStatus, 0).set(WrongQuestion::getUpdateTime, LocalDateTime.now()));
        if (updated != 1) throw new BusinessException(409, "错题正在分析或已被删除，请刷新后重试");
        if (changed) {
            analysisMapper.update(null, Wrappers.<AiAnalysis>lambdaUpdate()
                    .eq(AiAnalysis::getWrongQuestionId, id).eq(AiAnalysis::getStatus, 1).set(AiAnalysis::getStatus, 0));
            knowledgeMapper.delete(Wrappers.<WrongQuestionKnowledge>lambdaQuery()
                    .eq(WrongQuestionKnowledge::getWrongQuestionId, id));
        }
    }

    @Transactional
    public void delete(Long id) {
        WrongQuestion question = owned(id);
        int deleted = questionMapper.delete(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getId, question.getId()).eq(WrongQuestion::getUserId, CurrentUser.id())
                .ne(WrongQuestion::getStatus, 1));
        if (deleted != 1) throw new BusinessException(409, "错题正在分析或已被删除，请刷新后重试");
    }

    private WrongQuestion owned(Long id) {
        WrongQuestion question = questionMapper.selectOne(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getId, id).eq(WrongQuestion::getUserId, CurrentUser.id()));
        if (question == null) throw new BusinessException(404, "错题不存在");
        return question;
    }

    private void apply(WrongQuestion question, WrongQuestionRequest request) {
        question.setTitle(request.title().trim());
        question.setDescription(request.description());
        question.setLanguage(request.language().trim());
        question.setCode(request.code());
        question.setErrorMessage(request.errorMessage());
        question.setActualOutput(request.actualOutput());
        question.setExpectedOutput(request.expectedOutput());
        question.setUserNote(request.userNote());
    }
}
