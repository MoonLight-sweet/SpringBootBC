package com.codeclinic.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeclinic.analysis.service.AnalysisService;
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

@Service
public class WrongQuestionService {
    private final WrongQuestionMapper questionMapper;
    private final AnalysisService analysisService;

    public WrongQuestionService(WrongQuestionMapper questionMapper, AnalysisService analysisService) {
        this.questionMapper = questionMapper;
        this.analysisService = analysisService;
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
            query.apply("id IN (SELECT a.wrong_question_id FROM ai_analysis a WHERE a.error_type = {0} AND a.id = (SELECT MAX(b.id) FROM ai_analysis b WHERE b.wrong_question_id = a.wrong_question_id))", errorType);
        }
        IPage<WrongQuestion> result = questionMapper.selectPage(new Page<>(current, size), query);
        return new PageResult<>(result.getRecords().stream().map(WrongQuestionVO::from).toList(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    public WrongQuestionDetailVO detail(Long id) {
        WrongQuestion question = owned(id);
        if (question.getStatus() == 1) throw new BusinessException(409, "正在分析，请完成后再修改");
        return new WrongQuestionDetailVO(WrongQuestionVO.from(question), analysisService.latestOrNull(id));
    }

    public void update(Long id, WrongQuestionRequest request) {
        WrongQuestion question = owned(id);
        if (question.getStatus() == 1) throw new BusinessException(409, "正在分析，请完成后再删除");
        apply(question, request);
        question.setStatus(0);
        question.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(question);
    }

    @Transactional
    public void delete(Long id) {
        WrongQuestion question = owned(id);
        questionMapper.deleteById(question.getId());
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
