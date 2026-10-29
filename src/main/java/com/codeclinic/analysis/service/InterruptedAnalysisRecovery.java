package com.codeclinic.analysis.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@DependsOnDatabaseInitialization
public class InterruptedAnalysisRecovery {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final boolean enabled;

    public InterruptedAnalysisRecovery(JdbcTemplate jdbc, PlatformTransactionManager manager,
            @Value("${ai.recover-on-startup:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
        this.enabled = enabled;
    }

    @PostConstruct
    public void recover() {
        if (!enabled) return;
        // shortcut: 启动恢复用于单实例部署，共享数据库运行多实例前须实现任务归属与租约。
        Integer count = transaction.execute(tx -> {
            var ids = jdbc.queryForList("SELECT id FROM wrong_question WHERE status = 1 FOR UPDATE", Long.class);
            for (Long id : ids) {
                jdbc.update("INSERT INTO analysis_record(wrong_question_id, status, error_message) VALUES (?, 0, ?)",
                        id, "上次分析因服务中断未完成，请重新分析");
                jdbc.update("UPDATE wrong_question SET status = 3, update_time = CURRENT_TIMESTAMP WHERE id = ? AND status = 1", id);
            }
            return ids.size();
        });
        if (count != null && count > 0) log.info("已恢复{}项中断分析，可重新提交", count);
    }
}
