package com.codeclinic.analysis.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.codeclinic.analysis.config.DeepSeekProperties;
import com.codeclinic.analysis.entity.AiAnalysis;
import com.codeclinic.analysis.entity.AnalysisRecord;
import com.codeclinic.analysis.mapper.AiAnalysisMapper;
import com.codeclinic.analysis.mapper.AnalysisRecordMapper;
import com.codeclinic.analysis.vo.AnalysisResultVO;
import com.codeclinic.common.BusinessException;
import com.codeclinic.knowledge.service.KnowledgeService;
import com.codeclinic.question.entity.WrongQuestion;
import com.codeclinic.question.mapper.WrongQuestionMapper;
import com.codeclinic.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AnalysisService {
    private final WrongQuestionMapper questionMapper;
    private final AiAnalysisMapper analysisMapper;
    private final AnalysisRecordMapper recordMapper;
    private final PromptService promptService;
    private final DeepSeekClient deepSeekClient;
    private final AiResultParser resultParser;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;
    private final DeepSeekProperties properties;
    private final AiQuotaService quota;
    private final org.springframework.transaction.support.TransactionTemplate transaction;

    public AnalysisService(WrongQuestionMapper questionMapper, AiAnalysisMapper analysisMapper,
                           AnalysisRecordMapper recordMapper, PromptService promptService,
                           DeepSeekClient deepSeekClient, AiResultParser resultParser,
                           KnowledgeService knowledgeService, ObjectMapper objectMapper,
                           DeepSeekProperties properties, AiQuotaService quota,
                           org.springframework.transaction.PlatformTransactionManager manager) {
        this.questionMapper = questionMapper;
        this.analysisMapper = analysisMapper;
        this.recordMapper = recordMapper;
        this.promptService = promptService;
        this.deepSeekClient = deepSeekClient;
        this.resultParser = resultParser;
        this.knowledgeService = knowledgeService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.quota = quota;
        this.transaction = new org.springframework.transaction.support.TransactionTemplate(manager);
    }

    public AnalysisResultVO analyze(Long questionId) {
        WrongQuestion question = ownedQuestion(questionId);
        if (question.getStatus() != null && question.getStatus() == 1) {
            throw new BusinessException(409, "该错题正在分析，请稍候");
        }
        try (AiQuotaService.Permit ignored = quota.acquire(CurrentUser.id())) {
            return analyzeWithPermit(questionId);
        }
    }

    private AnalysisResultVO analyzeWithPermit(Long questionId) {
        Integer claimed = transaction.execute(tx -> {
            WrongQuestion current = questionMapper.selectOne(Wrappers.<WrongQuestion>lambdaQuery()
                    .eq(WrongQuestion::getId, questionId).eq(WrongQuestion::getUserId, CurrentUser.id()).last("FOR UPDATE"));
            if (current == null || current.getStatus() == 1) return 0;
            if (current.getStatus() == 0) {
                analysisMapper.update(null, Wrappers.<AiAnalysis>lambdaUpdate()
                        .eq(AiAnalysis::getWrongQuestionId, questionId).eq(AiAnalysis::getStatus, 1).set(AiAnalysis::getStatus, 0));
                knowledgeService.clear(questionId);
            }
            return questionMapper.update(null, Wrappers.<WrongQuestion>lambdaUpdate()
                .eq(WrongQuestion::getId, questionId).eq(WrongQuestion::getUserId, CurrentUser.id())
                .ne(WrongQuestion::getStatus, 1).set(WrongQuestion::getStatus, 1)
                .set(WrongQuestion::getUpdateTime, LocalDateTime.now()));
        });
        if (claimed == null || claimed != 1) throw new BusinessException(409, "该错题正在分析或已被删除，请刷新后重试");
        String prompt = null;
        String response = null;
        log.info("开始进行AI错题分析，questionId={}", questionId);
        try {
            WrongQuestion question = ownedQuestion(questionId);
            prompt = promptService.buildPrompt(question);
            response = deepSeekClient.chat(prompt);
            AiResultParser.ParsedAnalysis parsed = resultParser.parse(response);
            String rawResponse = response;
            String savedPrompt = prompt;
            return transaction.execute(tx -> {
                AiAnalysis analysis = saveAnalysis(questionId, parsed);
                knowledgeService.bind(questionId, parsed.knowledgePoints());
                saveRecord(questionId, savedPrompt, rawResponse, 1, null);
                setAnalysisStatus(questionId, 2);
                return toVO(analysis);
            });
        } catch (Exception e) {
            setAnalysisStatus(questionId, 3);
            saveRecord(questionId, prompt, response, 0, e.getMessage());
            log.warn("AI分析失败，questionId={}，异常类型={}", questionId, e.getClass().getSimpleName());
            if (e instanceof BusinessException businessException) throw businessException;
            throw new BusinessException(503, "AI分析服务暂时不可用");
        }
    }

    private void setAnalysisStatus(Long questionId, int status) {
        int updated = questionMapper.update(null, Wrappers.<WrongQuestion>lambdaUpdate()
                .eq(WrongQuestion::getId, questionId).eq(WrongQuestion::getUserId, CurrentUser.id())
                .eq(WrongQuestion::getStatus, 1).set(WrongQuestion::getStatus, status)
                .set(WrongQuestion::getUpdateTime, LocalDateTime.now()));
        if (updated != 1) throw new BusinessException(409, "错题分析状态已改变，请刷新后重试");
    }

    public AnalysisResultVO latest(Long questionId) {
        if (ownedQuestion(questionId).getStatus() == 0) throw new BusinessException(404, "该错题还没有有效分析结果");
        AiAnalysis analysis = analysisMapper.selectOne(Wrappers.<AiAnalysis>lambdaQuery()
                .eq(AiAnalysis::getWrongQuestionId, questionId)
                .eq(AiAnalysis::getStatus, 1)
                .orderByDesc(AiAnalysis::getId).last("LIMIT 1"));
        if (analysis == null) throw new BusinessException(404, "该错题还没有分析结果");
        return toVO(analysis);
    }

    public AnalysisResultVO latestOrNull(Long questionId) {
        AiAnalysis analysis = analysisMapper.selectOne(Wrappers.<AiAnalysis>lambdaQuery()
                .eq(AiAnalysis::getWrongQuestionId, questionId)
                .eq(AiAnalysis::getStatus, 1)
                .orderByDesc(AiAnalysis::getId).last("LIMIT 1"));
        return analysis == null ? null : toVO(analysis);
    }

    public List<AnalysisResultVO> history(Long questionId) {
        ownedQuestion(questionId);
        return analysisMapper.selectList(Wrappers.<AiAnalysis>lambdaQuery()
                        .eq(AiAnalysis::getWrongQuestionId, questionId).orderByDesc(AiAnalysis::getId))
                .stream().map(this::toVO).toList();
    }

    private WrongQuestion ownedQuestion(Long id) {
        WrongQuestion question = questionMapper.selectOne(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getId, id).eq(WrongQuestion::getUserId, CurrentUser.id()));
        if (question == null) throw new BusinessException(404, "错题不存在");
        return question;
    }

    private AiAnalysis saveAnalysis(Long questionId, AiResultParser.ParsedAnalysis result) {
        AiAnalysis analysis = new AiAnalysis();
        analysis.setWrongQuestionId(questionId);
        analysis.setErrorType(result.errorType());
        analysis.setErrorLocation(result.errorLocation());
        analysis.setErrorReason(result.errorReason());
        analysis.setSuggestion(result.suggestion());
        analysis.setCorrectCode(result.correctCode());
        try {
            analysis.setKnowledgePoints(objectMapper.writeValueAsString(result.knowledgePoints()));
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "知识点保存失败");
        }
        analysis.setLearningAdvice(result.learningAdvice());
        analysis.setModelName(deepSeekClient.modelName());
        analysis.setStatus(1);
        analysis.setCreateTime(LocalDateTime.now());
        analysisMapper.insert(analysis);
        return analysis;
    }

    private void saveRecord(Long questionId, String prompt, String response, int status, String error) {
        AnalysisRecord record = new AnalysisRecord();
        record.setWrongQuestionId(questionId);
        record.setModelName(deepSeekClient.modelName());
        record.setPrompt(prompt);
        record.setResponse(response);
        record.setStatus(status);
        record.setErrorMessage(error);
        recordMapper.insert(record);
    }

    public AnalysisResultVO toVO(AiAnalysis analysis) {
        try {
            List<String> points = objectMapper.readValue(analysis.getKnowledgePoints(), new TypeReference<>() {});
            return new AnalysisResultVO(analysis.getId(), analysis.getErrorType(), analysis.getErrorLocation(),
                    analysis.getErrorReason(), analysis.getSuggestion(), analysis.getCorrectCode(), points,
                    analysis.getLearningAdvice(), analysis.getModelName(), analysis.getCreateTime());
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "分析结果读取失败");
        }
    }
}
