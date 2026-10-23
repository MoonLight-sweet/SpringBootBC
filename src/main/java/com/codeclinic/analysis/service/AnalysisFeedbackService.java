package com.codeclinic.analysis.service;

import com.codeclinic.common.BusinessException;
import com.codeclinic.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalysisFeedbackService {
    private static final Set<String> VERDICTS = Set.of("accurate", "inaccurate", "uncertain");
    private final JdbcTemplate jdbc;

    public AnalysisFeedbackService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> get(long analysisId) {
        requireOwned(analysisId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT verdict, comment, create_time AS createTime, update_time AS updateTime
                FROM analysis_feedback WHERE analysis_id = ? AND user_id = ?
                """, analysisId, CurrentUser.id());
        return rows.isEmpty() ? Map.of("verdict", "unreviewed") : rows.getFirst();
    }

    public Map<String, Object> save(long analysisId, String verdict, String comment) {
        requireOwned(analysisId);
        if (!VERDICTS.contains(verdict)) throw new BusinessException("核验结论不正确");
        if (comment != null && comment.length() > 1000) throw new BusinessException("核验说明不能超过1000字");
        jdbc.update("""
                INSERT INTO analysis_feedback(analysis_id, user_id, verdict, comment) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE verdict = VALUES(verdict), comment = VALUES(comment)
                """, analysisId, CurrentUser.id(), verdict, comment == null || comment.isBlank() ? null : comment.trim());
        return get(analysisId);
    }

    private void requireOwned(long analysisId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ai_analysis a JOIN wrong_question q ON q.id = a.wrong_question_id
                WHERE a.id = ? AND q.user_id = ?
                """, Long.class, analysisId, CurrentUser.id());
        if (count == null || count == 0) throw new BusinessException(404, "分析结果不存在");
    }
}
