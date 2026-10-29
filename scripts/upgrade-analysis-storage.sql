-- 停止应用并备份后执行；旧表只扩大容量，新快照字段只添加一次。
ALTER TABLE wrong_question MODIFY description MEDIUMTEXT, MODIFY code MEDIUMTEXT NOT NULL,
 MODIFY error_message MEDIUMTEXT, MODIFY actual_output MEDIUMTEXT, MODIFY expected_output MEDIUMTEXT, MODIFY user_note MEDIUMTEXT;
ALTER TABLE ai_analysis MODIFY error_location MEDIUMTEXT NOT NULL, MODIFY error_reason MEDIUMTEXT NOT NULL,
 MODIFY suggestion MEDIUMTEXT NOT NULL, MODIFY correct_code MEDIUMTEXT NOT NULL,
 MODIFY knowledge_points MEDIUMTEXT NOT NULL, MODIFY learning_advice MEDIUMTEXT NOT NULL;
ALTER TABLE analysis_record MODIFY prompt MEDIUMTEXT, MODIFY response MEDIUMTEXT, MODIFY error_message MEDIUMTEXT;
SET @add_snapshot = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ai_analysis' AND column_name='input_snapshot')=0,
 'ALTER TABLE ai_analysis ADD COLUMN input_snapshot MEDIUMTEXT NULL', 'SELECT 1');
PREPARE upgrade_stmt FROM @add_snapshot;
EXECUTE upgrade_stmt;
DEALLOCATE PREPARE upgrade_stmt;
