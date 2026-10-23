package com.codeclinic.review;

import com.codeclinic.common.BusinessException;
import com.codeclinic.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {
    private final JdbcTemplate jdbc;

    public ReviewService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(boolean dueOnly) {
        String sql = """
                SELECT q.id AS questionId, q.title, q.language,
                       COALESCE(r.state, 'todo') AS state, COALESCE(r.streak, 0) AS streak,
                       COALESCE(r.review_count, 0) AS reviewCount, r.next_review_at AS nextReviewAt,
                       r.last_review_at AS lastReviewAt,
                       CASE WHEN r.next_review_at IS NULL OR r.next_review_at <= CURRENT_TIMESTAMP THEN 1 ELSE 0 END AS due
                FROM wrong_question q LEFT JOIN question_review r ON r.question_id = q.id
                WHERE q.user_id = ?
                """ + (dueOnly ? " AND (r.next_review_at IS NULL OR r.next_review_at <= CURRENT_TIMESTAMP)" : "")
                + " ORDER BY CASE WHEN r.next_review_at IS NULL THEN 0 ELSE 1 END, r.next_review_at, q.id LIMIT 100";
        return jdbc.queryForList(sql, CurrentUser.id());
    }

    public Map<String, Object> summary() {
        return jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       SUM(CASE WHEN r.next_review_at IS NULL OR r.next_review_at <= CURRENT_TIMESTAMP THEN 1 ELSE 0 END) AS due,
                       SUM(CASE WHEN r.state = 'mastered' THEN 1 ELSE 0 END) AS mastered,
                       SUM(CASE WHEN r.state = 'reviewing' THEN 1 ELSE 0 END) AS reviewing
                FROM wrong_question q LEFT JOIN question_review r ON r.question_id = q.id
                WHERE q.user_id = ?
                """, CurrentUser.id());
    }

    public List<Map<String, Object>> history(long questionId) {
        requireOwned(questionId);
        return jdbc.queryForList("""
                SELECT id, remembered, note, create_time AS createTime FROM review_record
                WHERE question_id = ? AND user_id = ? ORDER BY id DESC LIMIT 100
                """, questionId, CurrentUser.id());
    }

    @Transactional
    public Map<String, Object> record(long questionId, boolean remembered, String note) {
        requireOwned(questionId);
        if (note != null && note.length() > 500) throw new BusinessException("复习备注不能超过500字");
        long userId = CurrentUser.id();
        jdbc.update("INSERT IGNORE INTO question_review(question_id, user_id) VALUES (?, ?)", questionId, userId);
        Integer previous = jdbc.queryForObject(
                "SELECT streak FROM question_review WHERE question_id = ? AND user_id = ? FOR UPDATE",
                Integer.class, questionId, userId);
        Integer due = jdbc.queryForObject("""
                SELECT CASE WHEN next_review_at IS NULL OR next_review_at <= CURRENT_TIMESTAMP THEN 1 ELSE 0 END
                FROM question_review WHERE question_id = ? AND user_id = ?
                """, Integer.class, questionId, userId);
        if (due == null || due != 1) throw new BusinessException(409, "尚未到下次复习时间");
        int streak = remembered ? Math.min(previous + 1, 30) : 0;
        int[] intervals = {1, 3, 7, 14, 30};
        int days = remembered ? intervals[Math.min(streak - 1, intervals.length - 1)] : 1;
        String state = streak >= 5 ? "mastered" : "reviewing";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.plusDays(days);
        jdbc.update("""
                UPDATE question_review SET state = ?, streak = ?, review_count = review_count + 1,
                    last_review_at = ?, next_review_at = ? WHERE question_id = ? AND user_id = ?
                """, state, streak, now, next, questionId, userId);
        jdbc.update("INSERT INTO review_record(question_id, user_id, remembered, note) VALUES (?, ?, ?, ?)",
                questionId, userId, remembered, note == null || note.isBlank() ? null : note.trim());
        return Map.of("questionId", questionId, "state", state, "streak", streak,
                "nextReviewAt", next, "reviewCountAdded", 1);
    }

    private void requireOwned(long questionId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM wrong_question WHERE id = ? AND user_id = ?",
                Long.class, questionId, CurrentUser.id());
        if (count == null || count == 0) throw new BusinessException(404, "错题不存在");
    }
}
