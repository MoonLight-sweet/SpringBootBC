package com.codeclinic.analysis.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.codeclinic.analysis.config.DeepSeekProperties;
import com.codeclinic.analysis.entity.*;
import com.codeclinic.analysis.mapper.*;
import com.codeclinic.common.BusinessException;
import com.codeclinic.knowledge.service.KnowledgeService;
import com.codeclinic.question.entity.WrongQuestion;
import com.codeclinic.question.mapper.WrongQuestionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisServiceTest {
    WrongQuestionMapper questions=mock(WrongQuestionMapper.class);
    AiAnalysisMapper analyses=mock(AiAnalysisMapper.class);
    AnalysisRecordMapper records=mock(AnalysisRecordMapper.class);
    DeepSeekClient client=mock(DeepSeekClient.class);
    KnowledgeService knowledge=mock(KnowledgeService.class);
    PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    AnalysisService service;
    @BeforeEach void setup(){
        for(Class<?> type:new Class<?>[]{WrongQuestion.class,AiAnalysis.class,AnalysisRecord.class})
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"test"),type);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(7L,null));
        WrongQuestion q=new WrongQuestion();q.setId(42L);q.setUserId(7L);q.setStatus(2);q.setCode("code");
        when(questions.selectOne(any())).thenReturn(q);
        when(questions.update(isNull(),any())).thenReturn(1);
        when(manager.getTransaction(any())).thenAnswer(x->new SimpleTransactionStatus());
        ObjectMapper json=new ObjectMapper();
        service=new AnalysisService(questions,analyses,records,new PromptService(),client,new AiResultParser(json),knowledge,json,new DeepSeekProperties(),new AiQuotaService(20,3),manager);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void malformedResponseDoesNotSaveResultOrKnowledge(){
        when(client.chat(any())).thenReturn("not json");
        assertThrows(BusinessException.class,()->service.analyze(42L));
        verify(analyses,never()).insert(any(AiAnalysis.class));
        verify(knowledge,never()).bind(anyLong(),anyList());
        verify(records).insert(argThat((AnalysisRecord r)->r.getStatus()==0));
    }
    @Test void knowledgeSaveFailureRollsBackResultTransaction(){
        when(client.chat(any())).thenReturn("{\"errorType\":\"x\",\"errorLocation\":\"x\",\"errorReason\":\"x\",\"suggestion\":\"x\",\"correctCode\":\"x\",\"knowledgePoints\":[\"x\"],\"learningAdvice\":\"x\"}");
        doThrow(new IllegalStateException("test")).when(knowledge).bind(anyLong(),anyList());
        assertThrows(BusinessException.class,()->service.analyze(42L));
        verify(manager).rollback(any());
        verify(records).insert(argThat((AnalysisRecord r)->r.getStatus()==0));
    }
}
