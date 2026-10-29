-- 升级前先停止所有应用实例并备份数据库；只执行一次。
-- 旧版本不记录题目版本，待分析、分析中或失败的旧结果无法可靠证明有效。
-- 历史分析及人工核验保留；相关题目需要重新分析。
START TRANSACTION;
UPDATE ai_analysis a JOIN wrong_question q ON q.id = a.wrong_question_id
SET a.status = 0 WHERE q.status IN (0, 1, 3);
DELETE r FROM wrong_question_knowledge r JOIN wrong_question q ON q.id = r.wrong_question_id
WHERE q.status IN (0, 1, 3);
UPDATE wrong_question SET status = 0 WHERE status IN (1, 3);
COMMIT;
